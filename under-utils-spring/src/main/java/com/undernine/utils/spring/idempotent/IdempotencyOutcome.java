package com.undernine.utils.spring.idempotent;

/**
 * 业务幂等操作结果。
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
public enum IdempotencyOutcome {

    /**
     * 当前调用获得首次执行资格。
     */
    ACQUIRED,

    /**
     * 复用已完成的首次执行结果。
     */
    COMPLETED,

    /**
     * 相同 key 已有调用正在执行。
     */
    IN_PROGRESS,

    /**
     * 操作成功。
     */
    SUCCESS,

    /**
     * 操作失败。
     */
    FAILURE,

    /**
     * owner 已过期或被替换，操作未生效。
     */
    SKIPPED
}
