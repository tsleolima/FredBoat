package fredboat.sentinel

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
import reactor.core.publisher.Flux
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

    val guildCache: LoadingCache<Long, RawGuild> = CacheBuilder
            .newBuilder()
            .recordStats()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build<Long, RawGuild>(
                    CacheLoader.from(Function {
                        val result = blockingTemplate.convertSendAndReceive(QueueNames.SENTINEL_REQUESTS_QUEUE, GuildRequest(it!!))

                        if (result == null) {
                            log.warn("Requested guild $it but got null in response!")
                        }

                        return@Function result as? RawGuild
                    })
            )

    fun getGuilds(shard: Shard): Flux<RawGuild> = Flux.create {
        val req = GuildsRequest(shard.id)
        template.convertSendAndReceive<GuildsResponse?>(req).addCallback(
                { res ->
                    if (res != null) {
                        res.guilds.forEach { g -> it.next(g) }
                        it.complete()
                    } else {
                        it.error(RuntimeException("Response was null"))
                    }
                },
                { exc -> it.error(exc) }
        )
    }

    fun getGuild(id: Long) = guildCache.get(id)!!

    fun sendMessage(channel: RawTextChannel, message: IMessage): Mono<SendMessageResponse> = Mono.create {
        val req = SendMessageRequest(channel.id, message)
        template.convertSendAndReceive<SendMessageResponse?>(QueueNames.SENTINEL_REQUESTS_QUEUE, req).addCallback(
                { res -> it.success(res) },
                { exc -> it.error(exc) }
        )
    }

    // TODO: Figure out how to route this. We don't know what Sentinel to contact!
    fun sendPrivateMessage(user: User, message: IMessage): Mono<Unit> = Mono.create {
        val req = SendPrivateMessageRequest(user.id, message)
        template.convertSendAndReceive<Unit>(QueueNames.SENTINEL_REQUESTS_QUEUE, req).addCallback(
                { _ -> it.success() },
                { exc -> it.error(exc) }
        )
    }

    fun editMessage(channel: TextChannel, messageId: Long, message: IMessage): Mono<Unit> = Mono.create {
        val req = EditMessageRequest(channel.id, messageId, message)
        template.convertSendAndReceive<Unit>(QueueNames.SENTINEL_REQUESTS_QUEUE, req).addCallback(
                { _ -> it.success() },
                { exc -> it.error(exc) }
        )
    }

    fun sendTyping(channel: RawTextChannel) {
        val req = SendTypingRequest(channel.id)
        template.convertSendAndReceive<Unit>(QueueNames.SENTINEL_REQUESTS_QUEUE, req).addCallback(
                {},
                { exc -> log.error("Failed sendTyping in channel {}", channel, exc) }
        )
    }

    private var cachedApplicationInfo: ApplicationInfo? = null
    fun getApplicationInfo(): ApplicationInfo {
        if (cachedApplicationInfo != null) return cachedApplicationInfo as ApplicationInfo

        cachedApplicationInfo = blockingTemplate.convertSendAndReceive(ApplicationInfoRequest()) as ApplicationInfo
        return cachedApplicationInfo!!
    }

    /* Permissions */

    private fun checkPermissions(guild: Guild, member: Member?, role: Role?, permissions: IPermissionSet):
            Mono<PermissionCheckResponse> = Mono.create {

        val req = GuildPermissionRequest(guild.id, member?.id, role?.id, permissions.raw)
        template.convertSendAndReceive<PermissionCheckResponse>(QueueNames.SENTINEL_REQUESTS_QUEUE, req).addCallback(
                { r -> it.success(r) },
                { exc -> it.error(RuntimeException("Failed checking permissions in $guild", exc)) }
        )
    }

    // Role and member are mutually exclusive
    fun checkPermissions(guild: Guild, member: Member, permissions: IPermissionSet)
            = checkPermissions(guild, member, null, permissions)
    fun checkPermissions(guild: Guild, role: Role, permissions: IPermissionSet)
            = checkPermissions(guild, null, role, permissions)

    private fun checkPermissions(channel: Channel, member: Member?, role: Role?, permissions: IPermissionSet):
            Mono<PermissionCheckResponse> = Mono.create {

        val req = ChannelPermissionRequest(channel.id, member?.id, role?.id, permissions.raw)
        template.convertSendAndReceive<PermissionCheckResponse>(QueueNames.SENTINEL_REQUESTS_QUEUE, req).addCallback(
                { r -> it.success(r) },
                { exc -> it.error(RuntimeException("Failed checking permissions in $channel", exc)) }
        )
    }

    // Role and member are mutually exclusive
    fun checkPermissions(channel: Channel, member: Member, permissions: IPermissionSet)
            = checkPermissions(channel, member, null, permissions)
    fun checkPermissions(channel: Channel, role: Role, permissions: IPermissionSet)
            = checkPermissions(channel, null, role, permissions)

}