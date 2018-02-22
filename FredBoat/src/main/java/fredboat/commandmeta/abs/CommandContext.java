/*
 *
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.commandmeta.abs;

import fredboat.command.config.PrefixCommand;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.CommandRegistry;
import fredboat.definitions.Module;
import fredboat.feature.metrics.Metrics;
import fredboat.main.BotController;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.annotations.FieldsAreNonNullByDefault;
import space.npstr.annotations.ParametersAreNonnullByDefault;
import space.npstr.annotations.ReturnTypesAreNonNullByDefault;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 08.09.17.
 * <p>
 * Convenience container for values associated with an issued command, also does the parsing
 * <p>
 * Don't save these anywhere as they hold references to JDA objects, just pass them down through (short-lived) command execution
 */
@FieldsAreNonNullByDefault
@ParametersAreNonnullByDefault
@ReturnTypesAreNonNullByDefault
public class CommandContext extends Context {

    private static final Logger log = LoggerFactory.getLogger(CommandContext.class);

    // https://regex101.com/r/ceFMeF/6
    //group 1 is the mention, group 2 is the id of the mention, group 3 is the rest of the input including new lines
    public static final Pattern MENTION_PREFIX = Pattern.compile("^(<@!?([0-9]+)>)(.*)$", Pattern.DOTALL);

    public final Guild guild;
    public final TextChannel channel;
    public final Member invoker;
    public final Message msg;

    public final boolean isMention;          // whether a mention was used to trigger this command
    public final String trigger;             // the command trigger, e.g. "play", or "p", or "pLaY", whatever the user typed
    public final String[] args;              // the arguments split by whitespace, excluding prefix and trigger
    public final String rawArgs;             // raw arguments excluding prefix and trigger, trimmed
    public final Command command;

    /**
     * @param event the event to be parsed
     * @return The full context for the triggered command, or null if it's not a command that we know.
     */
    @Nullable
    public static CommandContext parse(MessageReceivedEvent event) {
        String raw = event.getMessage().getContentRaw();

        String input;
        boolean isMention = false;
        Matcher mentionMatcher = MENTION_PREFIX.matcher(raw);
        // either starts with a mention of us
        if (mentionMatcher.find() && mentionMatcher.group(2).equals(event.getJDA().getSelfUser().getId())) {
            input = mentionMatcher.group(3).trim();
            isMention = true;
        }
        // or starts with a custom/default prefix
        else {
            String prefix = PrefixCommand.giefPrefix(event.getGuild());
            String defaultPrefix = BotController.INS.getAppConfig().getPrefix();
            if (raw.startsWith(prefix)) {
                input = raw.substring(prefix.length());
                if (prefix.equals(defaultPrefix)) {
                    Metrics.prefixParsed.labels("default").inc();
                } else {
                    Metrics.prefixParsed.labels("custom").inc();
                }
            } else {
                //hardcoded check for the help or prefix command that is always displayed as FredBoat status
                if (raw.startsWith(defaultPrefix + CommandInitializer.HELP_COMM_NAME)
                        || raw.startsWith(defaultPrefix + CommandInitializer.PREFIX_COMM_NAME)) {
                    Metrics.prefixParsed.labels("default").inc();
                    input = raw.substring(defaultPrefix.length());
                } else {
                    //no match neither mention nor custom/default prefix
                    return null;
                }
            }
        }
        input = input.trim();// eliminate possible whitespace between the mention/prefix and the rest of the input
        if (input.isEmpty()) {
            if (isMention) { //just a mention and nothing else? trigger the prefix command
                input = "prefix";
            } else {
                return null; //no command will be detectable from an empty input
            }
        }

        // the \p{javaSpaceChar} instead of the better known \s is used because it actually includes unicode whitespaces
        String[] args = input.split("\\p{javaSpaceChar}+");
        if (args.length < 1) {
            return null; //while this shouldn't technically be possible due to the preprocessing of the input, better be safe than throw exceptions
        }

        String commandTrigger = args[0];

        Command command = CommandRegistry.findCommand(commandTrigger.toLowerCase());
        if (command == null) {
            log.info("Unknown command:\t{}", commandTrigger);
            return null;
        } else {
            return new CommandContext(
                    event.getGuild(),
                    event.getTextChannel(),
                    event.getMember(),
                    event.getMessage(),
                    isMention,
                    commandTrigger,
                    Arrays.copyOfRange(args, 1, args.length),//exclude args[0] that contains the command trigger
                    input.replaceFirst(commandTrigger, "").trim(),
                    command);
        }
    }

    private CommandContext(Guild guild, TextChannel channel, Member invoker, Message message,
                           boolean isMention, String trigger, String[] args, String rawArgs, Command command) {
        this.guild = guild;
        this.channel = channel;
        this.invoker = invoker;
        this.msg = message;
        this.isMention = isMention;
        this.trigger = trigger;
        this.args = args;
        this.rawArgs = rawArgs;
        this.command = command;
    }

    /**
     * Deletes the users message that triggered this command, if we have the permissions to do so
     */
    public void deleteMessage() {
        TextChannel tc = msg.getTextChannel();

        if (tc != null && hasPermissions(tc, Permission.MESSAGE_MANAGE)) {
            CentralMessaging.deleteMessage(msg);
        }
    }

    /**
     * @return an adjusted list of mentions in case the prefix mention is used to exclude it. This method should always
     * be used over Message#getMentions()
     */
    public List<User> getMentionedUsers() {
        if (isMention) {
            //remove the first mention
            List<User> mentions = new ArrayList<>(msg.getMentionedUsers());
            if (!mentions.isEmpty()) {
                mentions.remove(0);
                //FIXME: this will mess with the mentions if the bot was mentioned at a later place in the messagea second time,
                // for example @bot hug @bot will not trigger a self hug message
                // low priority, this is mostly a cosmetic issue
            }
            return mentions;
        } else {
            return msg.getMentionedUsers();
        }
    }

    public boolean hasArguments() {
        return args.length > 0 && !rawArgs.isEmpty();
    }

    public Collection<Module> getEnabledModules() {
        return BotController.INS.getEntityIO().fetchGuildModules(this.guild).getEnabledModules();
    }

    @Override
    public TextChannel getTextChannel() {
        return channel;
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    @Override
    public Member getMember() {
        return invoker;
    }

    @Override
    public User getUser() {
        return invoker.getUser();
    }
}
