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

package fredboat.jda;

import com.google.common.base.Suppliers;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by napster on 25.02.18.
 * <p>
 * The Lazy init of the shard manager allows us to resolve circular dependency of our components on the shard manager
 */
@Component
public class JdaEntityProvider implements EmoteProvider, GuildProvider, MemberProvider, RoleProvider, ShardProvider,
        TextChannelProvider, UserProvider, VoiceChannelProvider {

    private static final Logger log = LoggerFactory.getLogger(JdaEntityProvider.class);

    private final Supplier<ShardManager> shardManager;

    public JdaEntityProvider(@Lazy ShardManager shardManagerProxy) {

        //unwrap the spring proxy of the shard manager
        // we require the raw shardManager, because the proxy will error out when accessed during shutdown hooks, but
        // we manage the lifecycle of the shardManager singleton ourselves, so we don't need spring refusing us to serve
        // a perfectly fine bean during shutdown hooks.
        this.shardManager = Suppliers.memoize(() -> {
            try {
                ShardManager target = (ShardManager) ((Advised) shardManagerProxy).getTargetSource().getTarget();
                if (target == null) {
                    throw new IllegalStateException();
                }
                return target;
            } catch (Exception e) {
                log.error("Failed to unproxy the shard manager", e);
                //this should not happen but if it does, just work with the proxy. however we might error out during
                // execution of shutdown handlers that rely on fetching jdaentities
                return shardManagerProxy;
            }
        });
    }

    @Nullable
    @Override
    public Emote getEmoteById(long emoteId) {
        return shardManager.get().getEmoteById(emoteId);
    }

    @Override
    public Stream<Emote> streamEmotes() {
        return shardManager.get().getEmoteCache().stream();
    }

    @Override
    @Nullable
    public Guild getGuildById(long guildId) {
        return shardManager.get().getGuildById(guildId);
    }

    @Override
    public Stream<Guild> streamGuilds() {
        return shardManager.get().getGuildCache().stream();
    }

    @Override
    @Nullable
    public Member getMemberById(long guildId, long userId) {
        Guild guild = getGuildById(guildId);
        return guild == null ? null : guild.getMemberById(userId);
    }

    @Override
    @Nullable
    public Role getRoleById(long roleId) {
        return shardManager.get().getRoleById(roleId);
    }

    @Override
    public Stream<Role> streamRoles() {
        return shardManager.get().getRoleCache().stream();
    }

    @Override
    public JDA getShardById(int shardId) {
        return shardManager.get().getShardById(shardId);
    }

    @Override
    public Stream<JDA> streamShards() {
        return shardManager.get().getShardCache().stream();
    }

    @Override
    @Nullable
    public TextChannel getTextChannelById(long textChannelId) {
        return shardManager.get().getTextChannelById(textChannelId);
    }

    @Override
    public Stream<TextChannel> streamTextChannels() {
        return shardManager.get().getTextChannelCache().stream();
    }

    @Override
    @Nullable
    public User getUserById(long userId) {
        return shardManager.get().getUserById(userId);
    }

    @Override
    public Stream<User> streamUsers() {
        return shardManager.get().getUserCache().stream();
    }

    @Override
    @Nullable
    public VoiceChannel getVoiceChannelById(long voiceChannelId) {
        return shardManager.get().getVoiceChannelById(voiceChannelId);
    }

    @Override
    public Stream<VoiceChannel> streamVoiceChannels() {
        return shardManager.get().getVoiceChannelCache().stream();
    }
}
