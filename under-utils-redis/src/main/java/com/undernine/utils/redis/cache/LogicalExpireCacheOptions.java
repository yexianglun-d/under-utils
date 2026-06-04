package com.undernine.utils.redis.cache;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 逻辑过期缓存模板配置。
 * <p>
 * {@link #logicalTtl()} 表示业务值的逻辑新鲜期，超过该时间后模板仍可返回旧值并触发后台刷新；
 * {@link #physicalTtl()} 表示 Redis key 的物理存活时间，应大于逻辑 TTL，以保留过期旧值作为兜底。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class LogicalExpireCacheOptions {

    private static final String DEFAULT_KEY_PREFIX = "under-utils:logical-cache:";
    private static final String DEFAULT_REBUILD_LOCK_KEY_PREFIX = "under-utils:logical-cache:rebuild-lock:";
    private static final Duration DEFAULT_LOGICAL_TTL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_PHYSICAL_TTL = Duration.ofMinutes(30);
    private static final Duration DEFAULT_LOCK_WAIT_TIME = Duration.ofSeconds(1);
    private static final Duration DEFAULT_LOCK_LEASE_TIME = Duration.ofSeconds(30);
    private static final int DEFAULT_REFRESH_QUEUE_CAPACITY = 1024;
    private static final Executor DEFAULT_REFRESH_EXECUTOR = createDefaultRefreshExecutor();

    private final Duration logicalTtl;
    private final Duration physicalTtl;
    private final boolean cacheNull;
    private final String keyPrefix;
    private final String rebuildLockKeyPrefix;
    private final Duration lockWaitTime;
    private final Duration lockLeaseTime;
    private final Executor refreshExecutor;
    private final LogicalExpireCacheRefreshFailureHandler refreshFailureHandler;

    private LogicalExpireCacheOptions(Builder builder) {
        this.logicalTtl = requirePositive(builder.logicalTtl, "logicalTtl");
        this.physicalTtl = requirePositive(builder.physicalTtl, "physicalTtl");
        requirePhysicalTtlLongerThanLogicalTtl(this.logicalTtl, this.physicalTtl);
        this.cacheNull = builder.cacheNull;
        this.keyPrefix = builder.keyPrefix == null ? "" : builder.keyPrefix;
        this.rebuildLockKeyPrefix = builder.rebuildLockKeyPrefix == null ? "" : builder.rebuildLockKeyPrefix;
        this.lockWaitTime = requireNotNegative(builder.lockWaitTime, "lockWaitTime");
        this.lockLeaseTime = requirePositive(builder.lockLeaseTime, "lockLeaseTime");
        this.refreshExecutor = Objects.requireNonNull(
            builder.refreshExecutor,
            "refreshExecutor must not be null"
        );
        this.refreshFailureHandler = builder.refreshFailureHandler;
    }

    public static LogicalExpireCacheOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
            .logicalTtl(logicalTtl)
            .physicalTtl(physicalTtl)
            .cacheNull(cacheNull)
            .keyPrefix(keyPrefix)
            .rebuildLockKeyPrefix(rebuildLockKeyPrefix)
            .lockWaitTime(lockWaitTime)
            .lockLeaseTime(lockLeaseTime)
            .refreshExecutor(refreshExecutor)
            .refreshFailureHandler(refreshFailureHandler);
    }

    public Duration logicalTtl() {
        return logicalTtl;
    }

    public Duration physicalTtl() {
        return physicalTtl;
    }

    public boolean cacheNull() {
        return cacheNull;
    }

    /**
     * 是否缓存空值占位。
     *
     * @return true 表示缓存空值占位
     */
    public boolean nullValueCacheEnabled() {
        return cacheNull();
    }

    public String keyPrefix() {
        return keyPrefix;
    }

    public String rebuildLockKeyPrefix() {
        return rebuildLockKeyPrefix;
    }

    public Duration lockWaitTime() {
        return lockWaitTime;
    }

    public Duration lockLeaseTime() {
        return lockLeaseTime;
    }

    public Executor refreshExecutor() {
        return refreshExecutor;
    }

    public LogicalExpireCacheRefreshFailureHandler refreshFailureHandler() {
        return refreshFailureHandler;
    }

    private static Duration requirePositive(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }

    private static Duration requireNotNegative(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return duration;
    }

    private static void requirePhysicalTtlLongerThanLogicalTtl(Duration logicalTtl, Duration physicalTtl) {
        if (physicalTtl.compareTo(logicalTtl) <= 0) {
            throw new IllegalArgumentException("physicalTtl must be greater than logicalTtl");
        }
    }

    private static Executor createDefaultRefreshExecutor() {
        int threadCount = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 4));
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable,
                    "under-utils-logical-cache-refresh-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
        return new ThreadPoolExecutor(
            threadCount,
            threadCount,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(DEFAULT_REFRESH_QUEUE_CAPACITY),
            threadFactory,
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * LogicalExpireCacheOptions 构造器。
     */
    public static final class Builder {

        private Duration logicalTtl = DEFAULT_LOGICAL_TTL;
        private Duration physicalTtl = DEFAULT_PHYSICAL_TTL;
        private boolean cacheNull = true;
        private String keyPrefix = DEFAULT_KEY_PREFIX;
        private String rebuildLockKeyPrefix = DEFAULT_REBUILD_LOCK_KEY_PREFIX;
        private Duration lockWaitTime = DEFAULT_LOCK_WAIT_TIME;
        private Duration lockLeaseTime = DEFAULT_LOCK_LEASE_TIME;
        private Executor refreshExecutor = DEFAULT_REFRESH_EXECUTOR;
        private LogicalExpireCacheRefreshFailureHandler refreshFailureHandler;

        private Builder() {
        }

        public Builder logicalTtl(Duration logicalTtl) {
            this.logicalTtl = logicalTtl;
            return this;
        }

        public Builder physicalTtl(Duration physicalTtl) {
            this.physicalTtl = physicalTtl;
            return this;
        }

        public Builder cacheNull(boolean cacheNull) {
            this.cacheNull = cacheNull;
            return this;
        }

        /**
         * 设置是否缓存空值占位，等价于 {@link #cacheNull(boolean)}。
         *
         * @param enabled true 表示缓存空值占位
         * @return 当前构建器
         */
        public Builder nullValueCacheEnabled(boolean enabled) {
            return cacheNull(enabled);
        }

        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        public Builder rebuildLockKeyPrefix(String rebuildLockKeyPrefix) {
            this.rebuildLockKeyPrefix = rebuildLockKeyPrefix;
            return this;
        }

        public Builder lockWaitTime(Duration lockWaitTime) {
            this.lockWaitTime = lockWaitTime;
            return this;
        }

        public Builder lockLeaseTime(Duration lockLeaseTime) {
            this.lockLeaseTime = lockLeaseTime;
            return this;
        }

        public Builder refreshExecutor(Executor refreshExecutor) {
            this.refreshExecutor = refreshExecutor;
            return this;
        }

        public Builder refreshFailureHandler(LogicalExpireCacheRefreshFailureHandler refreshFailureHandler) {
            this.refreshFailureHandler = refreshFailureHandler;
            return this;
        }

        public LogicalExpireCacheOptions build() {
            return new LogicalExpireCacheOptions(this);
        }
    }
}
