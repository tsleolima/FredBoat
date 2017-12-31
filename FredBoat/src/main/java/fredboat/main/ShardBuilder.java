package fredboat.main;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import fredboat.audio.player.LavalinkManager;
import fredboat.event.EventLogger;
import fredboat.feature.DikeSessionController;
import fredboat.feature.metrics.Metrics;
import fredboat.feature.metrics.OkHttpEventMetrics;
import fredboat.util.rest.Http;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.utils.SessionController;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class ShardBuilder {

    private static final Logger log = LoggerFactory.getLogger(ShardBuilder.class);
    private static volatile SessionController sessionController = null;

    public static JDA buildJDA(int shardId, boolean blocking) {
        JDA newJda = null;
        JDABuilder builder = getBuilder();

        try {
            boolean success = false;
            while (!success) {
                builder.useSharding(shardId, Config.getNumShards());

                try {
                    newJda = blocking ? builder.buildBlocking() : builder.buildAsync();
                    success = true;
                } catch (Exception e) {
                    log.error("Generic exception when building a JDA instance! Retrying...", e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start JDA shard " + shardId, e);
        }

        return newJda;
    }

    @Nonnull
    private synchronized static JDABuilder getBuilder() {
        JDABuilder builder;

        if (sessionController == null)
            sessionController = Config.CONFIG.getDikeUrl() == null
                    ? new SessionControllerAdapter()
                    : new DikeSessionController();

        builder = new JDABuilder(AccountType.BOT)
                .setToken(Config.CONFIG.getBotToken())
                .setGame(Game.playing(Config.CONFIG.getGame()))
                .setBulkDeleteSplittingEnabled(false)
                .setEnableShutdownHook(false)
                .setAudioEnabled(true)
                .setAutoReconnect(true)
                .setSessionController(sessionController)
                .setContextEnabled(false)
                .setHttpClientBuilder(Http.defaultHttpClient.newBuilder())
                .setHttpClientBuilder(new OkHttpClient.Builder()
                        .eventListener(new OkHttpEventMetrics("jda")))
                .addEventListener(BotController.INS.getMainEventListener())
                .addEventListener(Metrics.instance().jdaEventsMetricsListener);

        String eventLogWebhook = Config.CONFIG.getEventLogWebhook();
        if (eventLogWebhook != null && !eventLogWebhook.isEmpty()) {
            try {
                builder.addEventListener(new EventLogger());
            } catch (Exception e) {
                log.error("Failed to create Eventlogger, events will not be logged to discord via webhook", e);
            }
        }

        if (LavalinkManager.ins.isEnabled()) {
            builder.addEventListener(LavalinkManager.ins.getLavalink());
        }

        if (!System.getProperty("os.arch").equalsIgnoreCase("arm")
                && !System.getProperty("os.arch").equalsIgnoreCase("arm-linux")) {
            builder.setAudioSendFactory(new NativeAudioSendFactory(800));
        }

        return builder;
    }

}
