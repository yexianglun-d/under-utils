package com.undernine.utils.security.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmFieldEncryptorTest {

    private final AesGcmFieldEncryptor encryptor = new AesGcmFieldEncryptor(
            StaticKeyProvider.ofBase64("k1", base64Key())
    );

    @Test
    void shouldEncryptAndDecryptFieldValue() {
        String ciphertext = encryptor.encrypt("13812345678");

        assertThat(ciphertext).startsWith("ENCv1:k1:");
        assertThat(ciphertext).doesNotContain("13812345678");
        assertThat(encryptor.decrypt(ciphertext)).isEqualTo("13812345678");
    }

    @Test
    void shouldUseRandomIvForSamePlaintext() {
        String first = encryptor.encrypt("same-value");
        String second = encryptor.encrypt("same-value");

        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.decrypt(first)).isEqualTo("same-value");
        assertThat(encryptor.decrypt(second)).isEqualTo("same-value");
    }

    @Test
    void shouldReturnPlainValueWhenValueIsNotEncryptedEnvelope() {
        assertThat(encryptor.decrypt("legacy-plaintext")).isEqualTo("legacy-plaintext");
    }

    @Test
    void shouldFailForInvalidEncryptedEnvelope() {
        assertThatThrownBy(() -> encryptor.decrypt("ENCv1:k1:bad:bad"))
                .isInstanceOf(FieldEncryptionException.class)
                .hasMessage("Failed to decrypt field value");
    }

    private static String base64Key() {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(key);
    }
}
