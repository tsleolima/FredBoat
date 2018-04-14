package fredboat.command

import java.time.Duration

class TestCommand : Command() {

    override fun invoke(context: CommandContext) {
        context.apply {
            val str = "Guild: ${guild.id} / ${guild.name}" +
                    "\nName: ${guild.name}" +
                    "\nMembers: ${guild.members.size}" +
                    "\nChannels: ${guild.textChannels.size + guild.voiceChannels.size}" +
                    "\nOwner: ${guild.owner?.effectiveName}" +
                    "\n" +
                    "\nMessage id: ${message.id}" +
                    "\nAuthor: ${message.member.effectiveName}" +
                    "\nMentions: ${message.mentionedMembers}" +
                    "\nChannel: ${channel.name}" +
                    "\nContent: ${message.content}"

            channel.sendTyping()
            channel.send(str)
                    .delaySubscription(Duration.ofSeconds(2))
                    .subscribe({
                        channel.send("Message id of last message was ${it.messageId}").subscribe()
                    })
        }
    }

}