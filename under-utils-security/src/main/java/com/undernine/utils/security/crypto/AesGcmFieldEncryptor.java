package com.undernine.utils.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * AES-GCM 字段加密器。
 * <p>
 * 密文格式：{@code ENCv1:<keyId>:<base64url iv>:<base64url ciphertext>}。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class AesGcmFieldEncryptor implements FieldEncryptor {

    private static final String PREFIX = "ENCv1:";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final KeyProvider keyProvider;
    private final SecureRandom secureRandom;

    public AesGcmFieldEncryptor(KeyProvider keyProvider) {
        this(keyProvider, new SecureRandom());
    }

    public AesGcmFieldEncryptor(KeyProvider keyProvider, SecureRandom secureRandom) {
        this.keyProvider = Objects.requireNonNull(keyProvider, "keyProvider must not be null");
        this.secureRandom = secureRandom == null ? new SecureRandom() : secureRandom;
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        EncryptionKey key = keyProvider.currentKey();
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key.getSecretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX
                    + key.getKeyId()
                    + ':'
                    + encode(iv)
                    + ':'
                    + encode(encrypted);
        } catch (GeneralSecurityException ex) {
            throw new FieldEncryptionException("Failed to encrypt field value", ex);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
            return ciphertext;
        }
        String[] parts = ciphertext.split(":", 4);
        if (parts.length != 4) {
            throw new FieldEncryptionException("Invalid encrypted field format");
        }
        EncryptionKey key = keyProvider.findKey(parts[1])
                .orElseThrow(() -> new FieldEncryptionException("Encryption key not found: " + parts[1]));
        try {
            byte[] iv = decode(parts[2]);
            byte[] encrypted = decode(parts[3]);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key.getSecretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new FieldEncryptionException("Failed to decrypt field value", ex);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
