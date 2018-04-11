package fredboat.rabbit

typealias RawGuild = com.fredboat.sentinel.entities.Guild
typealias RawMember = com.fredboat.sentinel.entities.Member
typealias RawUser = com.fredboat.sentinel.entities.User
typealias RawTextChannel = com.fredboat.sentinel.entities.TextChannel
typealias RawVoiceChannel = com.fredboat.sentinel.entities.VoiceChannel


class Guild(
        val id: String
) {
    val raw: RawGuild
        get() = Sentinel.INSTANCE.getGuild(id)
    val name: String?
        get() = raw.name
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