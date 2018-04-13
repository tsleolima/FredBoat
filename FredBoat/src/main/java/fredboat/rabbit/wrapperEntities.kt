package fredboat.rabbit

import com.fredboat.sentinel.entities.MessageReceivedEvent

typealias RawGuild = com.fredboat.sentinel.entities.Guild
typealias RawMember = com.fredboat.sentinel.entities.Member
typealias RawUser = com.fredboat.sentinel.entities.User
typealias RawTextChannel = com.fredboat.sentinel.entities.TextChannel
typealias RawVoiceChannel = com.fredboat.sentinel.entities.VoiceChannel


class Guild(
        val id: String
) {
    // TODO: Roles

    val raw: RawGuild
        get() = Sentinel.INSTANCE.getGuild(id)
    val idLong: Long
        get() = id.toLong()
    val name: String
        get() = raw.name
    val owner: Member?
        get() {
            if (raw.owner != null) return Member(raw.owner!!)
            return null
        }
    val textChannels: List<TextChannel>
        get() {
            val list = mutableListOf<TextChannel>()
            raw.textChannels.forEach {list.add(TextChannel(it))}
            return list
        }
    val voiceChannels: List<VoiceChannel>
        get() {
            val list = mutableListOf<VoiceChannel>()
            raw.voiceChannels.forEach {list.add(VoiceChannel(it))}
            return list
        }
    val members: List<Member>
        get() {
            val list = mutableListOf<Member>()
            raw.members.forEach {list.add(Member(it))}
            return list
        }
}

class Member(val raw: RawMember) {
    // TODO: Roles property

    val id: String
        get() = raw.id
    val idLong: Long
        get() = raw.id.toLong()
    val name: String
        get() = raw.name
    val discrim: Short
        get() = raw.discrim
    val guild: RawGuild
        get() = Sentinel.INSTANCE.getGuild(raw.guildId)
    val bot: Boolean
        get() = raw.bot
    val voiceChannel: VoiceChannel?
        get() {
            if (raw.voiceChannel != null) return VoiceChannel(raw.voiceChannel!!)
            return null
        }
}

class User(val raw: RawUser) {
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

class TextChannel(val raw: RawTextChannel) {
    val id: String
        get() = raw.id
    val idLong: Long
        get() = raw.id.toLong()
    val name: String
        get() = raw.name
    val ourEffectivePermissions: Long
        get() = raw.ourEffectivePermissions
}

class VoiceChannel(val raw: RawVoiceChannel) {
    // TODO: List of members

    val id: String
        get() = raw.id
    val idLong: Long
        get() = raw.id.toLong()
    val name: String
        get() = raw.name
    val ourEffectivePermissions: Long
        get() = raw.ourEffectivePermissions
}

class Message(val raw: MessageReceivedEvent) {
    val id: String
        get() = raw.id
    val content: String
        get() = raw.id
    val member: Member
        get() = Member(raw.author)
    val guild: RawGuild
        get() = Sentinel.INSTANCE.getGuild(raw.guildId)
    val channel: TextChannel
        get() = TextChannel(raw.channel)
}