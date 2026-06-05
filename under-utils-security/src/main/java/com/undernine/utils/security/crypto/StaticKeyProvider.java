package com.undernine.utils.security.crypto;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 静态字段加密密钥提供者。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class StaticKeyProvider implements KeyProvider {

    private final String currentKeyId;
    private final Map<String, EncryptionKey> keys;

    public StaticKeyProvider(EncryptionKey currentKey) {
        this(currentKey.getKeyId(), Map.of(currentKey.getKeyId(), currentKey));
    }

    public StaticKeyProvider(String currentKeyId, Map<String, EncryptionKey> keys) {
        if (currentKeyId == null || currentKeyId.trim().isEmpty()) {
            throw new IllegalArgumentException("currentKeyId must not be blank");
        }
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("keys must not be empty");
        }
        if (!keys.containsKey(currentKeyId)) {
            throw new IllegalArgumentException("current key must exist in keys");
        }
        this.currentKeyId = currentKeyId;
        this.keys = Map.copyOf(keys);
    }

    public static StaticKeyProvider ofBase64(String keyId, String base64Key) {
        return new StaticKeyProvider(EncryptionKey.ofBase64(keyId, base64Key));
    }

    @Override
    public EncryptionKey currentKey() {
        return Objects.requireNonNull(keys.get(currentKeyId), "current key must not be null");
    }

    @Override
    public Optional<EncryptionKey> findKey(String keyId) {
        return Optional.ofNullable(keys.get(keyId));
    }
}
