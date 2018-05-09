package fredboat.event

import fredboat.audio.player.PlayerRegistry
import fredboat.command.info.HelloCommand
import fredboat.db.api.GuildDataService
import fredboat.db.transfer.GuildData
import fredboat.feature.metrics.Metrics
import fredboat.sentinel.Guild
import net.dv8tion.jda.core.entities.TextChannel
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.OffsetDateTime

@Component
class GuildEventHandler(
        private val guildDataService: GuildDataService,
        private val playerRegistry: PlayerRegistry
) : SentinelEventHandler() {
    override fun onGuildJoin(guild: Guild) {
        // Wait a few seconds to allow permissions to be set and applied and propagated
        val mono = Mono.create<Unit> {
            sendHelloOnJoin(guild)
        }
        mono.delaySubscription(Duration.ofSeconds(10))
        mono.subscribe()
    }

    override fun onGuildLeave(guild: Guild) {
        playerRegistry.destroyPlayer(guild)

        val lifespan = OffsetDateTime.now().toEpochSecond() - guild.selfMember.getJoinDate().toEpochSecond()
        Metrics.guildLifespan.observe(lifespan.toDouble())
    }

    private fun sendHelloOnJoin(guild: Guild) {
        //filter guilds that already received a hello message
        // useful for when discord trolls us with fake guild joins
        // or to prevent it send repeatedly due to kick and reinvite
        val gd = guildDataService.fetchGuildData(guild)
        if (gd.timestampHelloSent > 0) {
            return
        }

        var channel: TextChannel? = guild.getTextChannelById(guild.id) //old public channel
        if (channel == null || !channel.canTalk()) {
            //find first channel that we can talk in
            for (tc in guild.textChannels) {
                if (tc.canTalk()) {
                    channel = tc
                    break
                }
            }
        }
        if (channel == null) {
            //no channel found to talk in
            return
        }

        //send actual hello message and persist on success
        CentralMessaging.message(channel, HelloCommand.getHello(guild))
                .success({ __ -> guildDataService.transformGuildData(guild, Function<GuildData, GuildData> { it.helloSent() }) })
                .send(null)
    }
}