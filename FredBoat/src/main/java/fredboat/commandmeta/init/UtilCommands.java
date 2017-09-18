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

import fredboat.command.fun.*;
import fredboat.command.maintenance.FuzzyUserSearchCommand;
import fredboat.command.maintenance.PingCommand;
import fredboat.command.util.MALCommand;
import fredboat.command.util.ServerInfoCommand;
import fredboat.command.util.UserInfoCommand;
import fredboat.commandmeta.CommandGroup;
import fredboat.commandmeta.CommandRegistry;

public class UtilCommands {

    public static void initCommands() {
        CommandRegistry.setMode(CommandGroup.UTIL);

        /* Util */
        CommandRegistry.registerCommand("serverinfo", new ServerInfoCommand(), "guildinfo");
        CommandRegistry.registerCommand("userinfo", new UserInfoCommand(), "memberinfo");
        CommandRegistry.registerCommand("ping", new PingCommand());
        CommandRegistry.registerCommand("fuzzy", new FuzzyUserSearchCommand());CommandRegistry.registerCommand("mal", new MALCommand());
        //CommandRegistry.registerCommand("brainfuck", new BrainfuckCommand()); // I don't think we want this on the music bot due to potential abuse ~~Fre_D
        CommandRegistry.registerCommand("github", new TextCommand("https://github.com/Frederikam"));
        CommandRegistry.registerCommand("repo", new TextCommand("https://github.com/Frederikam/FredBoat"));
    }

}
