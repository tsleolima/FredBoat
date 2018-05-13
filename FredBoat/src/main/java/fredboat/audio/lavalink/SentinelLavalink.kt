package fredboat.audio.lavalink

import fredboat.sentinel.Sentinel
import lavalink.client.io.Lavalink
import net.dv8tion.jda.bot.sharding.ShardManager
import org.springframework.stereotype.Service

@Service
class SentinelLavalink(
        public val sentinel: Sentinel,
        shardManager: ShardManager
) : Lavalink<SentinelLink>(
        sentinel.getApplicationInfo().botId.toString(),
        shardManager.shardsTotal
) {

    companion object {
        lateinit var INSTANCE: SentinelLavalink
    }

    init {
        INSTANCE = this
    }

    override fun buildNewLink(guildId: String) = SentinelLink(this, guildId)
}