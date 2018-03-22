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

package fredboat.feature.metrics.collectors;

import fredboat.feature.metrics.BotMetrics;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by napster on 22.03.18.
 */
@Component
public class GuildSizesCollector extends Collector {

    private final BotMetrics botMetrics;

    public GuildSizesCollector(BotMetrics botMetrics) {
        super();
        this.botMetrics = botMetrics;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();
        List<String> labelNames = Collections.singletonList("bucket");

        GaugeMetricFamily guildSizes = new GaugeMetricFamily("fredboat_guild_sizes",
                "Guilds by size", labelNames);
        mfs.add(guildSizes);

        guildSizes.addMetric(Collections.singletonList("1-10"), botMetrics.getGuildSizes().getBucket1to10());
        guildSizes.addMetric(Collections.singletonList("10-100"), botMetrics.getGuildSizes().getBucket10to100());
        guildSizes.addMetric(Collections.singletonList("100-1k"), botMetrics.getGuildSizes().getBucket100to1k());
        guildSizes.addMetric(Collections.singletonList("1k-10k"), botMetrics.getGuildSizes().getBucket1kto10k());
        guildSizes.addMetric(Collections.singletonList("10k-100k"), botMetrics.getGuildSizes().getBucket10kto100k());
        guildSizes.addMetric(Collections.singletonList("100k+"), botMetrics.getGuildSizes().getBucket100kAndUp());

        return mfs;
    }
}
