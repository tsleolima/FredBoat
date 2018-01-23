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

package fredboat.command.util;

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.db.EntityIO;
import fredboat.db.entity.main.Aliases;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.shared.constant.BotConstants;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import org.apache.commons.lang3.ArrayUtils;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 23.01.18.
 * <p>
 * Allows user to set up aliases
 * //todo i18n
 */
public class AliasCommand extends Command implements IUtilCommand {

    private static final int MAX_ALLOWED_ALIASES = 30;

    public AliasCommand(@Nonnull String name, String... aliases) {
        super(name, aliases);
    }

    //todo add guild functionality
    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        //add, remove, list
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        String[] args = ArrayUtils.clone(context.args);

        boolean forGuild = false;
        if (args[0].equalsIgnoreCase("guild") || args[0].equalsIgnoreCase("here")) {
            forGuild = true;
            args = ArrayUtils.remove(args, 0);
        }
        //todo select other user or guild by id / mention, remember to adjust authorization

        if (args[0].equalsIgnoreCase("list")) {
            //todo show aliases of this user or guild

            EntityKey<Long, Aliases> entityKey;
            String title;
            if (forGuild) {
                entityKey = Aliases.key(context.guild);
                title = "Guild " + TextUtils.escapeAndDefuse(context.guild.getName());
            } else {
                entityKey = Aliases.key(context.invoker);
                title = "User " + TextUtils.escapeAndDefuse(context.invoker.getEffectiveName());
            }
            Aliases aliases = EntityIO.getAliases(entityKey);

            StringBuilder sb = new StringBuilder();
            List<Map.Entry<String, String>> sorted = new ArrayList<>(aliases.getAliases().entrySet());
            sorted.sort(Comparator.comparing(Map.Entry::getKey));

            for (Map.Entry<String, String> entry : sorted) {
                sb.append(entry.getKey())
                        .append(" -> ")
                        .append(entry.getValue())
                        .append("\n");
            }
            if (sb.length() == 0) {
                sb.append("No aliases defined.");
            }

            EmbedBuilder eb = CentralMessaging.getColoredEmbedBuilder()
                    .addField("Aliases for " + title, sb.toString(), false);

            //todo more embed things?
            context.reply(eb.build());
            return;
        }


        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 3) {
                HelpCommand.sendFormattedCommandHelp(context);
                return;
            }

            String aliasKey = args[1];
            String aliasValue = String.join(" ", ArrayUtils.removeAll(args, 0, 1)); //todo preserve original whitespace?

            //todo check whether the aliasKey would overwrite a fredboat command, and warn users about it
            //the rules are: command > guild alias > user alias

            EntityKey<Long, Aliases> entityKey;
            //noinspection Duplicates
            if (forGuild) {
                if (!PermsUtil.checkPerms(PermissionLevel.ADMIN, context.invoker)) {
                    context.reply("You need to be admin to edit the aliases of this guild.");
                    return;
                }
                entityKey = Aliases.key(context.guild);
            } else {
                entityKey = Aliases.key(context.invoker);
            }

            EntityIO.doUserFriendly(EntityIO.onMainDb(wrapper ->
                    wrapper.findApplyAndMerge(entityKey, aliases -> {
                        if (aliases.size() < MAX_ALLOWED_ALIASES) {
                            context.reply("Alias set!");
                            return aliases.setAlias(aliasKey, aliasValue);
                        } else {
                            context.reply(String.format("You have reached the maximum of %s aliases. Please remove an alias to add more.", MAX_ALLOWED_ALIASES));
                            return aliases;
                        }
                    })
            ));
            return;
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                HelpCommand.sendFormattedCommandHelp(context);
                return;
            }

            String aliasKey = args[1];

            EntityKey<Long, Aliases> entityKey;
            //noinspection Duplicates
            if (forGuild) {
                if (!PermsUtil.checkPerms(PermissionLevel.ADMIN, context.invoker)) {
                    context.reply("You need to be admin to edit the aliases of this guild.");
                    return;
                }
                entityKey = Aliases.key(context.guild);
            } else {
                entityKey = Aliases.key(context.invoker);
            }

            EntityIO.doUserFriendly(EntityIO.onMainDb(wrapper -> wrapper.findApplyAndMerge(
                    entityKey,
                    aliases -> aliases.removeAlias(aliasKey)
            )));

            context.reply("Alias removed!");
            return;
        }

        //if we ended up here, the user put in something we did not understand
        HelpCommand.sendFormattedCommandHelp(context);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return String.join("\n\n",
                "{0}{1} list OR {0}{1} guild list\n#Show your aliases, or the aliases of this guild.",
                "{0}{1} add <key> <value> OR {0}{1} guild add <key> <value>\n#Create an alias for you, or this guild.",
                "{0}{1} remove <key> OR {0}{1} guild remove <key>\n#Remove one of your, or this guild's, aliases.",
                "For more details please visit " + BotConstants.DOCS_ALIASES_URL
        );
    }
}
