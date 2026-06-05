package com.undernine.utils.spring.idempotent;

/**
 * 幂等 key 当前状态。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public enum IdempotencyStatus {

    /**
     * 当前调用获得首次执行业务的资格。
     */
    ACQUIRED,

    /**
     * 已有相同 key 的调用正在执行。
     */
    IN_PROGRESS,

    /**
     * 已有相同 key 的调用成功完成，并可复用第一次结果。
     */
    COMPLETED
}
