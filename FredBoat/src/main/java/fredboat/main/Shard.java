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

package fredboat.main;

import fredboat.agent.StatsAgent;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.LavalinkManager;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.MusicPersistenceHandler;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Future;

/**
 * This class represents a FredBoat shard, containing all non-static FredBoat shard-level logic.
 */
public class Shard implements IShard {

    private static final Logger log = LoggerFactory.getLogger(Shard.class);
    private static final BotController FBC = BotController.INS;

    private final int shardId;

    //For when we need to join a revived shard with it's old GuildPlayers
    private final ArrayList<String> channelsToRejoin = new ArrayList<>();
    private final BotMetrics.JdaEntityCounts jdaEntityCountsShard = new BotMetrics.JdaEntityCounts();

    @Nonnull
    protected volatile JDA jda;

    Shard(int shardId) {
        this.shardId = shardId;
        log.info("Building shard " + shardId);
        jda = ShardBuilder.buildJDA(shardId, false);

        FBC.getJdaEntityCountAgent().addAction(new ShardStatsCounter(jda.getShardInfo(),
                () -> jdaEntityCountsShard.count(Collections.singletonList(this))));
    }

    @Override
    public void onInit(@Nonnull ReadyEvent readyEvent) {
        log.info("Received ready event for {}", readyEvent.getJDA().getShardInfo().toString());
        jdaEntityCountsShard.count(Collections.singletonList(this), true);//jda finished loading, do a single count to init values


        if (Config.getNumShards() <= 10) {
            //the current implementation of music persistence is not a good idea on big bots
            MusicPersistenceHandler.reloadPlaylists(this);
        }

        //Rejoin old channels if revived
        channelsToRejoin.forEach(vcid -> {
            VoiceChannel channel = readyEvent.getJDA().getVoiceChannelById(vcid);
            if (channel == null) return;
            GuildPlayer player = PlayerRegistry.getOrCreate(channel.getGuild());

            LavalinkManager.ins.openConnection(channel);

            if (!LavalinkManager.ins.isEnabled()) {
                AudioManager am = channel.getGuild().getAudioManager();
                am.setSendingHandler(player);
            }
        });

        channelsToRejoin.clear();
    }

    private volatile Future reviveTask;
    private volatile long reviveTaskStarted;

    @Nonnull
    @Override
    public synchronized String revive(boolean... force) {

        String info = "";
        //is there an active task doing that already?
        if (reviveTask != null && !reviveTask.isCancelled() && !reviveTask.isDone()) {
            info += String.format("Active task to revive shard %s running for %s detected.",
                    shardId, TextUtils.formatTime(System.currentTimeMillis() - reviveTaskStarted));

            //is the force flag set?
            if (force.length > 0 && force[0]) {
                info += "\nForce option detected: Killing running task, creating a new one.";
                reviveTask.cancel(true);
            } else {
                info += "\nNo action required. Set force flag to force a recreation of the task.";
                log.info(info);
                return info;
            }
        }

        //wrap this into a task to avoid blocking a thread
        reviveTaskStarted = System.currentTimeMillis();
        reviveTask = BotController.INS.getExecutor().submit(() -> {
            try {
                log.info("Reviving shard " + shardId);

                try {
                    channelsToRejoin.clear();

                    PlayerRegistry.getPlayingPlayers().stream()
                            .filter(guildPlayer -> guildPlayer.getJda().getShardInfo().getShardId() == shardId)
                            .forEach(guildPlayer -> {
                                VoiceChannel channel = guildPlayer.getCurrentVoiceChannel();
                                if (channel != null) channelsToRejoin.add(channel.getId());
                            });
                } catch (Exception ex) {
                    log.error("Caught exception while saving channels to revive shard {}", shardId, ex);
                }

                jda.shutdown();

                //remove listeners from decommissioned jda for good memory hygiene
                jda.removeEventListener(jda.getRegisteredListeners());

                //a blocking build makes sure the revive task runs until the shard is connected, otherwise the shard may
                // get revived again accidentally while still connecting
                jda = ShardBuilder.buildJDA(shardId, true);

            } catch (Exception e) {
                log.error("Task to revive shard {} threw an exception after running for {}",
                        shardId, TextUtils.formatTime(System.currentTimeMillis() - reviveTaskStarted), e);
            }
        });
        info += String.format("\nTask to revive shard %s started!", shardId);
        log.info(info);
        return info;
    }

    @Override
    public int getShardId() {
        return shardId;
    }

    @Nonnull
    @Override
    public JDA.ShardInfo getShardInfo() {
        //assuming this is never null because we are forcing sharding in the Config
        return getJda().getShardInfo();
    }

    @Override
    @Nonnull
    public JDA getJda() {
        return jda;
    }

    //JDA entity counts
    @Override
    public int getUserCount() {
        return jdaEntityCountsShard.uniqueUsersCount;
    }

    @Override
    public int getGuildCount() {
        return jdaEntityCountsShard.guildsCount;
    }

    @Override
    public int getTextChannelCount() {
        return jdaEntityCountsShard.textChannelsCount;
    }

    @Override
    public int getVoiceChannelCount() {
        return jdaEntityCountsShard.voiceChannelsCount;
    }

    @Override
    public int getCategoriesCount() {
        return jdaEntityCountsShard.categoriesCount;
    }

    @Override
    public int getEmotesCount() {
        return jdaEntityCountsShard.emotesCount;
    }

    @Override
    public int getRolesCount() {
        return jdaEntityCountsShard.rolesCount;
    }

    private static class ShardStatsCounter implements StatsAgent.Action {
        private final JDA.ShardInfo shardInfo;
        private final StatsAgent.Action action;

        ShardStatsCounter(JDA.ShardInfo shardInfo, StatsAgent.Action action) {
            this.shardInfo = shardInfo;
            this.action = action;
        }

        @Override
        public String getName() {
            return "jda entity stats for shard " + shardInfo.getShardString();
        }

        @Override
        public void act() throws Exception {
            action.act();
        }
    }
}
