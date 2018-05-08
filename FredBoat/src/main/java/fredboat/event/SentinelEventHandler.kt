package fredboat.event

import com.fredboat.sentinel.entities.ShardStatusChange
import fredboat.sentinel.*

abstract class SentinelEventHandler {

    open fun onShardStatusChange(event: ShardStatusChange) {}

    open fun onGuildJoin(guild: Guild) {}
    open fun onGuildLeave(guild: Guild) {}

    open fun onVoiceJoin(channel: VoiceChannel, member: Member) {}
    open fun onVoiceLeave(channel: VoiceChannel, member: Member) {}
    open fun onVoiceMove(oldChannel: VoiceChannel, newChannel: VoiceChannel, member: Member) {}

    open fun onGuildMessage(channel: TextChannel, author: Member, content: String) {}
    open fun onPrivateMessage(author: User, content: String) {}

}
