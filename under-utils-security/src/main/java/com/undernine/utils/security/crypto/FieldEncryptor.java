package com.undernine.utils.security.crypto;

/**
 * 字段级加密器。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public interface FieldEncryptor {

    /**
     * 加密明文。
     *
     * @param plaintext 明文
     * @return 密文 envelope
     */
    String encrypt(String plaintext);

    /**
     * 解密密文 envelope。
     *
     * @param ciphertext 密文 envelope
     * @return 明文
     */
    String decrypt(String ciphertext);
}
