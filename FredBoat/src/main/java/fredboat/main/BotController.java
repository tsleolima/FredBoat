package fredboat.main;

import fredboat.agent.FredBoatAgent;
import fredboat.agent.StatsAgent;
import fredboat.audio.player.LavalinkManager;
import fredboat.audio.player.PlayerRegistry;
import fredboat.config.property.*;
import fredboat.db.DatabaseManager;
import fredboat.db.EntityIO;
import fredboat.event.EventListenerBoat;
import fredboat.feature.metrics.Metrics;
import fredboat.metrics.OkHttpEventMetrics;
import fredboat.util.rest.Http;
import io.prometheus.client.hibernate.HibernateStatisticsCollector;
import net.dv8tion.jda.bot.sharding.ShardManager;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;

/**
 * Class responsible for controlling FredBoat at large
 */
@Component
public class BotController {

    public static final Http HTTP = new Http(Http.DEFAULT_BUILDER.newBuilder()
            .eventListener(new OkHttpEventMetrics("default", Metrics.httpEventCounter))
            .build());

    private final PropertyConfigProvider configProvider;
    private final LavalinkManager lavalinkManager;
    private final ShardManager shardManager;
    //central event listener that all events by all shards pass through
    private final EventListenerBoat mainEventListener;
    private final ShutdownHandler shutdownHandler;
    private final DatabaseManager databaseManager;
    private final EntityIO entityIO;
    private final PlayerRegistry playerRegistry;
    private final ExecutorService executor;


    private final StatsAgent statsAgent = new StatsAgent("bot metrics");

    public BotController(PropertyConfigProvider configProvider, LavalinkManager lavalinkManager, ShardManager shardManager,
                         EventListenerBoat eventListenerBoat, ShutdownHandler shutdownHandler, DatabaseManager databaseManager,
                         EntityIO entityIO, ExecutorService executor, HibernateStatisticsCollector hibernateStats,
                         PlayerRegistry playerRegistry) {
        this.configProvider = configProvider;
        this.lavalinkManager = lavalinkManager;
        this.shardManager = shardManager;
        this.mainEventListener = eventListenerBoat;
        this.shutdownHandler = shutdownHandler;
        this.databaseManager = databaseManager;
        this.entityIO = entityIO;
        this.playerRegistry = playerRegistry;
        hibernateStats.register(); //call this exactly once after all db connections have been created
        this.executor = executor;

        Runtime.getRuntime().addShutdownHook(new Thread(createShutdownHook(), "FredBoat main shutdownhook"));
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

    public ShutdownHandler getShutdownHandler() {
        return shutdownHandler;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
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

    public PlayerRegistry getPlayerRegistry() {
        return playerRegistry;
    }

    //Shutdown hook
    private Runnable createShutdownHook() {
        return () -> {
            FredBoatAgent.shutdown();
        };
    }
}
