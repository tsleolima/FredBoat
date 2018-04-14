package fredboat.rabbit

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.*
import com.google.common.base.Function
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

@Component
@RabbitListener(queues = [QueueNames.JDA_EVENTS_QUEUE])
class Sentinel(private val template: AsyncRabbitTemplate,
               private val blockingTemplate: RabbitTemplate) {

    companion object {
        // This may be static abuse. Consider refactoring
        lateinit var INSTANCE: Sentinel
        private val log: Logger = LoggerFactory.getLogger(Sentinel::class.java)
    }

    init {
        INSTANCE = this
    }

    private val guildCache: LoadingCache<String, RawGuild> = CacheBuilder
            .newBuilder()
            .recordStats()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build<String, RawGuild>(
                    CacheLoader.from(Function {
                        blockingTemplate.convertSendAndReceive(GuildRequest(it!!)) as RawGuild
                    })
            )

    fun getGuilds(shard: Shard): Mono<List<RawGuild>> = Mono.create {
        val req = GuildsRequest(shard.id)
        template.convertSendAndReceive<GuildsResponse?>(req).addCallback(
                { res ->
                    it.success(res?.guilds)
                    // We might not actually want to cache these
                    //res?.guilds?.forEach { guildCache.put(it.id, it) }
                },
                { exc -> it.error(exc) }
        )
    }

    fun getGuild(id: String) = guildCache.get(id)!!

    fun sendMessage(channel: RawTextChannel, message: String): Mono<SendMessageResponse> = Mono.create {
        val req = SendMessageRequest(channel.id, message)
        template.convertSendAndReceive<SendMessageResponse?>(req).addCallback(
                { res -> it.success(res) },
                { exc -> it.error(exc) }
        )
    }

    fun sendTyping(channel: RawTextChannel) {
        val req = SendTypingRequest(channel.id)
        template.convertSendAndReceive<Unit>(req).addCallback(
                {},
                { exc -> log.error("Failed sendTyping in channel {}", exc) }
        )
    }

    fun getApplicationInfo() = blockingTemplate.convertSendAndReceive(ApplicationInfoRequest()) as ApplicationInfo

    /* Events */

    @RabbitListener
    fun guildInvalidate(event: GuildInvalidation) {
        guildCache.invalidate(event.id)
    }

}