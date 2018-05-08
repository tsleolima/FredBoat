package fredboat.sentinel

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.*
import fredboat.event.EventLogger
import fredboat.event.SentinelEventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
@RabbitListener(queues = [QueueNames.JDA_EVENTS_QUEUE])
class RabbitConsumer(
        private val sentinel: Sentinel,
        eventLogger: EventLogger
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitConsumer::class.java)
    }
    private val shardStatuses = ConcurrentHashMap<Int, ShardStatus>()
    private val eventHandlers: List<SentinelEventHandler> = listOf(eventLogger) //TODO

    /* Shard lifecycle */

    @RabbitHandler
    fun receive(event: ShardStatusChange) {
        event.shard.apply {
            log.info("Shard [$id / $total] status ${shardStatuses.getOrDefault(id, "<new>")} => $status")
            shardStatuses[id] = status
        }
        eventHandlers.forEach { it.onShardStatusChange(event) }
    }

    @RabbitHandler
    fun receive(event: ShardLifecycleEvent) {
        eventHandlers.forEach { it.onShardLifecycle(event) }
    }

    /* Guild events */

    @RabbitHandler
    fun receive(event: GuildJoinEvent) {
        log.info("Joined guild ${event.guildId}")
        eventHandlers.forEach { it.onGuildJoin(Guild(event.guildId)) }
    }

    @RabbitHandler
    fun receive(event: GuildLeaveEvent) {
        log.info("Left guild ${event.guildId}")
        eventHandlers.forEach { it.onGuildLeave(Guild(event.guildId)) }
    }

    /* Voice events */

    @RabbitHandler
    fun receive(event: VoiceJoinEvent) {
        val channel = VoiceChannel(event.channel)
        val member = Member(event.member)
        eventHandlers.forEach { it.onVoiceJoin(channel, member) }
    }

    @RabbitHandler
    fun receive(event: VoiceLeaveEvent) {
        val channel = VoiceChannel(event.channel)
        val member = Member(event.member)
        eventHandlers.forEach { it.onVoiceLeave(channel, member) }
    }

    @RabbitHandler
    fun receive(event: VoiceMoveEvent) {
        val old = VoiceChannel(event.oldChannel)
        val new = VoiceChannel(event.newChannel)
        val member = Member(event.member)
        eventHandlers.forEach { it.onVoiceMove(old, new, member) }
    }

    /* Message events */

    @RabbitHandler
    fun receive(event: MessageReceivedEvent) {
        val channel = TextChannel(event.channel)
        val author = Member(event.author)
        eventHandlers.forEach { it.onGuildMessage(channel, author, event.content) }
    }

    @RabbitHandler
    fun receive(event: PrivateMessageReceivedEvent) {
        val author = User(event.author)
        eventHandlers.forEach { it.onPrivateMessage(author, event.content) }
    }

    @RabbitListener
    fun guildInvalidate(event: GuildInvalidation) = sentinel.guildCache.invalidate(event.id)

    @RabbitHandler(isDefault = true)
    fun default(msg: Any) = log.warn("Unhandled event $msg")
}