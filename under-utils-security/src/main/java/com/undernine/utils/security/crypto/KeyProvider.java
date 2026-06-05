package com.undernine.utils.security.crypto;

import java.util.Optional;

/**
 * 字段加密密钥提供者。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public interface KeyProvider {

    /**
     * 当前写入使用的密钥。
     *
     * @return 当前密钥
     */
    EncryptionKey currentKey();

    /**
     * 按 keyId 查找历史密钥。
     *
     * @param keyId 密钥 ID
     * @return 密钥
     */
    Optional<EncryptionKey> findKey(String keyId);
}
