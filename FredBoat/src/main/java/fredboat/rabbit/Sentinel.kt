package fredboat.rabbit

import com.fredboat.sentinel.entities.*
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.stereotype.Component
import org.springframework.util.concurrent.SuccessCallback
import reactor.core.publisher.Mono

@Component
class Sentinel(private val template: AsyncRabbitTemplate){

    fun getGuilds(shard: Shard): Mono<List<Guild>> = Mono.create {
        val req = GuildsRequest(shard.id)
        template.convertSendAndReceive<GuildsResponse?>(req).addCallback(
                {res -> it.success(res?.guilds)},
                {exc -> it.error(exc)}
        )
    }

    fun getGuild(id: String): Mono<Guild?> = Mono.create {
        val req = GuildRequest(id)
        template.convertSendAndReceive<Guild?>(req).addCallback(
                {res -> it.success(res)},
                {exc -> it.error(exc)}
        )
    }

    fun sendMessage(channel: TextChannel, message: String): Mono<SendMessageResponse> = Mono.create {
        val req = SendMessageRequest(channel.id, message)
        template.convertSendAndReceive<SendMessageResponse?>(req).addCallback(
                {res -> it.success(res)},
                {exc -> it.error(exc)}
        )
    }

    fun sendTyping(channel: TextChannel): Mono<Unit> = Mono.create {
        val req = SendTypingRequest(channel.id)
        template.convertSendAndReceive<Unit>(req).addCallback(
                {_   -> it.success()},
                {exc -> it.error(exc)}
        )
    }

}