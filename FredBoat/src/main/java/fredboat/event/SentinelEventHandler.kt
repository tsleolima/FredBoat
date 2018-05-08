package fredboat.event

import com.fredboat.sentinel.entities.*

abstract class SentinelEventHandler {

    open fun onShardStatusChange(event: ShardStatusChange) {}

    open fun onGuildJoin(guild: Guild) {}
    open fun onGuildLeave(guild: Guild) {}

    open fun onVoiceJoin(channel: VoiceChannel, member: Member) {}
    open fun onVoiceLeave(channel: VoiceChannel, member: Member) {}
    open fun onVoiceMove(old: VoiceChannel, new: VoiceChannel, member: Member) {}

    open fun onGuildMessage(channel: TextChannel, member: Member, content: String) {}
    open fun onPrivateMEssage(user: User, content: String) {}

}
