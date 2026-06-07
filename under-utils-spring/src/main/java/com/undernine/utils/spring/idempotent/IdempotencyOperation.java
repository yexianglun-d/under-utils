package com.undernine.utils.spring.idempotent;

/**
 * 业务幂等内部操作类型。
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
public enum IdempotencyOperation {

    /**
     * 登记或读取幂等 key。
     */
    BEGIN,

    /**
     * 执行业务方法。
     */
    BUSINESS,

    /**
     * 写入完成态结果。
     */
    COMPLETE,

    /**
     * 业务异常后的 owner 释放。
     */
    RELEASE
}
