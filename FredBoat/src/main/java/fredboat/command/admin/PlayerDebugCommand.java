/*
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
 *
 */

package fredboat.command.admin;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.definitions.PermissionLevel;
import fredboat.main.Launcher;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * @deprecated This command can probably not show information sanely with FredBoats scale. Instead, the information it
 * provides should be covered by metrics and / or rest endpoints and / or executing eval commands.
 */
@Deprecated
public class PlayerDebugCommand extends Command implements ICommandRestricted {

    public PlayerDebugCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        JSONArray a = new JSONArray();

        Launcher.getBotController().getPlayerRegistry().forEach((guildId, player) -> {
            JSONObject data = new JSONObject();
            Guild guild = player.getGuild();
            data.put("name", guild == null ? "null" : guild.getName());
            data.put("id", guildId);
            VoiceChannel voiceChannel = player.getCurrentVoiceChannel();
            data.put("users", voiceChannel == null ? "not in a voiceChannel" : voiceChannel.getMembers().toString());
            data.put("isPlaying", player.isPlaying());
            data.put("isPaused", player.isPaused());
            data.put("songCount", player.getTrackCount());

            a.put(data);
        });

        TextUtils.postToPasteService(a.toString())
                .thenApply(pasteUrl -> pasteUrl.orElse("Failed to upload to any pasteservice."))
                .thenAccept(context::reply);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Show debug information about all music players of this bot.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_OWNER;
    }
}
