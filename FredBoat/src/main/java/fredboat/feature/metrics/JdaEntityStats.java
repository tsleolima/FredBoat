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

package fredboat.feature.metrics;

import fredboat.util.JDAUtil;
import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Created by napster on 18.04.18.
 * <p>
 * Holds counts of JDA entities
 * Regular calls by stats agents are preferred over triggering counts by JDA events, since we cannot predict how often
 * JDA events are going to happen.
 * The resulting lower resolution of datapoints is fine, we don't need a high data resolution for these metrics.
 */
public class JdaEntityStats {

    private static final Logger log = LoggerFactory.getLogger(JdaEntityStats.class);

    private boolean counted = false;
    protected int uniqueUsersCount;
    protected int guildsCount;
    protected int textChannelsCount;
    protected int voiceChannelsCount;
    protected int categoriesCount;
    protected int emotesCount;
    protected int rolesCount;

    private final AtomicInteger expectedUniqueUserCount = new AtomicInteger(-1);

    //counts things
    // also checks shards for readiness and only counts if all of them are ready
    // the force is an option for when we want to do a count when receiving the onReady event, but JDAs status is
    // not CONNECTED at that point
    void count(Supplier<Collection<JDA>> shardSupplier, boolean... force) {
        Collection<JDA> shards = shardSupplier.get();
        for (JDA shard : shards) {
            if ((shard.getStatus() != JDA.Status.CONNECTED) && (force.length < 1 || !force[0])) {
                log.info("Skipping counts since not all requested shards are ready.");
                return;
            }
        }

        this.uniqueUsersCount = JDAUtil.countUniqueUsers(shards, expectedUniqueUserCount);
        //never shrink the expected user count (might happen due to unready/reloading shards)
        this.expectedUniqueUserCount.accumulateAndGet(uniqueUsersCount, Math::max);

        this.guildsCount = JDAUtil.countGuilds(shards);
        this.textChannelsCount = JDAUtil.countTextChannels(shards);
        this.voiceChannelsCount = JDAUtil.countVoiceChannels(shards);
        this.categoriesCount = JDAUtil.countCategories(shards);
        this.emotesCount = JDAUtil.countEmotes(shards);
        this.rolesCount = JDAUtil.countRoles(shards);

        counted = true;
    }

    public boolean isCounted() {
        return counted;
    }

    public int getUniqueUsersCount() {
        return uniqueUsersCount;
    }

    public int getGuildsCount() {
        return guildsCount;
    }

    public int getTextChannelsCount() {
        return textChannelsCount;
    }

    public int getVoiceChannelsCount() {
        return voiceChannelsCount;
    }

    public int getCategoriesCount() {
        return categoriesCount;
    }

    public int getEmotesCount() {
        return emotesCount;
    }

    public int getRolesCount() {
        return rolesCount;
    }
}
