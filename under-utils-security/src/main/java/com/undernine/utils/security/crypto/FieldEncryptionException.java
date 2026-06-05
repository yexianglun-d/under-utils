package com.undernine.utils.security.crypto;

/**
 * 字段加密异常。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class FieldEncryptionException extends RuntimeException {

    public FieldEncryptionException(String message) {
        super(message);
    }

    public FieldEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
