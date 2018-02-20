package fredboat.main;

import com.google.common.annotations.VisibleForTesting;
import fredboat.agent.FredBoatAgent;
import fredboat.agent.StatsAgent;
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
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Class responsible for controlling FredBoat at large
 */
public class BotController {

    public static final BotController INS = new BotController();

    public static final Http HTTP = new Http(Http.DEFAULT_BUILDER.newBuilder()
            .eventListener(new OkHttpEventMetrics("default", Metrics.httpEventCounter))
            .build());

    private static final Logger log = LoggerFactory.getLogger(BotController.class);
    private ShardManager shardManager = null;
    public static final int UNKNOWN_SHUTDOWN_CODE = -991023;

    //unlimited threads = http://i.imgur.com/H3b7H1S.gif
    //use this executor for various small async tasks
    private final ExecutorService executor = Executors.newCachedThreadPool();

    //central event listener that all events by all shards pass through
    private EventListenerBoat mainEventListener;
    private final StatsAgent statsAgent = new StatsAgent("bot metrics");
    private EntityIO entityIO;
    private DatabaseManager databaseManager;
    private int shutdownCode = UNKNOWN_SHUTDOWN_CODE;//Used when specifying the intended code for shutdown hooks


    private Supplier<AppConfig> appConfigSupplier = FileConfig::get;
    private Supplier<AudioSourcesConfig> audioSourcesConfigSupplier = FileConfig::get;
    private Supplier<Credentials> credentialsSupplier = FileConfig::get;
    private Supplier<DatabaseConfig> databaseConfigSupplier = FileConfig::get;
    private Supplier<EventLoggerConfig> eventLoggerConfigSupplier = FileConfig::get;
    private Supplier<LavalinkConfig> lavalinkConfigSupplier = FileConfig::get;

    @VisibleForTesting
    @SuppressWarnings("unchecked")
    public void setConfigSuppliers(Supplier allConfigsSupplier) {
        appConfigSupplier = allConfigsSupplier;
        audioSourcesConfigSupplier = allConfigsSupplier;
        credentialsSupplier = allConfigsSupplier;
        databaseConfigSupplier = allConfigsSupplier;
        eventLoggerConfigSupplier = allConfigsSupplier;
        lavalinkConfigSupplier = allConfigsSupplier;
    }

    public AppConfig getAppConfig() {
        return appConfigSupplier.get();
    }

    public AudioSourcesConfig getAudioSourcesConfig() {
        return audioSourcesConfigSupplier.get();
    }

    public Credentials getCredentials() {
        return credentialsSupplier.get();
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfigSupplier.get();
    }

    public EventLoggerConfig getEventLoggerConfig() {
        return eventLoggerConfigSupplier.get();
    }

    public LavalinkConfig getLavalinkConfig() {
        return lavalinkConfigSupplier.get();
    }

    /**
     * Initialises the event listener. This can't be done during construction,
     *   since that causes an NPE as ins is null during that time
     */
    void postInit() {
        mainEventListener = new EventListenerBoat();
    }

    void setShardManager(@Nonnull ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Nonnull
    public ExecutorService getExecutor() {
        return executor;
    }

    public EventListenerBoat getMainEventListener() {
        return mainEventListener;
    }

    protected void setMainEventListener(@Nonnull EventListenerBoat mainEventListener) {
        this.mainEventListener = mainEventListener;
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

        if (shardManager != null) {
            shardManager.shutdown();
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
