package com.undernine.utils.spring.idempotent;

/**
 * 幂等能力基础异常。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class IdempotencyException extends RuntimeException {

    public IdempotencyException(String message) {
        super(message);
    }

    public IdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
