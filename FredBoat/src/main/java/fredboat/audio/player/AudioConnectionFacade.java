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

package fredboat.audio.player;

import fredboat.config.property.Credentials;
import fredboat.config.property.LavalinkConfig;
import fredboat.main.Launcher;
import fredboat.util.DiscordUtil;
import lavalink.client.io.Lavalink;
import lavalink.client.io.metrics.LavalinkCollector;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.AudioManager;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Delegate audio connections either to the local JDA instances or remote lavalink nodes
 */
@Component
public class AudioConnectionFacade implements EventListener {

    @Nullable
    private final Lavalink lavalink;
    private final DebugConnectionListenerProvider debugConnectionListenerProvider;

    public AudioConnectionFacade(LavalinkConfig lavalinkConfig, Credentials credentials,
                                 DebugConnectionListenerProvider debugConnectionListenerProvider) {
        this.debugConnectionListenerProvider = debugConnectionListenerProvider;
        if (lavalinkConfig.getLavalinkHosts().isEmpty()) {
            lavalink = null; //local playback
            return;
        }

        lavalink = new Lavalink(
                Long.toString(DiscordUtil.getBotId(credentials)),
                credentials.getRecommendedShardCount(),
                shardId -> Launcher.getBotController().getShardManager().getShardById(shardId)
        );
        Runtime.getRuntime().addShutdownHook(new Thread(lavalink::shutdown, "lavalink-shutdown-hook"));

        List<LavalinkConfig.LavalinkHost> hosts = lavalinkConfig.getLavalinkHosts();
        hosts.forEach(lavalinkHost -> lavalink.addNode(lavalinkHost.getName(), lavalinkHost.getUri(),
                lavalinkHost.getPassword()));

        new LavalinkCollector(lavalink).register();
    }

    public boolean isLocal() {
        return lavalink == null;
    }

    IPlayer createPlayer(String guildId) {
        return lavalink == null
                ? new LavaplayerPlayerWrapper(AbstractPlayer.getPlayerManager().createPlayer())
                : lavalink.getLink(guildId).getPlayer();
    }

    public void openConnection(VoiceChannel channel) {
        if (lavalink == null) {
            AudioManager audioManager = channel.getGuild().getAudioManager();
            audioManager.setConnectionListener(debugConnectionListenerProvider.get(channel.getGuild()));
        } else {
            lavalink.getLink(channel.getGuild()).connect(channel);
        }
    }

    public void closeConnection(Guild guild) {
        if (lavalink == null) {
            guild.getAudioManager().closeAudioConnection();
        } else {
            lavalink.getLink(guild).disconnect();
        }
    }

    @Nullable
    public Lavalink getLavalink() {
        return lavalink;
    }

    @Override
    public void onEvent(Event event) {
        if (lavalink != null) {
            lavalink.onEvent(event);
        }
    }
}
