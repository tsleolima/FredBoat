/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
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
 *
 */

package fredboat.main;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class FredBoat {

    private static final Logger log = LoggerFactory.getLogger(FredBoat.class);


    // ################################################################################
    // ##                           Global lookups
    // ################################################################################

    @Nullable
    public static TextChannel getTextChannelById(long id) {
        for (FredBoat fb : BotController.INS.getShards()) {
            TextChannel tc = fb.getJda().getTextChannelById(id);
            if (tc != null) return tc;
        }
        return null;
    }

    @Nullable
    public static VoiceChannel getVoiceChannelById(long id) {
        for (FredBoat fb : BotController.INS.getShards()) {
            VoiceChannel vc = fb.getJda().getVoiceChannelById(id);
            if (vc != null) return vc;
        }
        return null;
    }

    @Nullable
    public static Guild getGuildById(long id) {
        for (FredBoat fb : BotController.INS.getShards()) {
            Guild g = fb.getJda().getGuildById(id);
            if (g != null) return g;
        }
        return null;
    }

    @Nullable
    public static User getUserById(long id) {
        for (FredBoat fb : BotController.INS.getShards()) {
            User u = fb.getJda().getUserById(id);
            if (u != null) return u;
        }
        return null;
    }

    @Nonnull
    public static FredBoat getShard(@Nonnull JDA jda) {
        int sId = jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();
        for (FredBoat fb : BotController.INS.getShards()) {
            if (fb.getShardId() == sId) {
                return fb;
            }
        }
        throw new IllegalStateException("Attempted to get instance for JDA shard that is not indexed, shardId: " + sId);
    }

    public static FredBoat getShard(int id) {
        return BotController.INS.getShards().get(id);
    }


    // ################################################################################
    // ##                           Shard definition
    // ################################################################################

    @Nonnull
    public abstract JDA getJda();

    @Nonnull
    public abstract String revive(boolean... force);

    public abstract int getShardId();

    @Nonnull
    public abstract JDA.ShardInfo getShardInfo();

    public abstract void onInit(@Nonnull ReadyEvent readyEvent);


    //JDA entity counts

    public abstract int getUserCount();

    public abstract int getGuildCount();

    public abstract int getTextChannelCount();

    public abstract int getVoiceChannelCount();

    public abstract int getCategoriesCount();

    public abstract int getEmotesCount();

    public abstract int getRolesCount();

}
