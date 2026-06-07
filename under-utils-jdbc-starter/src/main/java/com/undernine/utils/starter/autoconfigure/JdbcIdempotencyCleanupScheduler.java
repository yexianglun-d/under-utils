package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.jdbc.idempotent.JdbcIdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JDBC 幂等过期记录清理任务。
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
public final class JdbcIdempotencyCleanupScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JdbcIdempotencyCleanupScheduler.class);
    private static final AtomicLong THREAD_SEQUENCE = new AtomicLong();

    private final JdbcIdempotencyStore idempotencyStore;
    private final ScheduledExecutorService executorService;
    private final ScheduledFuture<?> cleanupFuture;

    public JdbcIdempotencyCleanupScheduler(JdbcIdempotencyStore idempotencyStore,
                                           Duration initialDelay,
                                           Duration interval) {
        this.idempotencyStore = Objects.requireNonNull(idempotencyStore, "idempotencyStore must not be null");
        Duration actualInitialDelay = normalizeInitialDelay(initialDelay);
        Duration actualInterval = normalizeInterval(interval);
        this.executorService = Executors.newSingleThreadScheduledExecutor(
                daemonThreadFactory("under-jdbc-idempotent-cleanup-"));
        this.cleanupFuture = executorService.scheduleWithFixedDelay(
                this::cleanupSafely,
                actualInitialDelay.toMillis(),
                Math.max(1L, actualInterval.toMillis()),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 立即执行一次过期记录清理。
     *
     * @return 删除行数
     */
    public int cleanupOnce() {
        return idempotencyStore.cleanupExpired();
    }

    @Override
    public void close() {
        cleanupFuture.cancel(false);
        executorService.shutdownNow();
    }

    private void cleanupSafely() {
        try {
            int deleted = cleanupOnce();
            if (deleted > 0) {
                log.debug("Cleaned {} expired JDBC idempotency records", deleted);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to cleanup expired JDBC idempotency records", ex);
        }
    }

    private Duration normalizeInitialDelay(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ofMinutes(1);
        }
        return duration;
    }

    private Duration normalizeInterval(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return Duration.ofMinutes(1);
        }
        return duration;
    }

    private static ThreadFactory daemonThreadFactory(String namePrefix) {
        return runnable -> {
            Thread thread = new Thread(runnable, namePrefix + THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
