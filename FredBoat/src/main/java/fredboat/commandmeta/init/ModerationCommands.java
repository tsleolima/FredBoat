package fredboat.commandmeta.init;

import fredboat.command.moderation.ClearCommand;
import fredboat.command.moderation.HardbanCommand;
import fredboat.command.moderation.KickCommand;
import fredboat.command.moderation.SoftbanCommand;
import fredboat.commandmeta.CommandGroup;
import fredboat.commandmeta.CommandRegistry;

public class ModerationCommands {

    public static void initCommands() {
        CommandRegistry.setMode(CommandGroup.MODERATION);

        /* Moderation */
        CommandRegistry.registerCommand("hardban", new HardbanCommand());
        CommandRegistry.registerCommand("kick", new KickCommand());
        CommandRegistry.registerCommand("softban", new SoftbanCommand());
        CommandRegistry.registerCommand("clear", new ClearCommand());
    }

}
