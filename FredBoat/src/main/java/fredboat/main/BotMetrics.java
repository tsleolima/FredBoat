package fredboat.main;

import fredboat.agent.StatsAgent;
import fredboat.util.JDAUtil;
import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class BotMetrics {

    private static final Logger log = LoggerFactory.getLogger(BotMetrics.class);
    private static BotMetrics.JdaEntityCounts jdaEntityCountsTotal = BotController.INS.getJdaEntityCountsTotal();

    //JDA total entity counts
    public static int getTotalUniqueUsersCount() {
        return BotController.INS.getJdaEntityCountsTotal().uniqueUsersCount;
    }

    public static int getTotalGuildsCount() {
        return jdaEntityCountsTotal.guildsCount;
    }

    public static int getTotalTextChannelsCount() {
        return jdaEntityCountsTotal.textChannelsCount;
    }

    public static int getTotalVoiceChannelsCount() {
        return jdaEntityCountsTotal.voiceChannelsCount;
    }

    public static int getTotalCategoriesCount() {
        return jdaEntityCountsTotal.categoriesCount;
    }

    public static int getTotalEmotesCount() {
        return jdaEntityCountsTotal.emotesCount;
    }

    public static int getTotalRolesCount() {
        return jdaEntityCountsTotal.rolesCount;
    }

    //holds counts of JDA entities
    //this is a central place for stats agents to make calls to
    //stats agents are preferred to triggering counts by JDA events, since we cannot predict JDA events
    //the resulting lower resolution of datapoints is fine, we don't need a high data resolution for these anyways
    protected static class JdaEntityCounts {

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
        protected boolean count(Collection<JDA> shards, boolean... force) {
            for (JDA shard : shards) {
                if ((shard.getStatus() != JDA.Status.CONNECTED) && (force.length < 1 || !force[0])) {
                    log.info("Skipping counts since not all requested shards are ready.");
                    return false;
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

            return true;
        }
    }

    protected static class FredBoatStatsCounter implements StatsAgent.Action {
        private final StatsAgent.Action action;

        FredBoatStatsCounter(StatsAgent.Action action) {
            this.action = action;
        }

        @Override
        public String getName() {
            return "jda entity stats for fredboat";
        }

        @Override
        public void act() throws Exception {
            action.act();
        }
    }
}
