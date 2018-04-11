package fredboat.rabbit

import com.fredboat.sentinel.entities.*
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class Sentinel(private val template: AsyncRabbitTemplate){

    companion object {
        // This may be static abuse. Consider refactoring
        lateinit var INSTANCE: Sentinel
    }

    init {
        INSTANCE = this
    }

    fun getGuilds(shard: Shard): Mono<List<RawGuild>> = Mono.create {
        val req = GuildsRequest(shard.id)
        template.convertSendAndReceive<GuildsResponse?>(req).addCallback(
                {res -> it.success(res?.guilds)},
                {exc -> it.error(exc)}
        )
    }

    fun getGuild(id: String): Mono<RawGuild?> = Mono.create {
        val req = GuildRequest(id)
        template.convertSendAndReceive<RawGuild?>(req).addCallback(
                {res -> it.success(res)},
                {exc -> it.error(exc)}
        )
    }

    fun sendMessage(channel: RawTextChannel, message: String): Mono<SendMessageResponse> = Mono.create {
        val req = SendMessageRequest(channel.id, message)
        template.convertSendAndReceive<SendMessageResponse?>(req).addCallback(
                {res -> it.success(res)},
                {exc -> it.error(exc)}
        )
    }

    fun sendTyping(channel: RawTextChannel): Mono<Unit> = Mono.create {
        val req = SendTypingRequest(channel.id)
        template.convertSendAndReceive<Unit>(req).addCallback(
                {_   -> it.success()},
                {exc -> it.error(exc)}
        )
    }

}