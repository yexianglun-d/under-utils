package com.undernine.utils.spring.idempotent;

import java.util.Objects;

/**
 * 幂等登记结果。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public final class IdempotencyExecution {

    private final IdempotencyStatus status;
    private final Object result;
    private final String executionToken;

    private IdempotencyExecution(IdempotencyStatus status, Object result, String executionToken) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.result = result;
        this.executionToken = executionToken;
    }

    public static IdempotencyExecution acquired() {
        return acquired(null);
    }

    public static IdempotencyExecution acquired(String executionToken) {
        return new IdempotencyExecution(IdempotencyStatus.ACQUIRED, null, executionToken);
    }

    public static IdempotencyExecution inProgress() {
        return new IdempotencyExecution(IdempotencyStatus.IN_PROGRESS, null, null);
    }

    public static IdempotencyExecution completed(Object result) {
        return new IdempotencyExecution(IdempotencyStatus.COMPLETED, result, null);
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Object getResult() {
        return result;
    }

    public String getExecutionToken() {
        return executionToken;
    }

    public boolean isAcquired() {
        return status == IdempotencyStatus.ACQUIRED;
    }

    public boolean isInProgress() {
        return status == IdempotencyStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == IdempotencyStatus.COMPLETED;
    }
}
