package fredboat.main;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import fredboat.agent.*;
import fredboat.api.API;
import fredboat.audio.player.LavalinkManager;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.init.MainCommandInitializer;
import fredboat.commandmeta.init.MusicCommandInitializer;
import fredboat.db.DatabaseManager;
import fredboat.event.EventListenerBoat;
import fredboat.feature.I18n;
import fredboat.feature.metrics.Metrics;
import fredboat.shared.constant.DistributionEnum;
import fredboat.shared.constant.ExitCodes;
import fredboat.util.AppInfo;
import fredboat.util.GitRepoState;
import fredboat.util.TextUtils;
import fredboat.util.rest.Http;
import fredboat.util.rest.OpenWeatherAPI;
import fredboat.util.rest.models.weather.RetrievedWeather;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import okhttp3.Credentials;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * The class responsible for launching FredBoat
 */
public class Launcher {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    public static final long START_TIME = System.currentTimeMillis();
    private static final BotController FBC = BotController.INS.postInit();

    public static void main(String[] args) throws IllegalArgumentException, InterruptedException, DatabaseException {
        //just post the info to the console
        if (args.length > 0 &&
                (args[0].equalsIgnoreCase("-v")
                        || args[0].equalsIgnoreCase("--version")
                        || args[0].equalsIgnoreCase("-version"))) {
            System.out.println("Version flag detected. Printing version info, then exiting.");
            System.out.println(getVersionInfo());
            System.out.println("Version info printed, exiting.");
            return;
        }
        Metrics.setup();
        FBC.postInit();

        Runtime.getRuntime().addShutdownHook(new Thread(FBC.shutdownHook, "FredBoat main shutdownhook"));
        log.info(getVersionInfo());

        String javaVersionMinor = null;
        try {
            javaVersionMinor = System.getProperty("java.version").split("\\.")[1];
        } catch (Exception e) {
            log.error("Exception while checking if java 8", e);
        }

        if (!Objects.equals(javaVersionMinor, "8")) {
            log.warn("\n\t\t __      ___   ___ _  _ ___ _  _  ___ \n" +
                    "\t\t \\ \\    / /_\\ | _ \\ \\| |_ _| \\| |/ __|\n" +
                    "\t\t  \\ \\/\\/ / _ \\|   / .` || || .` | (_ |\n" +
                    "\t\t   \\_/\\_/_/ \\_\\_|_\\_|\\_|___|_|\\_|\\___|\n" +
                    "\t\t                                      ");
            log.warn("FredBoat only officially supports Java 8. You are running Java {}", System.getProperty("java.version"));
        }

        try {
            ShardManager shardManager = new DefaultShardManagerBuilder()
                    .setToken(Config.CONFIG.getBotToken())
                    .setShardsTotal(Config.getNumShards())
                    .setContextEnabled(false)
                    .build();

            FBC.setShardManager(shardManager);
        } catch (LoginException e) {
            throw new RuntimeException("Failed to log in to Discord! Is your token invalid?", e);
        }

        I18n.start();

        try {
            API.start();
        } catch (Exception e) {
            log.info("Failed to ignite Spark, FredBoat API unavailable", e);
        }

        //attempt to connect to the database a few times
        // this is relevant in a dockerized environment because after a reboot there is no guarantee that the db
        // container will be started before the fredboat one
        int dbConnectionAttempts = 0;
        DatabaseConnection mainDbConn = null;
        while ((mainDbConn == null || !mainDbConn.isAvailable()) && dbConnectionAttempts++ < 10) {
            try {
                if (mainDbConn != null) {
                    mainDbConn.shutdown();
                }
                mainDbConn = DatabaseManager.main();
            } catch (Exception e) {
                log.info("Could not connect to the database. Retrying in a moment...", e);
                Thread.sleep(6000);
            }
        }
        if (mainDbConn == null || !mainDbConn.isAvailable()) {
            log.error("Could not establish database connection. Exiting...");
            FBC.shutdown(ExitCodes.EXIT_CODE_ERROR);
            return;
        }
        FredBoatAgent.start(new DBConnectionWatchdogAgent(mainDbConn));
        FBC.setMainDbWrapper(new DatabaseWrapper(mainDbConn));

        try {
            FBC.setCacheDbConn(DatabaseManager.cache());
        } catch (Exception e) {
            log.error("Exception when connecting to cache db", e);
            FBC.shutdown(ExitCodes.EXIT_CODE_ERROR);
        }
        Metrics.instance().hibernateStats.register(); //call this exactly once after all db connections have been created

        //Initialise event listeners
        FBC.setMainEventListener(new EventListenerBoat());
        LavalinkManager.ins.start();

        //Commands
        if (Config.CONFIG.getDistribution() == DistributionEnum.DEVELOPMENT)
            MainCommandInitializer.initCommands();

        MusicCommandInitializer.initCommands();

        if (!Config.CONFIG.isPatronDistribution() && Config.CONFIG.useVoiceChannelCleanup()) {
            log.info("Starting VoiceChannelCleanupAgent.");
            FredBoatAgent.start(new VoiceChannelCleanupAgent());
        } else {
            log.info("Skipped setting up the VoiceChannelCleanupAgent, " +
                    "either running Patron distro or overridden by temp config");
        }

        log.info("Loaded commands, registry size is " + CommandRegistry.getSize());

        ExecutorService executor = FBC.getExecutor();



        //Check MAL creds
        executor.submit(Launcher::hasValidMALLogin);

        //Check imgur creds
        executor.submit(Launcher::hasValidImgurCredentials);

        //Check OpenWeather key
        executor.submit(Launcher::hasValidOpenWeatherKey);

        /* Init JDA */
        initBotShards();

        String carbonKey = Config.CONFIG.getCarbonKey();
        if (Config.CONFIG.getDistribution() == DistributionEnum.MUSIC && carbonKey != null && !carbonKey.isEmpty()) {
            FredBoatAgent.start(new CarbonitexAgent(carbonKey));
        }

        //wait for all shards to ready up before requesting a total count of jda entities
        while (!areWeReadyYet()) {
            Thread.sleep(1000);
        }

        //force a count and then turn on metrics to be served
        List<Shard> shards = FBC.getShards();
        StatsAgent jdaEntityCountAgent = FBC.getJdaEntityCountAgent();
        BotMetrics.JdaEntityCounts jdaEntityCountsTotal = FBC.getJdaEntityCountsTotal();
        jdaEntityCountsTotal.count(shards);
        FBC.getJdaEntityCountAgent().addAction(new BotMetrics.FredBoatStatsCounter(
                () -> jdaEntityCountsTotal.count(shards)));
        FredBoatAgent.start(jdaEntityCountAgent);
        API.turnOnMetrics();
    }

    // ################################################################################
    // ##                     Login / credential tests
    // ################################################################################

    private static boolean hasValidMALLogin() {
        String malUser = Config.CONFIG.getMalUser();
        String malPassWord = Config.CONFIG.getMalPassword();
        if (malUser == null || malUser.isEmpty() || malPassWord == null || malPassWord.isEmpty()) {
            log.info("MAL credentials not found. MAL related commands will not be available.");
            return false;
        }

        Http.SimpleRequest request = Http.get("https://myanimelist.net/api/account/verify_credentials.xml")
                .auth(Credentials.basic(malUser, malPassWord));

        try (Response response = request.execute()) {
            if (response.isSuccessful()) {
                log.info("MAL login successful");
                return true;
            } else {
                //noinspection ConstantConditions
                log.warn("MAL login failed with {}\n{}", response.toString(), response.body().string());
            }
        } catch (IOException e) {
            log.warn("MAL login failed, it seems to be down.", e);
        }
        return false;
    }

    private static boolean hasValidImgurCredentials() {
        String imgurClientId = Config.CONFIG.getImgurClientId();
        if (imgurClientId == null || imgurClientId.isEmpty()) {
            log.info("Imgur credentials not found. Commands relying on Imgur will not work properly.");
            return false;
        }
        Http.SimpleRequest request = Http.get("https://api.imgur.com/3/credits")
                .auth("Client-ID " + imgurClientId);
        try (Response response = request.execute()) {
            //noinspection ConstantConditions
            String content = response.body().string();
            if (response.isSuccessful()) {
                JSONObject data = new JSONObject(content).getJSONObject("data");
                //https://api.imgur.com/#limits
                //at the time of the introduction of this code imgur offers daily 12500 and hourly 500 GET requests for open source software
                //hitting the daily limit 5 times in a month will blacklist the app for the rest of the month
                //we use 3 requests per hour (and per restart of the bot), so there should be no problems with imgur's rate limit
                int hourlyLimit = data.getInt("UserLimit");
                int hourlyLeft = data.getInt("UserRemaining");
                long seconds = data.getLong("UserReset") - (System.currentTimeMillis() / 1000);
                String timeTillReset = String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
                int dailyLimit = data.getInt("ClientLimit");
                int dailyLeft = data.getInt("ClientRemaining");
                log.info("Imgur credentials are valid. " + hourlyLeft + "/" + hourlyLimit +
                        " requests remaining this hour, resetting in " + timeTillReset + ", " +
                        dailyLeft + "/" + dailyLimit + " requests remaining today.");
                return true;
            } else {
                log.warn("Imgur login failed with {}\n{}", response.toString(), content);
            }
        } catch (IOException e) {
            log.warn("Imgur login failed, it seems to be down.", e);
        }
        return false;
    }

    /**
     * Method to check if there is an error to retrieve open weather data.
     *
     * @return True if it can retrieve data, else return false.
     */
    private static boolean hasValidOpenWeatherKey() {
        if ("".equals(Config.CONFIG.getOpenWeatherKey())) {
            log.warn("Open Weather API credentials not found. Weather related commands will not work properly.");
            return false;
        }

        OpenWeatherAPI api = new OpenWeatherAPI();
        RetrievedWeather weather = api.getCurrentWeatherByCity("san francisco");

        boolean isSuccess = !(weather == null || weather.isError());

        if (isSuccess) {
            log.info("Open Weather API check successful");
        } else {
            log.warn("Open Weather API check failed. It may be down, the provided credentials may be invalid, or temporarily blocked.");
        }
        return isSuccess;
    }

    private static void initBotShards() {
        for (int i = 0; i < Config.getNumShards(); i++) {
            try {
                //NOTE: This will take a while since creating shards happens in a blocking fashion
                FBC.addShard(i, new Shard(i));
            } catch (Exception e) {
                //todo this is fatal and requires a restart to fix, so either remove it by guaranteeing that
                //todo shard creation never fails, or have a proper handling for it
                log.error("Caught an exception while starting shard {}!", i, e);
            }
        }

        log.info(FBC.getShards().size() + " shards have been constructed");

    }

    //returns true if all registered shards are reporting back as CONNECTED, false otherwise
    private static boolean areWeReadyYet() {
        for (Shard shard : FBC.getShards()) {
            if (shard.getJda().getStatus() != JDA.Status.CONNECTED) {
                return false;
            }
        }
        return true;
    }

    private static String getVersionInfo() {
        return "\n\n" +
                "  ______            _ ____              _   \n" +
                " |  ____|          | |  _ \\            | |  \n" +
                " | |__ _ __ ___  __| | |_) | ___   __ _| |_ \n" +
                " |  __| '__/ _ \\/ _` |  _ < / _ \\ / _` | __|\n" +
                " | |  | | |  __/ (_| | |_) | (_) | (_| | |_ \n" +
                " |_|  |_|  \\___|\\__,_|____/ \\___/ \\__,_|\\__|\n\n"

                + "\n\tVersion:       " + AppInfo.getAppInfo().VERSION
                + "\n\tBuild:         " + AppInfo.getAppInfo().BUILD_NUMBER
                + "\n\tCommit:        " + GitRepoState.getGitRepositoryState().commitIdAbbrev + " (" + GitRepoState.getGitRepositoryState().branch + ")"
                + "\n\tCommit time:   " + TextUtils.asTimeInCentralEurope(GitRepoState.getGitRepositoryState().commitTime * 1000)
                + "\n\tJVM:           " + System.getProperty("java.version")
                + "\n\tJDA:           " + JDAInfo.VERSION
                + "\n\tLavaplayer     " + PlayerLibrary.VERSION
                + "\n";
    }

}
