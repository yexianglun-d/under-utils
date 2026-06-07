package com.undernine.utils.spring.idempotent;

import java.util.Objects;

/**
 * 业务幂等观察事件。
 * <p>
 * 事件不携带业务 key，避免监控系统出现高基数指标或泄漏敏感参数。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
public final class IdempotencyEvent {

    private final IdempotencyOperation operation;
    private final IdempotencyOutcome outcome;
    private final long durationNanos;
    private final Throwable error;

    private IdempotencyEvent(IdempotencyOperation operation,
                             IdempotencyOutcome outcome,
                             long durationNanos,
                             Throwable error) {
        this.operation = Objects.requireNonNull(operation, "operation must not be null");
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.durationNanos = Math.max(0L, durationNanos);
        this.error = error;
    }

    public static IdempotencyEvent of(IdempotencyOperation operation,
                                      IdempotencyOutcome outcome,
                                      long durationNanos) {
        return new IdempotencyEvent(operation, outcome, durationNanos, null);
    }

    public static IdempotencyEvent failure(IdempotencyOperation operation,
                                           long durationNanos,
                                           Throwable error) {
        return new IdempotencyEvent(operation, IdempotencyOutcome.FAILURE, durationNanos, error);
    }

    public IdempotencyOperation getOperation() {
        return operation;
    }

    public IdempotencyOutcome getOutcome() {
        return outcome;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public Throwable getError() {
        return error;
    }
}
