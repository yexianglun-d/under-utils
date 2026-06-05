package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.redis.cache.CacheAsideTemplate;
import com.undernine.utils.redis.cache.CacheOperationObserver;
import com.undernine.utils.redis.cache.CacheOptions;
import com.undernine.utils.redis.cache.CacheValueCodec;
import com.undernine.utils.redis.cache.JacksonCacheValueCodec;
import com.undernine.utils.redis.cache.LogicalExpireCacheOptions;
import com.undernine.utils.redis.cache.LogicalExpireCacheRefreshFailureHandler;
import com.undernine.utils.redis.cache.LogicalExpireCacheTemplate;
import com.undernine.utils.redis.cache.MicrometerCacheOperationObserver;
import com.undernine.utils.redis.idempotent.RedisIdempotencyStore;
import com.undernine.utils.redis.lock.DistributedLockTemplate;
import com.undernine.utils.redis.ratelimit.RedisRateLimitStore;
import com.undernine.utils.redis.repeat.RedisRepeatSubmitStore;
import com.undernine.utils.spring.idempotent.IdempotencyResultCodec;
import com.undernine.utils.spring.idempotent.IdempotencyStore;
import com.undernine.utils.spring.ratelimit.RateLimitStore;
import com.undernine.utils.spring.repeat.RepeatSubmitStore;
import com.undernine.utils.starter.properties.UnderUtilsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Under-Utils Redis 自动配置入口。
 *
 * @author Under-Utils Team
 * @version 1.0.2
 * @since 1.0.2
 */
@AutoConfiguration(after = UnderUtilsSpringAutoConfiguration.class)
@EnableConfigurationProperties(UnderUtilsProperties.class)
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnBean(RedissonClient.class)
public class UnderUtilsRedisAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
            "io.micrometer.core.instrument.MeterRegistry",
            "io.micrometer.observation.ObservationRegistry",
            "com.undernine.utils.redis.cache.MicrometerCacheOperationObserver"
    })
    static class CacheObservationConfiguration {

        @Bean
        @ConditionalOnMissingBean(CacheOperationObserver.class)
        @ConditionalOnBean(MeterRegistry.class)
        @ConditionalOnExpression("T(Boolean).parseBoolean('${under.utils.redis.cache.enabled:true}')"
                + " || T(Boolean).parseBoolean('${under.utils.redis.logical-cache.enabled:false}')")
        @ConditionalOnProperty(prefix = "under.utils.redis.observation", name = "enabled",
                havingValue = "true", matchIfMissing = true)
        public CacheOperationObserver micrometerCacheOperationObserver(
                MeterRegistry meterRegistry,
                ObjectProvider<ObservationRegistry> observationRegistry) {
            return new MicrometerCacheOperationObserver(
                    meterRegistry,
                    observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP)
            );
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.web.rate-limit", name = "store", havingValue = "redis")
    public RateLimitStore redisRateLimitStore(RedissonClient redissonClient) {
        return new RedisRateLimitStore(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.web.repeat-submit", name = "store", havingValue = "redis")
    public RepeatSubmitStore redisRepeatSubmitStore(RedissonClient redissonClient) {
        return new RedisRepeatSubmitStore(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.idempotent", name = "store", havingValue = "redis")
    public IdempotencyStore redisIdempotencyStore(RedissonClient redissonClient,
                                                  IdempotencyResultCodec resultCodec,
                                                  UnderUtilsProperties properties) {
        return new RedisIdempotencyStore(
                redissonClient,
                resultCodec,
                properties.getIdempotent().getKeyPrefix()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.redis", name = "lock-enabled", havingValue = "true", matchIfMissing = true)
    public DistributedLockTemplate distributedLockTemplate(RedissonClient redissonClient,
                                                          UnderUtilsProperties properties) {
        return new DistributedLockTemplate(redissonClient, properties.getRedis().getLockKeyPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("T(Boolean).parseBoolean('${under.utils.redis.cache.enabled:true}')"
            + " || T(Boolean).parseBoolean('${under.utils.redis.logical-cache.enabled:false}')")
    public CacheValueCodec cacheValueCodec() {
        return new JacksonCacheValueCodec();
    }

    @Bean
    @ConditionalOnMissingBean({CacheOptions.class, CacheAsideTemplate.class})
    @ConditionalOnProperty(prefix = "under.utils.redis.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheOptions cacheOptions(UnderUtilsProperties properties) {
        UnderUtilsProperties.Cache cache = properties.getRedis().getCache();
        return CacheOptions.builder()
                .ttl(cache.getTtl())
                .nullTtl(cache.getNullTtl())
                .jitter(cache.getJitter())
                .cacheNull(cache.isCacheNull())
                .keyPrefix(cache.getKeyPrefix())
                .rebuildLockEnabled(cache.isRebuildLockEnabled())
                .rebuildLockKeyPrefix(cache.getRebuildLockKeyPrefix())
                .lockWaitTime(cache.getLockWaitTime())
                .lockLeaseTime(cache.getLockLeaseTime())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.redis.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheAsideTemplate cacheAsideTemplate(RedissonClient redissonClient,
                                                 CacheValueCodec cacheValueCodec,
                                                 CacheOptions cacheOptions,
                                                 ObjectProvider<CacheOperationObserver> operationObserver) {
        return new CacheAsideTemplate(redissonClient, cacheValueCodec, cacheOptions,
                operationObserver.getIfAvailable(CacheOperationObserver::noop));
    }

    @Bean(name = "underUtilsLogicalCacheRefreshExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "underUtilsLogicalCacheRefreshExecutor")
    @ConditionalOnProperty(prefix = "under.utils.redis.logical-cache", name = "enabled", havingValue = "true")
    public ThreadPoolTaskExecutor underUtilsLogicalCacheRefreshExecutor(UnderUtilsProperties properties) {
        UnderUtilsProperties.LogicalCache logicalCache = properties.getRedis().getLogicalCache();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(logicalCache.getCorePoolSize());
        executor.setMaxPoolSize(logicalCache.getMaxPoolSize());
        executor.setQueueCapacity(logicalCache.getQueueCapacity());
        executor.setThreadNamePrefix(logicalCache.getThreadNamePrefix());
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean({LogicalExpireCacheOptions.class, LogicalExpireCacheTemplate.class})
    @ConditionalOnProperty(prefix = "under.utils.redis.logical-cache", name = "enabled", havingValue = "true")
    public LogicalExpireCacheOptions logicalExpireCacheOptions(
            UnderUtilsProperties properties,
            @Qualifier("underUtilsLogicalCacheRefreshExecutor") Executor refreshExecutor,
            ObjectProvider<LogicalExpireCacheRefreshFailureHandler> refreshFailureHandler) {
        UnderUtilsProperties.LogicalCache logicalCache = properties.getRedis().getLogicalCache();
        return LogicalExpireCacheOptions.builder()
                .logicalTtl(logicalCache.getLogicalTtl())
                .physicalTtl(logicalCache.getPhysicalTtl())
                .cacheNull(logicalCache.isCacheNull())
                .keyPrefix(logicalCache.getKeyPrefix())
                .rebuildLockKeyPrefix(logicalCache.getRebuildLockKeyPrefix())
                .lockWaitTime(logicalCache.getLockWaitTime())
                .lockLeaseTime(logicalCache.getLockLeaseTime())
                .refreshExecutor(refreshExecutor)
                .refreshFailureHandler(refreshFailureHandler.getIfAvailable())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.redis.logical-cache", name = "enabled", havingValue = "true")
    public LogicalExpireCacheTemplate logicalExpireCacheTemplate(RedissonClient redissonClient,
                                                                 CacheValueCodec cacheValueCodec,
                                                                 LogicalExpireCacheOptions logicalExpireCacheOptions,
                                                                 ObjectProvider<CacheOperationObserver> operationObserver) {
        return new LogicalExpireCacheTemplate(redissonClient, cacheValueCodec, logicalExpireCacheOptions,
                operationObserver.getIfAvailable(CacheOperationObserver::noop));
    }
}
