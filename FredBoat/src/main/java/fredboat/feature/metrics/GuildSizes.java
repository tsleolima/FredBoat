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

import net.dv8tion.jda.core.entities.Guild;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by napster on 18.04.18.
 */
public class GuildSizes {

    protected int bucket1to10;
    protected int bucket10to100;
    protected int bucket100to1k;
    protected int bucket1kto10k;
    protected int bucket10kto100k;
    protected int bucket100kAndUp;


    protected void count(Supplier<Stream<Guild>> guilds) {
        AtomicInteger b1to10 = new AtomicInteger(0);
        AtomicInteger b10to100 = new AtomicInteger(0);
        AtomicInteger b100to1k = new AtomicInteger(0);
        AtomicInteger b1kto10k = new AtomicInteger(0);
        AtomicInteger b10kto100k = new AtomicInteger(0);
        AtomicInteger b100kAndUp = new AtomicInteger(0);

        guilds.get().forEach(guild -> {
            long size = guild.getMemberCache().size();
            if (size <= 10) {
                b1to10.incrementAndGet();
            } else if (size <= 100) {
                b10to100.incrementAndGet();
            } else if (size <= 1000) {
                b100to1k.incrementAndGet();
            } else if (size <= 10000) {
                b1kto10k.incrementAndGet();
            } else if (size <= 100000) {
                b10kto100k.incrementAndGet();
            } else {
                b100kAndUp.incrementAndGet();
            }
        });

        bucket1to10 = b1to10.get();
        bucket10to100 = b10to100.get();
        bucket100to1k = b100to1k.get();
        bucket1kto10k = b1kto10k.get();
        bucket10kto100k = b10kto100k.get();
        bucket100kAndUp = b100kAndUp.get();
    }

    public int getBucket1to10() {
        return bucket1to10;
    }

    public int getBucket10to100() {
        return bucket10to100;
    }

    public int getBucket100to1k() {
        return bucket100to1k;
    }

    public int getBucket1kto10k() {
        return bucket1kto10k;
    }

    public int getBucket10kto100k() {
        return bucket10kto100k;
    }

    public int getBucket100kAndUp() {
        return bucket100kAndUp;
    }
}
