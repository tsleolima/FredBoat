package fredboat.sentinel

import com.fredboat.sentinel.entities.MessageReceivedEvent
import com.fredboat.sentinel.entities.SendMessageResponse
import reactor.core.publisher.Mono
import java.util.regex.Pattern

typealias RawGuild = com.fredboat.sentinel.entities.Guild
typealias RawMember = com.fredboat.sentinel.entities.Member
typealias RawUser = com.fredboat.sentinel.entities.User
typealias RawTextChannel = com.fredboat.sentinel.entities.TextChannel
typealias RawVoiceChannel = com.fredboat.sentinel.entities.VoiceChannel

private val MENTION_PATTERN = Pattern.compile("<@!?([0-9]+)>", Pattern.DOTALL)

class Guild(
        val id: Long
) {
    // TODO: Roles

    val raw: RawGuild
        get() = Sentinel.INSTANCE.getGuild(id)
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
            raw.members.forEach { (_, v) -> list.add(Member(v))}
            return list
        }
    val membersMap: Map<String, Member>
        get() {
            val list = mutableMapOf<String, Member>()
            raw.members.forEach { (k, v) -> list[k] = Member(v) }
            return list
        }
}

class Member(val raw: RawMember) {
    // TODO: Roles property

    val id: Long
        get() = raw.id
    val name: String
        get() = raw.name
    val effectiveName: String
        get() = raw.name //TODO
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

    fun asMention() = "<@$id>"
    fun asUser(): User {
        return User(RawUser(
                id,
                name,
                discrim,
                bot
        ))
    }
}

class User(val raw: RawUser) {
    val id: Long
        get() = raw.id
    val name: String
        get() = raw.name
    val discrim: Short
        get() = raw.discrim
    val bot: Boolean
        get() = raw.bot
}

interface Channel {
    val id: Long
    val name: String
    val ourEffectivePermissions: Long
}

class TextChannel(val raw: RawTextChannel) : Channel {
    override val id: Long
        get() = raw.id
    override val name: String
        get() = raw.name
    override val ourEffectivePermissions: Long
        get() = raw.ourEffectivePermissions

    fun send(str: String): Mono<SendMessageResponse> {
        return Sentinel.INSTANCE.sendMessage(raw, str)
    }

    fun sendTyping() {
        Sentinel.INSTANCE.sendTyping(raw)
    }
}

class VoiceChannel(val raw: RawVoiceChannel) : Channel {
    // TODO: List of members

    override val id: Long
        get() = raw.id
    override val name: String
        get() = raw.name
    override val ourEffectivePermissions: Long
        get() = raw.ourEffectivePermissions
}

class Message(val raw: MessageReceivedEvent) {
    val id: Long
        get() = raw.id
    val content: String
        get() = raw.content
    val member: Member
        get() = Member(raw.author)
    val guild: Guild
        get() = Guild(raw.guildId)
    val channel: TextChannel
        get() = TextChannel(raw.channel)
    val mentionedMembers: List<Member>
        get() {
            // Technically one could mention someone who isn't a member of the guild,
            // but we don't really care for that

            val matcher = MENTION_PATTERN.matcher(content)
            val list =  mutableListOf<Member>()
            val members = guild.membersMap
            while (matcher.find()) {
                members[matcher.group(1)]?.let { list.add(it) }
            }

            return list
        }
}