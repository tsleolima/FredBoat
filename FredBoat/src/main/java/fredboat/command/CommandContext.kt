package fredboat.command

import fredboat.rabbit.Guild
import fredboat.rabbit.Member
import fredboat.rabbit.Message
import fredboat.rabbit.TextChannel

class CommandContext(
        val guild: Guild,
        val invoker: Member,
        val channel: TextChannel,
        val message: Message
)