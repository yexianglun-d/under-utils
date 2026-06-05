package com.undernine.utils.spring.idempotent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JVM 本地业务幂等状态存储。
 * <p>
 * 适合单实例应用或本地开发。多实例部署时应替换为 Redis 等分布式实现。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class LocalIdempotencyStore implements IdempotencyStore, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LocalIdempotencyStore.class);

    private static final int DEFAULT_MAX_ENTRIES = 100_000;
    private static final Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofSeconds(1);
    private static final String DEFAULT_KEY_PREFIX = "under-utils:idempotent:";
    private static final AtomicLong THREAD_SEQUENCE = new AtomicLong();

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final IdempotencyResultCodec resultCodec;
    private final int maxEntries;
    private final String keyPrefix;
    private final long cleanupIntervalMillis;
    private volatile ScheduledExecutorService cleanupExecutor;
    private volatile ScheduledFuture<?> cleanupFuture;
    private final AtomicLong nextCleanupAt = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();

    public LocalIdempotencyStore() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_CLEANUP_INTERVAL);
    }

    public LocalIdempotencyStore(int maxEntries) {
        this(maxEntries, DEFAULT_CLEANUP_INTERVAL);
    }

    public LocalIdempotencyStore(int maxEntries, Duration cleanupInterval) {
        this(maxEntries, cleanupInterval, DEFAULT_KEY_PREFIX);
    }

    public LocalIdempotencyStore(int maxEntries, Duration cleanupInterval, String keyPrefix) {
        this(maxEntries, cleanupInterval, keyPrefix, new JacksonIdempotencyResultCodec());
    }

    public LocalIdempotencyStore(int maxEntries,
                                 Duration cleanupInterval,
                                 String keyPrefix,
                                 IdempotencyResultCodec resultCodec) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be greater than 0");
        }
        Duration interval = normalizeDuration(cleanupInterval, "cleanupInterval");
        this.maxEntries = maxEntries;
        this.keyPrefix = keyPrefix == null ? DEFAULT_KEY_PREFIX : keyPrefix;
        this.resultCodec = resultCodec == null ? new JacksonIdempotencyResultCodec() : resultCodec;
        this.cleanupIntervalMillis = Math.max(1L, interval.toMillis());
    }

    @Override
    public synchronized IdempotencyExecution begin(String key, Duration processingTtl, Type resultType) {
        ensureCleanupScheduled();
        String storeKey = storeKey(key);
        long now = System.currentTimeMillis();
        cleanupExpired(now, false);
        Entry entry = entries.get(storeKey);
        if (entry != null && entry.expireAt <= now) {
            entries.remove(storeKey);
            entry = null;
        }
        if (entry != null) {
            return entry.status == IdempotencyStatus.COMPLETED
                    ? IdempotencyExecution.completed(resultCodec.deserialize(entry.resultPayload, resultType))
                    : IdempotencyExecution.inProgress();
        }
        if (entries.size() >= maxEntries) {
            cleanupExpired(now, true);
            if (entries.size() >= maxEntries) {
                throw new IdempotencyException("Local idempotency store is full");
            }
        }
        String executionToken = UUID.randomUUID().toString();
        entries.put(storeKey, Entry.processing(executionToken, now + ttlMillis(processingTtl)));
        return IdempotencyExecution.acquired(executionToken);
    }

    @Override
    public synchronized boolean complete(String key,
                                         String executionToken,
                                         Object result,
                                         Type resultType,
                                         Duration resultTtl) {
        String storeKey = storeKey(key);
        long now = System.currentTimeMillis();
        Entry entry = entries.get(storeKey);
        if (!isCurrentOwner(entry, executionToken, now)) {
            return false;
        }
        String resultPayload = resultCodec.serialize(result, resultType);
        entries.put(storeKey, Entry.completed(resultPayload, now + ttlMillis(resultTtl)));
        return true;
    }

    @Override
    public synchronized void release(String key, String executionToken) {
        String storeKey = storeKey(key);
        Entry entry = entries.get(storeKey);
        if (isCurrentOwner(entry, executionToken, System.currentTimeMillis())) {
            entries.remove(storeKey);
        }
    }

    /**
     * 清空本地状态，主要用于测试或主动重置。
     */
    public void clear() {
        entries.clear();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            ScheduledFuture<?> future = cleanupFuture;
            if (future != null) {
                future.cancel(false);
            }
            ScheduledExecutorService executor = cleanupExecutor;
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    int size() {
        return entries.size();
    }

    private void cleanupExpiredSafely() {
        try {
            cleanupExpired(System.currentTimeMillis(), true);
        } catch (RuntimeException ex) {
            log.warn("Failed to cleanup local idempotency entries", ex);
        }
    }

    private void ensureCleanupScheduled() {
        if (cleanupFuture != null || closed.get()) {
            return;
        }
        synchronized (this) {
            if (cleanupFuture != null || closed.get()) {
                return;
            }
            cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
                    daemonThreadFactory("under-local-idempotent-cleanup-"));
            cleanupFuture = cleanupExecutor.scheduleWithFixedDelay(
                    this::cleanupExpiredSafely,
                    cleanupIntervalMillis,
                    cleanupIntervalMillis,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void cleanupExpired(long now, boolean force) {
        if (!force) {
            long next = nextCleanupAt.get();
            if (now < next || !nextCleanupAt.compareAndSet(next, now + cleanupIntervalMillis)) {
                return;
            }
        }
        entries.entrySet().removeIf(entry -> entry.getValue().expireAt <= now);
    }

    private long ttlMillis(Duration ttl) {
        return Math.max(1L, normalizeDuration(ttl, "ttl").toMillis());
    }

    private String storeKey(String key) {
        return keyPrefix + key;
    }

    private boolean isCurrentOwner(Entry entry, String executionToken, long now) {
        return entry != null
                && entry.status == IdempotencyStatus.IN_PROGRESS
                && entry.expireAt > now
                && entry.executionToken != null
                && entry.executionToken.equals(executionToken);
    }

    private Duration normalizeDuration(Duration duration, String fieldName) {
        Duration actual = duration == null ? DEFAULT_CLEANUP_INTERVAL : duration;
        if (actual.isZero() || actual.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return actual;
    }

    private static ThreadFactory daemonThreadFactory(String namePrefix) {
        return runnable -> {
            Thread thread = new Thread(runnable, namePrefix + THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static final class Entry {
        private final IdempotencyStatus status;
        private final String executionToken;
        private final String resultPayload;
        private final long expireAt;

        private Entry(IdempotencyStatus status, String executionToken, String resultPayload, long expireAt) {
            this.status = status;
            this.executionToken = executionToken;
            this.resultPayload = resultPayload;
            this.expireAt = expireAt;
        }

        private static Entry processing(String executionToken, long expireAt) {
            return new Entry(IdempotencyStatus.IN_PROGRESS, executionToken, null, expireAt);
        }

        private static Entry completed(String resultPayload, long expireAt) {
            return new Entry(IdempotencyStatus.COMPLETED, null, resultPayload, expireAt);
        }
    }
}
