package com.undernine.utils.spring.idempotent;

/**
 * 幂等 key 解析失败。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class IdempotentKeyResolveException extends IdempotencyException {

    public IdempotentKeyResolveException(String message, Throwable cause) {
        super(message, cause);
    }
}
