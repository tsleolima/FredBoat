package fredboat.main;

import fredboat.agent.FredBoatAgent;
import fredboat.agent.StatsAgent;
import fredboat.audio.queue.MusicPersistenceHandler;
import fredboat.event.EventListenerBoat;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class responsible for controlling FredBoat at large
 */
public class BotController {

    public static final BotController INS = new BotController();
    private static final Logger log = LoggerFactory.getLogger(BotController.class);
    private ShardManager shardManager = null;
    public static final int UNKNOWN_SHUTDOWN_CODE = -991023;

    //unlimited threads = http://i.imgur.com/H3b7H1S.gif
    //use this executor for various small async tasks
    private final ExecutorService executor = Executors.newCachedThreadPool();

    //central event listener that all events by all shards pass through
    private EventListenerBoat mainEventListener = new EventListenerBoat();
    private final StatsAgent jdaEntityCountAgent = new StatsAgent("jda entity counter");
    private final BotMetrics.JdaEntityCounts jdaEntityCountsTotal = new BotMetrics.JdaEntityCounts();
    private DatabaseWrapper mainDbWrapper;
    private int shutdownCode = UNKNOWN_SHUTDOWN_CODE;//Used when specifying the intended code for shutdown hooks

    @Nullable //will be null if no cache database has been configured
    private DatabaseConnection cacheDbConn;

    private List<Shard> shards = new CopyOnWriteArrayList<>();

    public ExecutorService getExecutor() {
        return executor;
    }

    public EventListenerBoat getMainEventListener() {
        return mainEventListener;
    }

    protected void setMainEventListener(EventListenerBoat mainEventListener) {
        this.mainEventListener = mainEventListener;
    }

    protected StatsAgent getJdaEntityCountAgent() {
        return jdaEntityCountAgent;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    @Nonnull
    public DatabaseConnection getMainDbConnection() {
        return mainDbWrapper.unwrap();
    }

    @Nonnull
    public DatabaseWrapper getMainDbWrapper() {
        return mainDbWrapper;
    }

    public DatabaseConnection getCacheDbConnection() {
        return cacheDbConn;
    }

    public void setMainDbWrapper(DatabaseWrapper mainDbWrapper) {
        this.mainDbWrapper = mainDbWrapper;
    }

    @Nullable
    public DatabaseConnection getCacheDbConn() {
        return cacheDbConn;
    }

    public void setCacheDbConn(@Nullable DatabaseConnection cacheDbConn) {
        this.cacheDbConn = cacheDbConn;
    }

    public void shutdown(int code) {
        log.info("Shutting down with exit code " + code);
        shutdownCode = code;

        System.exit(code);
    }

    public BotMetrics.JdaEntityCounts getJdaEntityCountsTotal() {
        return jdaEntityCountsTotal;
    }

    @Nonnull
    public List<Shard> getShards() {
        return Collections.unmodifiableList(shards);
    }

    protected void addShard(int index, @Nonnull Shard shard) {
        shards.add(shard);
    }

    //Shutdown hook
    protected final Runnable shutdownHook = () -> {
        int code = shutdownCode != UNKNOWN_SHUTDOWN_CODE ? shutdownCode : -1;

        FredBoatAgent.shutdown();

        try {
            MusicPersistenceHandler.handlePreShutdown(code);
        } catch (Exception e) {
            log.error("Critical error while handling music persistence.", e);
        }

        for (Shard fb : shards) {
            fb.getJda().shutdown();
        }

        executor.shutdown();
        if (cacheDbConn != null) {
            cacheDbConn.shutdown();
        }
        if (mainDbWrapper != null) {
            mainDbWrapper.unwrap().shutdown();
        }
    };

    public int getShutdownCode() {
        return shutdownCode;
    }

    // ################################################################################
    // ##                           Global lookups
    // ################################################################################

    // TODO: Make nonstatic

    @Nullable
    public static TextChannel getTextChannelById(long id) {
        for (Shard fb : INS.getShards()) {
            TextChannel tc = fb.getJda().getTextChannelById(id);
            if (tc != null) return tc;
        }
        return null;
    }

    @Nullable
    public static VoiceChannel getVoiceChannelById(long id) {
        for (Shard fb : INS.getShards()) {
            VoiceChannel vc = fb.getJda().getVoiceChannelById(id);
            if (vc != null) return vc;
        }
        return null;
    }

    @Nullable
    public static Guild getGuildById(long id) {
        for (Shard fb : INS.getShards()) {
            Guild g = fb.getJda().getGuildById(id);
            if (g != null) return g;
        }
        return null;
    }

    @Nullable
    public static User getUserById(long id) {
        for (Shard fb : INS.getShards()) {
            User u = fb.getJda().getUserById(id);
            if (u != null) return u;
        }
        return null;
    }

    @Nonnull
    public static Shard getShard(@Nonnull JDA jda) {
        int sId = jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();
        for (Shard fb : INS.getShards()) {
            if (fb.getShardId() == sId) {
                return fb;
            }
        }
        throw new IllegalStateException("Attempted to get instance for JDA shard that is not indexed, shardId: " + sId);
    }

    public static Shard getShard(int id) {
        return INS.getShards().get(id);
    }
}
