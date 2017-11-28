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

package fredboat.db;

import com.zaxxer.hikari.HikariConfig;
import fredboat.Config;
import fredboat.feature.metrics.Metrics;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.ssh.SshTunnel;

import java.util.Properties;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String DEFAULT_PERSISTENCE_UNIT_NAME = "fredboat.default";

    //local port, if using SSH tunnel point your jdbc to this, e.g. jdbc:postgresql://localhost:9333/...
    private static final int SSH_TUNNEL_PORT = 9333;


    public static DatabaseConnection postgres() throws DatabaseException {
        String jdbc = Config.CONFIG.getJdbcUrl();
        if (jdbc == null || jdbc.isEmpty()) {
            log.info("No JDBC URL found, assuming this is a docker environment. Trying default docker JDBC url");
            jdbc = "jdbc:postgresql://db:5432/fredboat?user=fredboat";
        }

        HikariConfig hikariConfig = DatabaseConnection.Builder.getDefaultHikariConfig();
        hikariConfig.setMaximumPoolSize(Config.CONFIG.getHikariPoolSize());

        Properties hibernateProps = DatabaseConnection.Builder.getDefaultHibernateProps();
        hibernateProps.put("configLocation", "hibernate.cfg.xml");
        hibernateProps.put("hibernate.cache.use_second_level_cache", "true");
        hibernateProps.put("hibernate.cache.provider_configuration_file_resource_path", "ehcache.xml");
        hibernateProps.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");

        DatabaseConnection databaseConnection = new DatabaseConnection.Builder(DEFAULT_PERSISTENCE_UNIT_NAME, jdbc)
                .setHikariConfig(hikariConfig)
                .setHibernateProps(hibernateProps)
                .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                .addEntityPackage("fredboat.db.entity")
                .setAppName("FredBoat_" + Config.CONFIG.getDistribution())
                .setSshDetails(!Config.CONFIG.isUseSshTunnel() ? null :
                        new SshTunnel.SshDetails(Config.CONFIG.getSshHost(), Config.CONFIG.getSshUser())
                                .setLocalPort(SSH_TUNNEL_PORT)
                                .setRemotePort(Config.CONFIG.getForwardToPort())
                                .setKeyFile(Config.CONFIG.getSshPrivateKeyFile())
                )
                .setHikariStats(Metrics.instance().hikariStats)
                .setHibernateStats(Metrics.instance().hibernateStats)
                .build();
        Metrics.instance().hibernateStats.register();

        //adjusting the ehcache config
        if (!Config.CONFIG.isUseSshTunnel()) {
            //local database: turn off overflow to disk of the cache
            for (CacheManager cacheManager : CacheManager.ALL_CACHE_MANAGERS) {
                for (String cacheName : cacheManager.getCacheNames()) {
                    CacheConfiguration cacheConfig = cacheManager.getCache(cacheName).getCacheConfiguration();
                    cacheConfig.getPersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE);
                }
            }
        }
        for (CacheManager cacheManager : CacheManager.ALL_CACHE_MANAGERS) {
            log.debug(cacheManager.getActiveConfigurationText());
        }

        return databaseConnection;
    }
}
