package fredboat.rabbit

import java.time.Duration

typealias RawGuild = com.fredboat.sentinel.entities.Guild
typealias RawMember = com.fredboat.sentinel.entities.Member
typealias RawUser = com.fredboat.sentinel.entities.User
typealias RawTextChannel = com.fredboat.sentinel.entities.TextChannel
typealias RawVoiceChannel = com.fredboat.sentinel.entities.VoiceChannel


class Guild(
        val id: String,
        private var _raw: RawGuild? = null
) {
    val raw: RawGuild?
        get() {
            if (_raw != null) return _raw
            return Sentinel.INSTANCE.getGuild(id).block(Duration.ofSeconds(2))
        }
    val name: String?
        get() = raw?.name
}

class Member(val raw: RawMember) {
    // TODO: Guild property
    // TODO: VC property
    // TODO: Roles property

    val id: String
        get() = raw.id
    val idLong: Long
        get() = raw.id.toLong()
    val name: String
        get() = raw.name
    val discrim: Short
        get() = raw.discrim
    val bot: Boolean
        get() = raw.bot
}