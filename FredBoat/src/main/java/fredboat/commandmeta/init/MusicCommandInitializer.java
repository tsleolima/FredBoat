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
 */

package fredboat.commandmeta.init;

import fredboat.Config;
import fredboat.agent.VoiceChannelCleanupAgent;
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
import fredboat.command.util.CommandsCommand;
import fredboat.command.util.HelpCommand;
import fredboat.command.util.MusicHelpCommand;
import fredboat.command.util.UserInfoCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.perms.PermissionLevel;
import fredboat.shared.constant.DistributionEnum;
import fredboat.util.rest.SearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicCommandInitializer {

    private static final Logger log = LoggerFactory.getLogger(MusicCommandInitializer.class);

    public static void initCommands() {
        CommandRegistry.MUSIC.registerCommand("help", new HelpCommand(), "info");
        CommandRegistry.MUSIC.registerCommand("music", new MusicHelpCommand(), "musichelp");
        CommandRegistry.MUSIC.registerCommand("commands", new CommandsCommand(), "comms", "cmds");
        
        /* Control */
        CommandRegistry.MUSIC.registerCommand("play", new PlayCommand(SearchUtil.SearchProvider.YOUTUBE, SearchUtil.SearchProvider.SOUNDCLOUD), "p");
        CommandRegistry.MUSIC.registerCommand("yt", new PlayCommand(SearchUtil.SearchProvider.YOUTUBE), "youtube");
        CommandRegistry.MUSIC.registerCommand("sc", new PlayCommand(SearchUtil.SearchProvider.SOUNDCLOUD), "soundcloud");
        CommandRegistry.MUSIC.registerCommand("skip", new SkipCommand(), "sk", "s");
        CommandRegistry.MUSIC.registerCommand("join", new JoinCommand(), "summon", "jn");
        CommandRegistry.MUSIC.registerCommand("leave", new LeaveCommand(), "lv");
        CommandRegistry.MUSIC.registerCommand("select", new SelectCommand(), buildNumericalSelectAllias("sel"));
        CommandRegistry.MUSIC.registerCommand("stop", new StopCommand(), "st");
        CommandRegistry.MUSIC.registerCommand("pause", new PauseCommand(), "pa", "ps");
        CommandRegistry.MUSIC.registerCommand("shuffle", new ShuffleCommand(), "sh");
        CommandRegistry.MUSIC.registerCommand("reshuffle", new ReshuffleCommand(), "resh");
        CommandRegistry.MUSIC.registerCommand("repeat", new RepeatCommand(), "rep");
        CommandRegistry.MUSIC.registerCommand("volume", new VolumeCommand(), "vol");
        CommandRegistry.MUSIC.registerCommand("unpause", new UnpauseCommand(), "unp", "resume");
        CommandRegistry.MUSIC.registerCommand("split", new PlaySplitCommand());
        CommandRegistry.MUSIC.registerCommand("destroy", new DestroyCommand());
        
        /* Info */
        CommandRegistry.MUSIC.registerCommand("nowplaying", new NowplayingCommand(), "np");
        CommandRegistry.MUSIC.registerCommand("list", new ListCommand(), "queue", "q", "l");
        CommandRegistry.MUSIC.registerCommand("export", new ExportCommand(), "ex");
        CommandRegistry.MUSIC.registerCommand("gr", new GensokyoRadioCommand(), "gensokyo", "gensokyoradio");
        CommandRegistry.MUSIC.registerCommand("muserinfo", new UserInfoCommand());

        /* Seeking */
        CommandRegistry.MUSIC.registerCommand("seek", new SeekCommand());
        CommandRegistry.MUSIC.registerCommand("forward", new ForwardCommand(), "fwd");
        CommandRegistry.MUSIC.registerCommand("rewind", new RewindCommand(), "rew");
        CommandRegistry.MUSIC.registerCommand("restart", new RestartCommand());
        
        /* Bot Maintenance Commands */
        CommandRegistry.MUSIC.registerCommand("mgitinfo", new GitInfoCommand(), "mgit");
        CommandRegistry.MUSIC.registerCommand("munblacklist", new UnblacklistCommand(), "munlimit");
        CommandRegistry.MUSIC.registerCommand("mexit", new ExitCommand());
        CommandRegistry.MUSIC.registerCommand("mbotrestart", new BotRestartCommand());
        CommandRegistry.MUSIC.registerCommand("mstats", new StatsCommand());
        CommandRegistry.MUSIC.registerCommand("meval", new EvalCommand());
        CommandRegistry.MUSIC.registerCommand("mupdate", new UpdateCommand());
        CommandRegistry.MUSIC.registerCommand("mcompile", new CompileCommand());
        CommandRegistry.MUSIC.registerCommand("mmvntest", new MavenTestCommand());
        CommandRegistry.MUSIC.registerCommand("getid", new GetIdCommand());
        CommandRegistry.MUSIC.registerCommand("playerdebug", new PlayerDebugCommand());
        CommandRegistry.MUSIC.registerCommand("nodes", new NodesCommand());
        CommandRegistry.MUSIC.registerCommand("mshards", new ShardsCommand());
        CommandRegistry.MUSIC.registerCommand("mrevive", new ReviveCommand());
        CommandRegistry.MUSIC.registerCommand("msentrydsn", new SentryDsnCommand());
        CommandRegistry.MUSIC.registerCommand("adebug", new AudioDebugCommand());
        CommandRegistry.MUSIC.registerCommand("announce", new AnnounceCommand());
        CommandRegistry.MUSIC.registerCommand("mping", new PingCommand());
        CommandRegistry.MUSIC.registerCommand("node", new NodeAdminCommand());
        
        /* Bot configuration */
        CommandRegistry.MUSIC.registerCommand("config", new ConfigCommand(), "cfg");
        CommandRegistry.MUSIC.registerCommand("lang", new LanguageCommand(), "language");
        
        /* Perms */
        CommandRegistry.MUSIC.registerCommand("admin", new PermissionsCommand(PermissionLevel.ADMIN));
        CommandRegistry.MUSIC.registerCommand("dj", new PermissionsCommand(PermissionLevel.DJ));
        CommandRegistry.MUSIC.registerCommand("user", new PermissionsCommand(PermissionLevel.USER));

        // The null check is to ensure we can run this in a test run
        if (Config.CONFIG == null || Config.CONFIG.getDistribution() != DistributionEnum.PATRON) {
            new VoiceChannelCleanupAgent().start();
        } else {
            log.info("Skipped setting up the VoiceChannelCleanupAgent since we are running as PATRON distribution.");
        }
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
