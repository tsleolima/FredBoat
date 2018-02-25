/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.main;

import fredboat.agent.StatsAgent;
import fredboat.config.property.Credentials;
import fredboat.feature.metrics.ShardStatsCounterProvider;
import fredboat.util.DiscordUtil;
import fredboat.util.func.NonnullSupplier;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by napster on 24.02.18.
 * <p>
 * This component's main goal is to provide access to the JDA of a shard and other, shard specific, objects
 */
@Component
public class ShardContext {

    private final ConcurrentHashMap<Integer, JdaProxy> instances = new ConcurrentHashMap<>();
    private final Credentials credentials;
    private final ShardManager shardManager;
    private final StatsAgent statsAgent;
    private final ShardStatsCounterProvider shardStatsCounterProvider;

    public ShardContext(Credentials credentials, @Lazy ShardManager shardManager, StatsAgent statsAgent,
                        ShardStatsCounterProvider shardStatsCounterProvider) {
        this.credentials = credentials;
        this.shardManager = shardManager;
        this.statsAgent = statsAgent;
        this.shardStatsCounterProvider = shardStatsCounterProvider;
    }

    @Nonnull //todo resolve circular dependency in a less ugly way than lazy loading the shard manager?
    private JDA getJda(int shardId) {
        return shardManager.getShardById(shardId);
    }

    @Nonnull
    public JdaProxy of(int id) {
        if ((shardManager.getShardById(id) == null) || id < 0 || id >= credentials.getRecommendedShardCount()) {
            throw new IllegalArgumentException("Shard " + id + " is outside of our shard range");
        }

        return instances.computeIfAbsent(id, integer -> {
            statsAgent.addAction(shardStatsCounterProvider.get(getJda(id).getShardInfo(), () -> this.getJda(id)));

            return new JdaProxy(() -> this.getJda(id));
        });
    }

    @Nonnull
    public JdaProxy of(JDA jda) {
        return of(jda.getShardInfo().getShardId());
    }

    @Nonnull
    public JdaProxy ofGuildId(long guildId) {
        return of(DiscordUtil.getShardId(guildId, credentials));
    }

    /**
     * A reference to a shard. Directly referencing a JDA instance is problematic, as it may shut down and be replaced.
     * Furthermore, it causes a memory leak.
     */
    public static class JdaProxy {
        private final NonnullSupplier<JDA> jdaProvider;

        //should only be created through the ShardContext
        private JdaProxy(NonnullSupplier<JDA> jdaProvider) {
            this.jdaProvider = jdaProvider;
        }

        @Nonnull
        public JDA getJda() {
            return jdaProvider.get();
        }
    }
}
