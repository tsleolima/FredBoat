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

package fredboat.db.rest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fredboat.db.transfer.TransferObject;
import fredboat.util.rest.CacheUtil;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 18.02.18.
 */
public abstract class CachedRestService<I extends Serializable, E extends TransferObject<I>> extends RestService<I, E> {

    protected final LoadingCache<I, E> cache;

    /**
     * Create the CachedRestRepo using a default cache
     */
    public CachedRestService(String path, Class<E> entityClass, RestTemplate backendRestTemplate) {
        this(path, entityClass, backendRestTemplate,
                CacheBuilder.newBuilder()
                        .expireAfterAccess(60, TimeUnit.SECONDS)
                        .expireAfterWrite(120, TimeUnit.SECONDS)
                        .recordStats()
        );
    }

    public CachedRestService(String path, Class<E> entityClass, RestTemplate backendRestTemplate, CacheBuilder<Object, Object> cacheBuilder) {
        super(path, entityClass, backendRestTemplate);
        this.cache = cacheBuilder.build(CacheLoader.from(super::fetch));
    }

    /**
     * Add the cache of this CachedRestRepo to the provided metrics under the provided name
     *
     * @return itself for chaining calls
     */
    public CachedRestService<I, E> registerCacheStats(CacheMetricsCollector cacheMetrics, String name) {
        cacheMetrics.addCache(name, cache);
        return this;
    }

    @Override
    protected void delete(I id) {
        try {
            super.delete(id);
        } finally {
            cache.invalidate(id);
        }
    }

    @Override
    public E fetch(I id) {
        return CacheUtil.getUncheckedUnwrapped(cache, id);
    }

    @Override
    public E merge(E entity) {
        E merged = super.merge(entity);
        cache.put(merged.getId(), merged);
        return merged;
    }
}
