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

import fredboat.command.admin.*;
import fredboat.command.fun.*;
import fredboat.command.maintenance.*;
import fredboat.command.moderation.ClearCommand;
import fredboat.command.moderation.HardbanCommand;
import fredboat.command.moderation.KickCommand;
import fredboat.command.moderation.SoftbanCommand;
import fredboat.command.util.*;
import fredboat.commandmeta.CommandRegistry;
import fredboat.util.AsciiArtConstant;

public class MainCommandInitializer {

    public static void initCommands() {
        CommandRegistry.NON_MUSIC.registerCommand("help", new HelpCommand(), "info");
        CommandRegistry.NON_MUSIC.registerCommand("commands", new CommandsCommand(), "comms", "cmds");
        CommandRegistry.NON_MUSIC.registerCommand("invite", new InviteCommand());
        
        /* Bot Maintenance */
        CommandRegistry.NON_MUSIC.registerCommand("unblacklist", new UnblacklistCommand(), "unlimit");
        CommandRegistry.NON_MUSIC.registerCommand("version", new VersionCommand());
        CommandRegistry.NON_MUSIC.registerCommand("uptime", new StatsCommand(), "stats");
        CommandRegistry.NON_MUSIC.registerCommand("update", new UpdateCommand());
        CommandRegistry.NON_MUSIC.registerCommand("compile", new CompileCommand());
        CommandRegistry.NON_MUSIC.registerCommand("mvntest", new MavenTestCommand());
        CommandRegistry.NON_MUSIC.registerCommand("botrestart", new BotRestartCommand());
        CommandRegistry.NON_MUSIC.registerCommand("eval", new EvalCommand());
        CommandRegistry.NON_MUSIC.registerCommand("shards", new ShardsCommand());
        CommandRegistry.NON_MUSIC.registerCommand("revive", new ReviveCommand());
        CommandRegistry.NON_MUSIC.registerCommand("sentrydsn", new SentryDsnCommand());
        CommandRegistry.NON_MUSIC.registerCommand("test", new TestCommand());
        CommandRegistry.NON_MUSIC.registerCommand("gitinfo", new GitInfoCommand(), "git");
        CommandRegistry.NON_MUSIC.registerCommand("exit", new ExitCommand());
        
        /* Moderation */
        CommandRegistry.NON_MUSIC.registerCommand("hardban", new HardbanCommand());
        CommandRegistry.NON_MUSIC.registerCommand("kick", new KickCommand());
        CommandRegistry.NON_MUSIC.registerCommand("softban", new SoftbanCommand());
        CommandRegistry.NON_MUSIC.registerCommand("clear", new ClearCommand());
        
        /* Util */
        CommandRegistry.NON_MUSIC.registerCommand("serverinfo", new fredboat.command.util.ServerInfoCommand(), "guildinfo");
        CommandRegistry.NON_MUSIC.registerCommand("userinfo", new fredboat.command.util.UserInfoCommand(), "memberinfo");
        CommandRegistry.NON_MUSIC.registerCommand("ping", new PingCommand());
        CommandRegistry.NON_MUSIC.registerCommand("fuzzy", new FuzzyUserSearchCommand());
        
        /* Fun Commands */
        CommandRegistry.NON_MUSIC.registerCommand("joke", new JokeCommand(), "jk");
        CommandRegistry.NON_MUSIC.registerCommand("riot", new RiotCommand());
        CommandRegistry.NON_MUSIC.registerCommand("dance", new DanceCommand());
        CommandRegistry.NON_MUSIC.registerCommand("talk", new TalkCommand());
        CommandRegistry.NON_MUSIC.registerCommand("akinator", new AkinatorCommand());
        CommandRegistry.NON_MUSIC.registerCommand("catgirl", new CatgirlCommand(), "neko", "catgrill");
        CommandRegistry.NON_MUSIC.registerCommand("avatar", new AvatarCommand(), "ava");
        CommandRegistry.NON_MUSIC.registerCommand("say", new SayCommand());

        /* Other Anime Discord, Sergi memes or any other memes */
        // saved in this album https://imgur.com/a/wYvDu
        CommandRegistry.NON_MUSIC.registerCommand("ram", new RemoteFileCommand("http://i.imgur.com/DYToB2e.jpg"));
        CommandRegistry.NON_MUSIC.registerCommand("welcome", new RemoteFileCommand("http://i.imgur.com/utPRe0e.gif"));
        CommandRegistry.NON_MUSIC.registerCommand("rude", new RemoteFileCommand("http://i.imgur.com/j8VvjOT.png"));
        CommandRegistry.NON_MUSIC.registerCommand("fuck", new RemoteFileCommand("http://i.imgur.com/oJL7m7m.png"));
        CommandRegistry.NON_MUSIC.registerCommand("idc", new RemoteFileCommand("http://i.imgur.com/BrCCbfx.png"));
        CommandRegistry.NON_MUSIC.registerCommand("beingraped", new RemoteFileCommand("http://i.imgur.com/jjoz783.png"));
        CommandRegistry.NON_MUSIC.registerCommand("anime", new RemoteFileCommand("http://i.imgur.com/93VahIh.png"));
        CommandRegistry.NON_MUSIC.registerCommand("wow", new RemoteFileCommand("http://i.imgur.com/w7x1885.png"));
        CommandRegistry.NON_MUSIC.registerCommand("what", new RemoteFileCommand("http://i.imgur.com/GNsAxkh.png"));
        CommandRegistry.NON_MUSIC.registerCommand("pun", new RemoteFileCommand("http://i.imgur.com/sBfq3wM.png"));
        CommandRegistry.NON_MUSIC.registerCommand("cancer", new RemoteFileCommand("http://i.imgur.com/pQiT26t.jpg"));
        CommandRegistry.NON_MUSIC.registerCommand("stupidbot", new RemoteFileCommand("http://i.imgur.com/YT1Bkhj.png"));
        CommandRegistry.NON_MUSIC.registerCommand("escape", new RemoteFileCommand("http://i.imgur.com/QmI469j.png"));
        CommandRegistry.NON_MUSIC.registerCommand("explosion", new RemoteFileCommand("http://i.imgur.com/qz6g1vj.gif"));
        CommandRegistry.NON_MUSIC.registerCommand("gif", new RemoteFileCommand("http://i.imgur.com/eBUFNJq.gif"));
        CommandRegistry.NON_MUSIC.registerCommand("noods", new RemoteFileCommand("http://i.imgur.com/mKdTGlg.png"));
        CommandRegistry.NON_MUSIC.registerCommand("internetspeed", new RemoteFileCommand("http://i.imgur.com/84nbpQe.png"));
        CommandRegistry.NON_MUSIC.registerCommand("powerpoint", new RemoteFileCommand("http://i.imgur.com/i65ss6p.png"));
        
        /* Text Faces & Unicode 'Art' & ASCII 'Art' and Stuff */
        CommandRegistry.NON_MUSIC.registerCommand("shr", new TextCommand("¯\\_(ツ)_/¯"), "shrug");
        CommandRegistry.NON_MUSIC.registerCommand("faceofdisapproval", new TextCommand("ಠ_ಠ"), "fod", "disapproving");
        CommandRegistry.NON_MUSIC.registerCommand("sendenergy", new TextCommand("༼ つ ◕_◕ ༽つ"));
        CommandRegistry.NON_MUSIC.registerCommand("dealwithit", new TextCommand("(•\\_•) ( •\\_•)>⌐■-■ (⌐■_■)"), "dwi");
        CommandRegistry.NON_MUSIC.registerCommand("channelingenergy", new TextCommand("(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧ ✧ﾟ･: *ヽ(◕ヮ◕ヽ)"));
        CommandRegistry.NON_MUSIC.registerCommand("butterfly", new TextCommand("Ƹ̵̡Ӝ̵̨̄Ʒ"));
        CommandRegistry.NON_MUSIC.registerCommand("angrytableflip", new TextCommand("(ノಠ益ಠ)ノ彡┻━┻"), "tableflipbutangry", "atp");
        CommandRegistry.NON_MUSIC.registerCommand("dog", new TextCommand(AsciiArtConstant.DOG), "cooldog", "dogmeme");
        CommandRegistry.NON_MUSIC.registerCommand("lood", new TextCommand("T-that's l-lewd, baka!!!"), "lewd", "l00d");
        CommandRegistry.NON_MUSIC.registerCommand("useless", new TextCommand("This command is useless."));
        CommandRegistry.NON_MUSIC.registerCommand("swtf", new TextCommand("¯\\\\(°_o)/¯"), "shrugwtf");
        CommandRegistry.NON_MUSIC.registerCommand("hurray", new TextCommand("ヽ(^o^)ノ"), "yay", "woot");
        // Lennies
        CommandRegistry.NON_MUSIC.registerCommand("spiderlenny", new TextCommand("/╲/╭( ͡° ͡° ͜ʖ ͡° ͡°)╮/╱\\"));
        CommandRegistry.NON_MUSIC.registerCommand("lenny", new TextCommand("( ͡° ͜ʖ ͡°)"));
        CommandRegistry.NON_MUSIC.registerCommand("peeking", new TextCommand("┬┴┬┴┤ ͜ʖ ͡°) ├┬┴┬┴"), "peekinglenny", "peek");
        CommandRegistry.NON_MUSIC.registerCommand("magicallenny", new TextCommand(AsciiArtConstant.MAGICAL_LENNY), "lennymagical");
        CommandRegistry.NON_MUSIC.registerCommand("eagleoflenny", new TextCommand(AsciiArtConstant.EAGLE_OF_LENNY), "eol", "lennyeagle");

        /* Misc - All commands under this line fall in this category */

        CommandRegistry.NON_MUSIC.registerCommand("mal", new MALCommand());
        CommandRegistry.NON_MUSIC.registerCommand("brainfuck", new BrainfuckCommand());

        CommandRegistry.NON_MUSIC.registerCommand("github", new TextCommand("https://github.com/Frederikam"));
        CommandRegistry.NON_MUSIC.registerCommand("repo", new TextCommand("https://github.com/Frederikam/FredBoat"));

        CommandRegistry.NON_MUSIC.registerCommand("hug", new HugCommand("https://imgur.com/a/jHJOc"));
        CommandRegistry.NON_MUSIC.registerCommand("pat", new PatCommand("https://imgur.com/a/WiPTl"));
        CommandRegistry.NON_MUSIC.registerCommand("facedesk", new FacedeskCommand("https://imgur.com/a/I5Q4U"));
        CommandRegistry.NON_MUSIC.registerCommand("roll", new RollCommand("https://imgur.com/a/lrEwS"));
    }

}
