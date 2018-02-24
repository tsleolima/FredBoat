package fredboat.main;

import fredboat.agent.StatsAgent;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A reference to a shard. Directly referencing a JDA instance is problematic, as it may shut down and be replaced.
 * Furthermore, it causes a memory leak.
 */
public class ShardContext {

    private static final Logger log = LoggerFactory.getLogger(ShardContext.class);
    private static final ConcurrentHashMap<Integer, ShardContext> instances = new ConcurrentHashMap<>();
    private final int id;
    private final PlayerRegistry playerRegistry;
    private final ArrayList<String> channelsToRejoin = new ArrayList<>();
    private final BotMetrics.JdaEntityCounts jdaEntityCountsShard = new BotMetrics.JdaEntityCounts();

    private ShardContext(int id, PlayerRegistry playerRegistry) {
        this.id = id;
        this.playerRegistry = playerRegistry;

        if (Launcher.getBotController().getShardManager().getShardById(id) == null) {
            throw new IllegalArgumentException("Shard " + id + " does not exist!");
        }
    }

    @Nonnull
    public static ShardContext of(int id, PlayerRegistry playerRegistry) {
        return instances.computeIfAbsent(id, integer -> new ShardContext(id, playerRegistry));
    }

    @Nonnull
    public static ShardContext of(JDA jda, PlayerRegistry playerRegistry) {
        return of(jda.getShardInfo().getShardId(), playerRegistry);
    }

    public int getId() {
        return id;
    }

    @Nonnull
    public JDA getJda() {
        return Launcher.getBotController().getShardManager().getShardById(id);
    }

    public void onReady(@Nonnull ReadyEvent readyEvent) {
        Launcher.getBotController().getStatsAgent().addAction(new ShardStatsCounter(getJda().getShardInfo(),
                () -> jdaEntityCountsShard.count(() -> Collections.singletonList(getJda()))));

        log.info("Received ready event for {}", readyEvent.getJDA().getShardInfo().toString());
        jdaEntityCountsShard.count(() -> Collections.singletonList(getJda()), true);//jda finished loading, do a single count to init values
    }

    private static class ShardStatsCounter implements StatsAgent.Action {
        private final JDA.ShardInfo shardInfo;
        private final Runnable action;

        ShardStatsCounter(JDA.ShardInfo shardInfo, Runnable action) {
            this.shardInfo = shardInfo;
            this.action = action;
        }


        @Override
        public String getName() {
            return "jda entity stats for shard " + shardInfo.getShardString();
        }


        @Override
        public void act() {
            action.run();
        }
    }

}
