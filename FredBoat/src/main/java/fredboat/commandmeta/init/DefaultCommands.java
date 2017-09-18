package fredboat.commandmeta.init;

import fredboat.command.admin.*;
import fredboat.command.maintenance.*;
import fredboat.command.moderation.ConfigCommand;
import fredboat.command.moderation.LanguageCommand;
import fredboat.command.moderation.PermissionsCommand;
import fredboat.command.music.control.*;
import fredboat.command.music.info.ExportCommand;
import fredboat.command.music.info.GensokyoRadioCommand;
import fredboat.command.music.info.ListCommand;
import fredboat.command.music.info.NowplayingCommand;
import fredboat.command.music.seeking.ForwardCommand;
import fredboat.command.music.seeking.RestartCommand;
import fredboat.command.music.seeking.RewindCommand;
import fredboat.command.music.seeking.SeekCommand;
import fredboat.command.util.*;
import fredboat.commandmeta.CommandRegistry;
import fredboat.perms.PermissionLevel;
import fredboat.util.rest.SearchUtil;

public class DefaultCommands {

    public static void initCommands() {
        /* Info commands */
        CommandRegistry.registerCommand("help", new HelpCommand(), "info");
        CommandRegistry.registerCommand("music", new MusicHelpCommand(), "musichelp");
        CommandRegistry.registerCommand("commands", new CommandsCommand(), "comms", "cmds");
        CommandRegistry.registerCommand("invite", new InviteCommand());

        /* Music control */
        CommandRegistry.registerCommand("play", new PlayCommand(SearchUtil.SearchProvider.YOUTUBE, SearchUtil.SearchProvider.SOUNDCLOUD), "p");
        CommandRegistry.registerCommand("yt", new PlayCommand(SearchUtil.SearchProvider.YOUTUBE), "youtube");
        CommandRegistry.registerCommand("sc", new PlayCommand(SearchUtil.SearchProvider.SOUNDCLOUD), "soundcloud");
        CommandRegistry.registerCommand("skip", new SkipCommand(), "sk", "s");
        CommandRegistry.registerCommand("join", new JoinCommand(), "summon", "jn");
        CommandRegistry.registerCommand("leave", new LeaveCommand(), "lv");
        CommandRegistry.registerCommand("select", new SelectCommand(), buildNumericalSelectAllias("sel"));
        CommandRegistry.registerCommand("stop", new StopCommand(), "st");
        CommandRegistry.registerCommand("pause", new PauseCommand(), "pa", "ps");
        CommandRegistry.registerCommand("shuffle", new ShuffleCommand(), "sh");
        CommandRegistry.registerCommand("reshuffle", new ReshuffleCommand(), "resh");
        CommandRegistry.registerCommand("repeat", new RepeatCommand(), "rep");
        CommandRegistry.registerCommand("volume", new VolumeCommand(), "vol");
        CommandRegistry.registerCommand("unpause", new UnpauseCommand(), "unp", "resume");
        CommandRegistry.registerCommand("split", new PlaySplitCommand());
        CommandRegistry.registerCommand("destroy", new DestroyCommand());

        /* Music info */
        CommandRegistry.registerCommand("nowplaying", new NowplayingCommand(), "np");
        CommandRegistry.registerCommand("list", new ListCommand(), "queue", "q", "l");
        CommandRegistry.registerCommand("export", new ExportCommand(), "ex");
        CommandRegistry.registerCommand("gr", new GensokyoRadioCommand(), "gensokyo", "gensokyoradio");
        CommandRegistry.registerCommand("muserinfo", new UserInfoCommand());

        /* Seeking */
        CommandRegistry.registerCommand("seek", new SeekCommand());
        CommandRegistry.registerCommand("forward", new ForwardCommand(), "fwd");
        CommandRegistry.registerCommand("rewind", new RewindCommand(), "rew");
        CommandRegistry.registerCommand("restart", new RestartCommand());

        /* Bot Maintenance Commands */
        CommandRegistry.registerCommand("gitinfo", new GitInfoCommand(), "mgit");
        CommandRegistry.registerCommand("unblacklist", new UnblacklistCommand(), "munlimit");
        CommandRegistry.registerCommand("exit", new ExitCommand());
        CommandRegistry.registerCommand("botrestart", new BotRestartCommand());
        CommandRegistry.registerCommand("stats", new StatsCommand());
        CommandRegistry.registerCommand("eval", new EvalCommand());
        CommandRegistry.registerCommand("update", new UpdateCommand());
        CommandRegistry.registerCommand("compile", new CompileCommand());
        CommandRegistry.registerCommand("mvntest", new MavenTestCommand());
        CommandRegistry.registerCommand("getid", new GetIdCommand());
        CommandRegistry.registerCommand("playerdebug", new PlayerDebugCommand());
        CommandRegistry.registerCommand("nodes", new NodesCommand());
        CommandRegistry.registerCommand("shards", new ShardsCommand());
        CommandRegistry.registerCommand("revive", new ReviveCommand());
        CommandRegistry.registerCommand("sentrydsn", new SentryDsnCommand());
        CommandRegistry.registerCommand("adebug", new AudioDebugCommand());
        CommandRegistry.registerCommand("announce", new AnnounceCommand());
        CommandRegistry.registerCommand("ping", new PingCommand());
        CommandRegistry.registerCommand("node", new NodeAdminCommand());

        /* Bot Maintenance Commands but with some "m" prefixes to prevent confusion */
        CommandRegistry.registerCommand("mgitinfo", new GitInfoCommand(), "mgit");
        CommandRegistry.registerCommand("munblacklist", new UnblacklistCommand(), "munlimit");
        CommandRegistry.registerCommand("mexit", new ExitCommand());
        CommandRegistry.registerCommand("mbotrestart", new BotRestartCommand());
        CommandRegistry.registerCommand("mstats", new StatsCommand());
        CommandRegistry.registerCommand("meval", new EvalCommand());
        CommandRegistry.registerCommand("mupdate", new UpdateCommand());
        CommandRegistry.registerCommand("mcompile", new CompileCommand());
        CommandRegistry.registerCommand("mmvntest", new MavenTestCommand());
        CommandRegistry.registerCommand("mshards", new ShardsCommand());
        CommandRegistry.registerCommand("mrevive", new ReviveCommand());
        CommandRegistry.registerCommand("msentrydsn", new SentryDsnCommand());
        CommandRegistry.registerCommand("mping", new PingCommand());

        /* Bot configuration */
        CommandRegistry.registerCommand("config", new ConfigCommand(), "cfg");
        CommandRegistry.registerCommand("lang", new LanguageCommand(), "language");

        /* Perms */
        CommandRegistry.registerCommand("admin", new PermissionsCommand(PermissionLevel.ADMIN));
        CommandRegistry.registerCommand("dj", new PermissionsCommand(PermissionLevel.DJ));
        CommandRegistry.registerCommand("user", new PermissionsCommand(PermissionLevel.USER));
    }

    /**
     * Build a string array that consist of the max number of searches.
     *
     * @param extraAliases Aliases to be appended to the rest of the ones being built.
     * @return String array that contains string representation of numbers with addOnAliases.
     */
    private static String[] buildNumericalSelectAllias(String... extraAliases) {
        String[] selectTrackAliases = new String[SearchUtil.MAX_RESULTS + extraAliases.length];
        int i = 0;
        for (; i < extraAliases.length; i++) {
            selectTrackAliases[i] = extraAliases[i];
        }
        for (; i < SearchUtil.MAX_RESULTS + extraAliases.length; i++) {
            selectTrackAliases[i] = String.valueOf(i - extraAliases.length + 1);
        }
        return selectTrackAliases;
    }

}
