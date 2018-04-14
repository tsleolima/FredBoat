package fredboat.event

import fredboat.command.CommandContext

abstract class Command {
    abstract fun invoke(event: CommandContext)
}