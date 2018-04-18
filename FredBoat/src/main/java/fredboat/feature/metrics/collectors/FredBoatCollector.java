/*
 *
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
 */

package fredboat.feature.metrics.collectors;

import fredboat.feature.metrics.BotMetrics;
import fredboat.feature.metrics.DockerStats;
import fredboat.feature.metrics.JdaEntityStats;
import fredboat.feature.metrics.MusicPlayerStats;
import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by napster on 19.10.17.
 * <p>
 * Collects various FredBoat stats for prometheus
 */
@Component
public class FredBoatCollector extends Collector {

    private final BotMetrics botMetrics;

    public FredBoatCollector(BotMetrics botMetrics) {
        super();
        this.botMetrics = botMetrics;
    }

    @Override
    public List<MetricFamilySamples> collect() {

        List<MetricFamilySamples> mfs = new ArrayList<>();
        //NOTE: shard specific metrics have been disabled, because we were not really using them, but they take up a ton
        // of samples (Example: 800 shards x 7 metrics = 5600 unused samples). the label has been kept to not break
        // existing graphs. Use "total" for it when adding botwide metrics.
        List<String> labelNames = Arrays.asList("shard", "entity");

        GaugeMetricFamily jdaEntities = new GaugeMetricFamily("fredboat_jda_entities",
                "Amount of JDA entities", labelNames);
        mfs.add(jdaEntities);

        GaugeMetricFamily playersPlaying = new GaugeMetricFamily("fredboat_playing_music_players",
                "Currently playing music players", labelNames);
        mfs.add(playersPlaying);

        CounterMetricFamily dockerPulls = new CounterMetricFamily("fredboat_docker_pulls",
                "Total fredboat docker image pulls as reported by the docker hub.", labelNames);
        mfs.add(dockerPulls);

        //global jda entity stats
        JdaEntityStats jdaEntityStatsTotal = botMetrics.getJdaEntityStatsTotal();
        if (jdaEntityStatsTotal.isCounted()) {
            jdaEntities.addMetric(Arrays.asList("total", "User"), jdaEntityStatsTotal.getUniqueUsersCount());
            jdaEntities.addMetric(Arrays.asList("total", "Guild"), jdaEntityStatsTotal.getGuildsCount());
            jdaEntities.addMetric(Arrays.asList("total", "TextChannel"), jdaEntityStatsTotal.getTextChannelsCount());
            jdaEntities.addMetric(Arrays.asList("total", "VoiceChannel"), jdaEntityStatsTotal.getVoiceChannelsCount());
            jdaEntities.addMetric(Arrays.asList("total", "Category"), jdaEntityStatsTotal.getCategoriesCount());
            jdaEntities.addMetric(Arrays.asList("total", "Emote"), jdaEntityStatsTotal.getEmotesCount());
            jdaEntities.addMetric(Arrays.asList("total", "Role"), jdaEntityStatsTotal.getRolesCount());
        }

        //music player stats
        MusicPlayerStats musicPlayerStats = botMetrics.getMusicPlayerStats();
        if (musicPlayerStats.isCounted()) {
            playersPlaying.addMetric(Arrays.asList("total", "Players"), musicPlayerStats.getPlaying()); //entity could be better named "PlayingPlayers", but dont break existing graphs...besides, players will hopefully one day be stateless entities in the database instead of paused objects in the JVM.
            playersPlaying.addMetric(Arrays.asList("total", "TotalPlayers"), musicPlayerStats.getTotal());
        }

        //docker stats
        DockerStats dockerStats = botMetrics.getDockerStats();
        if (dockerStats.isFetched()) {
            dockerPulls.addMetric(Arrays.asList("total", "Bot"), dockerStats.getDockerPullsBot());
            dockerPulls.addMetric(Arrays.asList("total", "Db"), dockerStats.getDockerPullsDb());
        }

        return mfs;
    }
}
