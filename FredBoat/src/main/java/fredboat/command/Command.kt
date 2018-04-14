package fredboat.command

import fredboat.command.CommandContext

abstract class Command {
    abstract fun invoke(context: CommandContext)
}