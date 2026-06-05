package com.undernine.utils.spring.idempotent;

/**
 * 相同幂等 key 的首次调用仍在处理中。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class IdempotentInProgressException extends IdempotencyException {

    public IdempotentInProgressException(String message) {
        super(message);
    }
}
