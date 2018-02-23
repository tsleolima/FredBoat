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

package fredboat.config;

import fredboat.db.repositories.api.*;
import fredboat.db.repositories.impl.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nullable;

/**
 * Created by napster on 24.02.18.
 * <p>
 * Create our entity repositories
 */
@Configuration
public class RepoConfiguration {

    @Bean
    public BlacklistRepo blacklistRepo(DatabaseWrapper mainDbWrapper) {
        return new SqlSauceBlacklistRepo(mainDbWrapper);
    }

    @Bean
    public GuildConfigRepo guildConfigRepo(DatabaseWrapper mainDbWrapper) {
        return new SqlSauceGuildConfigRepo(mainDbWrapper);
    }

    @Bean
    public GuildDataRepo guildDataRepo(DatabaseWrapper mainDbWrapper) {
        return new SqlSauceGuildDataRepo(mainDbWrapper);
    }

    @Bean
    public GuildModulesRepo guildModulesRepo(DatabaseWrapper mainDbWrapper) {
        return new SqlSauceGuildModulesRepo(mainDbWrapper);
    }

    @Bean
    public GuildPermsRepo guildPermsRepo(DatabaseWrapper mainDbWrapper) {
        return new SqlSauceGuildPermsRepo(mainDbWrapper);
    }

    @Bean
    public PrefixRepo prefixRepo(DatabaseWrapper mainDbWrapper) {
        return new SqlSaucePrefixRepo(mainDbWrapper);
    }

    @Nullable
    @Bean
    public SearchResultRepo searchResultRepo(@Nullable @Qualifier("cacheDbWrapper") DatabaseWrapper cacheDbWrapper) {
        return cacheDbWrapper == null ? null : new SqlSauceSearchResultRepo(cacheDbWrapper); //todo noop repo for cache entities?
    }
}
