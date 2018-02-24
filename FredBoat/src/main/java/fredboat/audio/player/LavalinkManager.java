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

import fredboat.config.property.LavalinkConfig;
import fredboat.config.property.PropertyConfigProvider;
import fredboat.main.Launcher;
import fredboat.util.DiscordUtil;
import lavalink.client.io.Lavalink;
import lavalink.client.io.metrics.LavalinkCollector;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LavalinkManager {

    private Lavalink lavalink = null;

    private final PropertyConfigProvider configProvider;

    public LavalinkManager(PropertyConfigProvider configProvider) {
        this.configProvider = configProvider;

        start();
    }

    private void start() {
        if (!isEnabled()) return;

        lavalink = new Lavalink(
                Long.toString(DiscordUtil.getBotId(configProvider.getCredentials())),
                configProvider.getCredentials().getRecommendedShardCount(),
                shardId -> Launcher.getBotController().getShardManager().getShardById(shardId)
        );

        Runtime.getRuntime().addShutdownHook(new Thread(lavalink::shutdown, "lavalink-shutdown-hook"));

        List<LavalinkConfig.LavalinkHost> hosts = configProvider.getLavalinkConfig().getLavalinkHosts();
        hosts.forEach(lavalinkHost -> lavalink.addNode(lavalinkHost.getName(), lavalinkHost.getUri(),
                lavalinkHost.getPassword()));

        new LavalinkCollector(lavalink).register();
    }

    public boolean isEnabled() {
        return !configProvider.getLavalinkConfig().getLavalinkHosts().isEmpty();
    }

    IPlayer createPlayer(String guildId) {
        return isEnabled()
                ? lavalink.getLink(guildId).getPlayer()
                : new LavaplayerPlayerWrapper(AbstractPlayer.getPlayerManager().createPlayer());
    }

    public void openConnection(VoiceChannel channel) {
        if (isEnabled()) {
            lavalink.getLink(channel.getGuild()).connect(channel);
        } else {
            channel.getGuild().getAudioManager().openAudioConnection(channel);
        }
    }

    public void closeConnection(Guild guild) {
        if (isEnabled()) {
            lavalink.getLink(guild).disconnect();
        } else {
            guild.getAudioManager().closeAudioConnection();
        }
    }

    public Lavalink getLavalink() {
        return lavalink;
    }
}
