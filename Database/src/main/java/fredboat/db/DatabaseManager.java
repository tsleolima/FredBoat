/*
 *
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

package fredboat.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import io.prometheus.client.hibernate.HibernateStatisticsCollector;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.ssh.SshTunnel;

import javax.annotation.Nullable;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String MAIN_PERSISTENCE_UNIT_NAME = "fredboat.main";
    private static final String CACHE_PERSISTENCE_UNIT_NAME = "fredboat.cache";

    @Nullable
    private final HibernateStatisticsCollector hibernateStats;
    @Nullable
    private final PrometheusMetricsTrackerFactory hikariStats;
    private final int poolsize;
    private final String appName;
    private final boolean migrateAndValidate;
    private final String mainJdbc;
    @Nullable
    private final SshTunnel.SshDetails mainTunnel;
    @Nullable
    private final String cacheJdbc;
    @Nullable
    private final SshTunnel.SshDetails cacheTunnel;


    @Nullable
    private DatabaseWrapper mainDbWrapper;
    private final Object mainDbWrapperInitLock = new Object();

    @Nullable
    private DatabaseWrapper cacheDbWrapper;
    private final Object cacheDbWrapperInitLock = new Object();

    public DatabaseManager(@Nullable HibernateStatisticsCollector hibernateStats,
                           @Nullable PrometheusMetricsTrackerFactory hikariStats,
                           int poolsize,
                           String appName,
                           boolean migrateAndValidate,
                           String mainJdbc,
                           @Nullable SshTunnel.SshDetails mainTunnel,
                           @Nullable String cacheJdbc,
                           @Nullable SshTunnel.SshDetails cacheTunnel) {
        this.hibernateStats = hibernateStats;
        this.hikariStats = hikariStats;
        this.poolsize = poolsize;
        this.appName = appName;
        this.migrateAndValidate = migrateAndValidate;
        this.mainJdbc = mainJdbc;
        this.mainTunnel = mainTunnel;
        this.cacheJdbc = cacheJdbc;
        this.cacheTunnel = cacheTunnel;
    }

    public DatabaseWrapper getMainDbWrapper() {
        DatabaseWrapper singleton = mainDbWrapper;
        if (singleton == null) {
            synchronized (mainDbWrapperInitLock) {
                singleton = mainDbWrapper;
                if (singleton == null) {
                    mainDbWrapper = singleton = initMainDbWrapper();
                }
            }
        }
        return singleton;
    }


    @Nullable //may return null if no cache db is configured
    public DatabaseWrapper getCacheDbWrapper() {
        if (cacheJdbc == null) {
            return null;
        }
        DatabaseWrapper singleton = cacheDbWrapper;
        if (singleton == null) {
            synchronized (cacheDbWrapperInitLock) {
                singleton = cacheDbWrapper;
                if (singleton == null) {
                    cacheDbWrapper = singleton = initCacheWrapper();
                }
            }
        }
        return singleton;
    }

    private DatabaseWrapper initMainDbWrapper()
            throws DatabaseException {
        Flyway flyway = new Flyway();
        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersion(MigrationVersion.fromVersion("0"));
        flyway.setBaselineDescription("Base Migration");
        flyway.setLocations("classpath:fredboat/db/migrations/main");

        HikariConfig hikariConfig = DatabaseConnection.Builder.getDefaultHikariConfig();
        hikariConfig.setMaximumPoolSize(poolsize);

        Properties hibernateProps = DatabaseConnection.Builder.getDefaultHibernateProps();
        hibernateProps.put("hibernate.cache.use_second_level_cache", "true");
        hibernateProps.put("hibernate.cache.use_query_cache", "true");
        hibernateProps.put("net.sf.ehcache.configurationResourceName", "/ehcache_main.xml");
        hibernateProps.put("hibernate.cache.provider_configuration_file_resource_path", "ehcache_main.xml");
        hibernateProps.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        //we use flyway db now for migrations, hibernate shall only run validations
        hibernateProps.put("hibernate.hbm2ddl.auto", "validate");

        if (!migrateAndValidate) {
            flyway = null;
            hibernateProps.put("hibernate.hbm2ddl.auto", "none");
        }

        DatabaseConnection databaseConnection = new DatabaseConnection.Builder(MAIN_PERSISTENCE_UNIT_NAME, mainJdbc)
                .setHikariConfig(hikariConfig)
                .setHibernateProps(hibernateProps)
                .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                .addEntityPackage("fredboat.db.entity.main")
                .setAppName("FredBoat_" + appName)
                .setSshDetails(mainTunnel)
                .setHikariStats(hikariStats)
                .setHibernateStats(hibernateStats)
                .setCheckConnection(false) //we run our own connection check for this with the DBConnectionWatchdogAgent
                .setProxyDataSourceBuilder(new ProxyDataSourceBuilder()
                        .logSlowQueryBySlf4j(10, TimeUnit.SECONDS, SLF4JLogLevel.WARN, "SlowQueryLog")
                        .multiline()
                )
                .setFlyway(flyway)
                .build();

        //adjusting the ehcache config
        if (mainTunnel == null && cacheTunnel == null) {
            //local database: turn off overflow to disk of the cache
            CacheManager cacheManager = CacheManager.getCacheManager("MAIN_CACHEMANAGER");
            for (String cacheName : cacheManager.getCacheNames()) {
                CacheConfiguration cacheConfig = cacheManager.getCache(cacheName).getCacheConfiguration();
                cacheConfig.getPersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE);
            }
        }
        log.debug(CacheManager.getCacheManager("MAIN_CACHEMANAGER").getActiveConfigurationText());

        return new DatabaseWrapper(databaseConnection);
    }


    public DatabaseWrapper initCacheWrapper()
            throws DatabaseException {

        Flyway flyway = new Flyway();
        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersion(MigrationVersion.fromVersion("0"));
        flyway.setBaselineDescription("Base Migration");
        flyway.setLocations("classpath:fredboat/db/migrations/cache");

        HikariConfig hikariConfig = DatabaseConnection.Builder.getDefaultHikariConfig();
        hikariConfig.setMaximumPoolSize(poolsize);

        Properties hibernateProps = DatabaseConnection.Builder.getDefaultHibernateProps();
        hibernateProps.put("hibernate.cache.use_second_level_cache", "true");
        hibernateProps.put("hibernate.cache.use_query_cache", "true");
        hibernateProps.put("net.sf.ehcache.configurationResourceName", "/ehcache_cache.xml");
        hibernateProps.put("hibernate.cache.provider_configuration_file_resource_path", "ehcache_cache.xml");
        hibernateProps.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        //we use flyway db now for migrations, hibernate shall only run validations
        hibernateProps.put("hibernate.hbm2ddl.auto", "validate");

        if (!migrateAndValidate) {
            flyway = null;
            hibernateProps.put("hibernate.hbm2ddl.auto", "none");
        }
        if (cacheJdbc == null) {
            throw new IllegalStateException("Trying to build a cache db connectiong with a null cache jdbc url");
        }

        DatabaseConnection databaseConnection = new DatabaseConnection.Builder(CACHE_PERSISTENCE_UNIT_NAME, cacheJdbc)
                .setHikariConfig(hikariConfig)
                .setHibernateProps(hibernateProps)
                .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                .addEntityPackage("fredboat.db.entity.cache")
                .setAppName("FredBoat_" + appName)
                .setSshDetails(cacheTunnel)
                .setHikariStats(hikariStats)
                .setHibernateStats(hibernateStats)
                .setProxyDataSourceBuilder(new ProxyDataSourceBuilder()
                        .logSlowQueryBySlf4j(10, TimeUnit.SECONDS, SLF4JLogLevel.WARN, "SlowQueryLog")
                        .multiline()
                )
                .setFlyway(flyway)
                .build();

        //adjusting the ehcache config
        if (mainTunnel == null && cacheTunnel == null) {
            //local database: turn off overflow to disk of the cache
            CacheManager cacheManager = CacheManager.getCacheManager("CACHE_CACHEMANAGER");
            for (String cacheName : cacheManager.getCacheNames()) {
                CacheConfiguration cacheConfig = cacheManager.getCache(cacheName).getCacheConfiguration();
                cacheConfig.getPersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE);
            }
        }
        log.debug(CacheManager.getCacheManager("CACHE_CACHEMANAGER").getActiveConfigurationText());

        return new DatabaseWrapper(databaseConnection);
    }
}
