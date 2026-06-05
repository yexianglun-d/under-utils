package com.undernine.utils.security.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * 字段加密密钥。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public final class EncryptionKey {

    private final String keyId;
    private final SecretKey secretKey;

    private EncryptionKey(String keyId, byte[] keyBytes) {
        this.keyId = requireKeyId(keyId);
        byte[] copied = validateKeyBytes(keyBytes);
        this.secretKey = new SecretKeySpec(copied, "AES");
        Arrays.fill(copied, (byte) 0);
    }

    public static EncryptionKey of(String keyId, byte[] keyBytes) {
        return new EncryptionKey(keyId, keyBytes);
    }

    public static EncryptionKey ofBase64(String keyId, String base64Key) {
        Objects.requireNonNull(base64Key, "base64Key must not be null");
        return new EncryptionKey(keyId, Base64.getDecoder().decode(base64Key));
    }

    public String getKeyId() {
        return keyId;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    private static String requireKeyId(String keyId) {
        if (keyId == null || keyId.trim().isEmpty()) {
            throw new IllegalArgumentException("keyId must not be blank");
        }
        String value = keyId.trim();
        if (value.contains(":")) {
            throw new IllegalArgumentException("keyId must not contain ':'");
        }
        return value;
    }

    private static byte[] validateKeyBytes(byte[] keyBytes) {
        Objects.requireNonNull(keyBytes, "keyBytes must not be null");
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key length must be 16, 24, or 32 bytes");
        }
        return Arrays.copyOf(keyBytes, keyBytes.length);
    }
}
