package com.undernine.utils.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Under-Utils 自动配置属性。
 *
 * @author Under-Utils Team
 * @version 1.0.2
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "under.utils")
public class UnderUtilsProperties {

    private Web web = new Web();

    private Redis redis = new Redis();

    public Web getWeb() {
        return web;
    }

    public void setWeb(Web web) {
        this.web = web;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    /**
     * Web 横切能力配置。
     */
    public static class Web {
        private OperationContext operationContext = new OperationContext();
        private StoreCapability rateLimit = new StoreCapability();
        private StoreCapability repeatSubmit = new StoreCapability();
        private ExceptionHandling exceptionHandling = new ExceptionHandling();

        public OperationContext getOperationContext() {
            return operationContext;
        }

        public void setOperationContext(OperationContext operationContext) {
            this.operationContext = operationContext;
        }

        public StoreCapability getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(StoreCapability rateLimit) {
            this.rateLimit = rateLimit;
        }

        public StoreCapability getRepeatSubmit() {
            return repeatSubmit;
        }

        public void setRepeatSubmit(StoreCapability repeatSubmit) {
            this.repeatSubmit = repeatSubmit;
        }

        public ExceptionHandling getExceptionHandling() {
            return exceptionHandling;
        }

        public void setExceptionHandling(ExceptionHandling exceptionHandling) {
            this.exceptionHandling = exceptionHandling;
        }
    }

    /**
     * Redis 能力配置。
     */
    public static class Redis {
        private boolean lockEnabled = true;
        private String lockKeyPrefix = "under-utils:lock:";
        private Cache cache = new Cache();
        private LogicalCache logicalCache = new LogicalCache();

        public boolean isLockEnabled() {
            return lockEnabled;
        }

        public void setLockEnabled(boolean lockEnabled) {
            this.lockEnabled = lockEnabled;
        }

        public String getLockKeyPrefix() {
            return lockKeyPrefix;
        }

        public void setLockKeyPrefix(String lockKeyPrefix) {
            this.lockKeyPrefix = lockKeyPrefix;
        }

        public Cache getCache() {
            return cache;
        }

        public void setCache(Cache cache) {
            this.cache = cache;
        }

        public LogicalCache getLogicalCache() {
            return logicalCache;
        }

        public void setLogicalCache(LogicalCache logicalCache) {
            this.logicalCache = logicalCache;
        }
    }

    /**
     * 请求操作上下文配置。
     * <p>
     * 属性前缀：{@code under.utils.web.operation-context}。
     * 开启后自动注册 {@code OperationContextFilter}，用于在一次请求内聚合用户、租户和 traceId。
     * {@code task-decorator-enabled} 只控制异步任务上下文传播装饰器；当用户已经声明任意
     * {@code TaskDecorator} Bean 时，starter 会退让给用户配置。
     * {@code trusted-proxy-headers} 只有在可信网关已经清洗
     * {@code X-Forwarded-For}/{@code X-Real-IP} 时才应开启。
     * </p>
     */
    public static class OperationContext {
        private boolean enabled = true;
        private boolean taskDecoratorEnabled = true;
        private boolean trustedIdentityHeaders = false;
        private boolean trustedProxyHeaders = false;
        private int order = Integer.MIN_VALUE + 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTaskDecoratorEnabled() {
            return taskDecoratorEnabled;
        }

        public void setTaskDecoratorEnabled(boolean taskDecoratorEnabled) {
            this.taskDecoratorEnabled = taskDecoratorEnabled;
        }

        public boolean isTrustedIdentityHeaders() {
            return trustedIdentityHeaders;
        }

        public void setTrustedIdentityHeaders(boolean trustedIdentityHeaders) {
            this.trustedIdentityHeaders = trustedIdentityHeaders;
        }

        public boolean isTrustedProxyHeaders() {
            return trustedProxyHeaders;
        }

        public void setTrustedProxyHeaders(boolean trustedProxyHeaders) {
            this.trustedProxyHeaders = trustedProxyHeaders;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }

    /**
     * 全局异常处理配置。
     * <p>
     * 属性前缀：{@code under.utils.web.exception-handling}。
     * 默认关闭，避免 starter 接管业务已有 API 响应契约。
     * </p>
     */
    public static class ExceptionHandling {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Redis cache-aside 缓存治理配置。
     * <p>
     * 属性前缀：{@code under.utils.redis.cache}。
     * 该开关只控制普通 cache-aside 的 {@code CacheOptions} 与 {@code CacheAsideTemplate}
     * 自动装配，不影响 {@code under.utils.redis.logical-cache}。逻辑过期缓存使用独立开关，
     * 但会复用同一个 {@code CacheValueCodec} 编解码抽象。
     * </p>
     */
    public static class Cache {
        private boolean enabled = true;
        private Duration ttl = Duration.ofMinutes(5);
        private Duration nullTtl = Duration.ofSeconds(30);
        private Duration jitter = Duration.ZERO;
        private boolean cacheNull = true;
        private String keyPrefix = "under-utils:cache:";
        private boolean rebuildLockEnabled = true;
        private String rebuildLockKeyPrefix = "under-utils:cache:rebuild-lock:";
        private Duration lockWaitTime = Duration.ofSeconds(1);
        private Duration lockLeaseTime = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public Duration getNullTtl() {
            return nullTtl;
        }

        public void setNullTtl(Duration nullTtl) {
            this.nullTtl = nullTtl;
        }

        public Duration getJitter() {
            return jitter;
        }

        public void setJitter(Duration jitter) {
            this.jitter = jitter;
        }

        public boolean isCacheNull() {
            return cacheNull;
        }

        public void setCacheNull(boolean cacheNull) {
            this.cacheNull = cacheNull;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public boolean isRebuildLockEnabled() {
            return rebuildLockEnabled;
        }

        public void setRebuildLockEnabled(boolean rebuildLockEnabled) {
            this.rebuildLockEnabled = rebuildLockEnabled;
        }

        public String getRebuildLockKeyPrefix() {
            return rebuildLockKeyPrefix;
        }

        public void setRebuildLockKeyPrefix(String rebuildLockKeyPrefix) {
            this.rebuildLockKeyPrefix = rebuildLockKeyPrefix;
        }

        public Duration getLockWaitTime() {
            return lockWaitTime;
        }

        public void setLockWaitTime(Duration lockWaitTime) {
            this.lockWaitTime = lockWaitTime;
        }

        public Duration getLockLeaseTime() {
            return lockLeaseTime;
        }

        public void setLockLeaseTime(Duration lockLeaseTime) {
            this.lockLeaseTime = lockLeaseTime;
        }
    }

    /**
     * Redis 逻辑过期缓存配置。
     * <p>
     * 属性前缀：{@code under.utils.redis.logical-cache}。
     * 默认关闭，开启后自动装配逻辑过期模板及其刷新线程池。该能力与普通 cache-aside 的
     * {@code under.utils.redis.cache.enabled} 相互独立，适合热点数据异步重建场景。
     * </p>
     */
    public static class LogicalCache {
        private boolean enabled = false;
        private Duration logicalTtl = Duration.ofMinutes(5);
        private Duration physicalTtl = Duration.ofMinutes(30);
        private boolean cacheNull = true;
        private String keyPrefix = "under-utils:logical-cache:";
        private String rebuildLockKeyPrefix = "under-utils:logical-cache:rebuild-lock:";
        private Duration lockWaitTime = Duration.ofSeconds(1);
        private Duration lockLeaseTime = Duration.ofSeconds(30);
        private int corePoolSize = 1;
        private int maxPoolSize = 4;
        private int queueCapacity = 1024;
        private String threadNamePrefix = "under-logical-cache-";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getLogicalTtl() {
            return logicalTtl;
        }

        public void setLogicalTtl(Duration logicalTtl) {
            this.logicalTtl = logicalTtl;
        }

        public Duration getPhysicalTtl() {
            return physicalTtl;
        }

        public void setPhysicalTtl(Duration physicalTtl) {
            this.physicalTtl = physicalTtl;
        }

        public boolean isCacheNull() {
            return cacheNull;
        }

        public void setCacheNull(boolean cacheNull) {
            this.cacheNull = cacheNull;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getRebuildLockKeyPrefix() {
            return rebuildLockKeyPrefix;
        }

        public void setRebuildLockKeyPrefix(String rebuildLockKeyPrefix) {
            this.rebuildLockKeyPrefix = rebuildLockKeyPrefix;
        }

        public Duration getLockWaitTime() {
            return lockWaitTime;
        }

        public void setLockWaitTime(Duration lockWaitTime) {
            this.lockWaitTime = lockWaitTime;
        }

        public Duration getLockLeaseTime() {
            return lockLeaseTime;
        }

        public void setLockLeaseTime(Duration lockLeaseTime) {
            this.lockLeaseTime = lockLeaseTime;
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }

    /**
     * 可切换存储的能力配置。
     * <p>
     * 用于 {@code under.utils.web.rate-limit} 和 {@code under.utils.web.repeat-submit}。
     * {@code enabled=false} 会关闭对应切面；{@code store=local} 使用 JVM 本地状态，只适合单实例或测试环境；
     * {@code store=redis} 使用 Redis 共享状态，要求应用上下文中存在 {@code RedissonClient}。
     * {@code local-max-entries} 和 {@code local-cleanup-interval} 只影响 local store。
     * </p>
     */
    public static class StoreCapability {
        private boolean enabled = true;
        private StoreType store = StoreType.LOCAL;
        private int localMaxEntries = 100_000;
        private Duration localCleanupInterval = Duration.ofSeconds(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public StoreType getStore() {
            return store;
        }

        public void setStore(StoreType store) {
            this.store = store;
        }

        public int getLocalMaxEntries() {
            return localMaxEntries;
        }

        public void setLocalMaxEntries(int localMaxEntries) {
            this.localMaxEntries = localMaxEntries;
        }

        public Duration getLocalCleanupInterval() {
            return localCleanupInterval;
        }

        public void setLocalCleanupInterval(Duration localCleanupInterval) {
            this.localCleanupInterval = localCleanupInterval;
        }
    }

    /**
     * 状态存储类型。
     */
    public enum StoreType {
        /**
         * JVM 本地状态，不跨实例共享。
         */
        LOCAL,
        /**
         * Redis 分布式状态，依赖 RedissonClient。
         */
        REDIS
    }
}
