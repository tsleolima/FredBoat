package fredboat.main;

import fredboat.agent.FredBoatAgent;
import fredboat.agent.StatsAgent;
import fredboat.audio.player.LavalinkManager;
import fredboat.audio.queue.MusicPersistenceHandler;
import fredboat.config.*;
import fredboat.db.DatabaseManager;
import fredboat.db.EntityIO;
import fredboat.event.EventListenerBoat;
import fredboat.feature.metrics.Metrics;
import fredboat.metrics.OkHttpEventMetrics;
import fredboat.util.rest.Http;
import net.dv8tion.jda.bot.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Class responsible for controlling FredBoat at large
 */
@Component
public class BotController {

    public static final Http HTTP = new Http(Http.DEFAULT_BUILDER.newBuilder()
            .eventListener(new OkHttpEventMetrics("default", Metrics.httpEventCounter))
            .build());
    public static final int UNKNOWN_SHUTDOWN_CODE = -991023;

    private static final Logger log = LoggerFactory.getLogger(BotController.class);

    private final PropertyConfigProvider configProvider;
    private final LavalinkManager lavalinkManager;
    private final ShardManager shardManager;
    //central event listener that all events by all shards pass through
    private final EventListenerBoat mainEventListener;

    //unlimited threads = http://i.imgur.com/H3b7H1S.gif
    //use this executor for various small async tasks
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final StatsAgent statsAgent = new StatsAgent("bot metrics");
    private EntityIO entityIO;
    private DatabaseManager databaseManager;
    private int shutdownCode = UNKNOWN_SHUTDOWN_CODE;//Used when specifying the intended code for shutdown hooks


    public BotController(PropertyConfigProvider configProvider, LavalinkManager lavalinkManager, ShardManager shardManager,
                         EventListenerBoat eventListenerBoat) {
        this.configProvider = configProvider;
        this.lavalinkManager = lavalinkManager;
        this.shardManager = shardManager;
        this.mainEventListener = eventListenerBoat;

        Metrics.instance().threadPoolCollector.addPool("main-executor", (ThreadPoolExecutor) executor);
    }

    public AppConfig getAppConfig() {
        return configProvider.getAppConfig();
    }

    public AudioSourcesConfig getAudioSourcesConfig() {
        return configProvider.getAudioSourcesConfig();
    }

    public Credentials getCredentials() {
        return configProvider.getCredentials();
    }

    public DatabaseConfig getDatabaseConfig() {
        return configProvider.getDatabaseConfig();
    }

    public EventLoggerConfig getEventLoggerConfig() {
        return configProvider.getEventLoggerConfig();
    }

    public LavalinkConfig getLavalinkConfig() {
        return configProvider.getLavalinkConfig();
    }

    public LavalinkManager getLavalinkManager() {
        return lavalinkManager;
    }

    @Nonnull
    public ExecutorService getExecutor() {
        return executor;
    }

    public EventListenerBoat getMainEventListener() {
        return mainEventListener;
    }

    @Nonnull
    protected StatsAgent getStatsAgent() {
        return statsAgent;
    }

    // Can be null during init, but usually not
    public ShardManager getShardManager() {
        return shardManager;
    }

    @Nonnull
    public EntityIO getEntityIO() {
        return entityIO;
    }

    public void setEntityIO(@Nonnull EntityIO entityIO) {
        this.entityIO = entityIO;
    }

    @Nonnull
    public DatabaseConnection getMainDbConnection() {
        return databaseManager.getMainDbConn();
    }

    @Nonnull
    public DatabaseWrapper getMainDbWrapper() {
        return databaseManager.getMainDbWrapper();
    }

    @Nullable
    public DatabaseConnection getCacheDbConnection() {
        return databaseManager.getCacheDbConn();
    }

    @Nullable
    public DatabaseWrapper getCacheDbWrapper() {
        return databaseManager.getCacheDbWrapper();
    }

    public void setDatabaseManager(@Nonnull DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void shutdown(int code) {
        log.info("Shutting down with exit code " + code);
        shutdownCode = code;

        System.exit(code);
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

        ShardManager sm = getShardManager();
        if (sm != null) {
            sm.shutdown();
        }

        executor.shutdown();
        if (databaseManager != null) {
            if (databaseManager.isCacheConnBuilt()) {
                DatabaseConnection cacheDbConn = databaseManager.getCacheDbConn();
                if (cacheDbConn != null) {
                    cacheDbConn.shutdown();
                }
            }
            if (databaseManager.isMainConnBuilt()) {
                databaseManager.getMainDbConn().shutdown();
            }
        }
    };

    public int getShutdownCode() {
        return shutdownCode;
    }

}
