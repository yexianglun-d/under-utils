package com.undernine.utils.core.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * AESUtils 测试类
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
class AESUtilsTest {

    // ==================== generateKey() 测试 ====================

    @Test
    void testGenerateKey_default() {
        String key = AESUtils.generateKey();

        assertThat(key).isNotNull();
        assertThat(key).isNotEmpty();
        // 128 位密钥 Base64 编码后的长度
        assertThat(key).hasSize(24);
    }

    @Test
    void testGenerateKey_128() {
        String key = AESUtils.generateKey(128);
        assertThat(key).isNotNull();
        assertThat(key).hasSize(24);
    }

    @Test
    void testGenerateKey_192() {
        String key = AESUtils.generateKey(192);
        assertThat(key).isNotNull();
        // 192 位密钥 Base64 编码后的长度
        assertThat(key.length()).isGreaterThan(24);
    }

    @Test
    void testGenerateKey_256() {
        String key = AESUtils.generateKey(256);
        assertThat(key).isNotNull();
        // 256 位密钥 Base64 编码后的长度
        assertThat(key.length()).isGreaterThan(32);
    }

    @Test
    void testGenerateKey_uniqueness() {
        String key1 = AESUtils.generateKey();
        String key2 = AESUtils.generateKey();

        assertThat(key1).isNotEqualTo(key2);
    }

    // ==================== generateIV() 测试 ====================

    @Test
    void testGenerateIV() {
        String iv = AESUtils.generateIV();

        assertThat(iv).isNotNull();
        assertThat(iv).isNotEmpty();
        // 16 字节 IV Base64 编码后的长度
        assertThat(iv).hasSize(24);
    }

    @Test
    void testGenerateIV_uniqueness() {
        String iv1 = AESUtils.generateIV();
        String iv2 = AESUtils.generateIV();

        assertThat(iv1).isNotEqualTo(iv2);
    }

    // ==================== encrypt() / decrypt() 测试 ====================

    @Test
    void testEncryptDecrypt() {
        String plainText = "hello world";
        String key = AESUtils.generateKey();
        String iv = AESUtils.generateIV();

        // 加密
        String encrypted = AESUtils.encrypt(plainText, key, iv);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plainText);

        // 解密
        String decrypted = AESUtils.decrypt(encrypted, key, iv);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void testEncryptDecrypt_chinese() {
        String plainText = "你好世界";
        String key = AESUtils.generateKey();
        String iv = AESUtils.generateIV();

        String encrypted = AESUtils.encrypt(plainText, key, iv);
        String decrypted = AESUtils.decrypt(encrypted, key, iv);

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void testEncryptDecrypt_longText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("test");
        }
        String plainText = sb.toString();
        String key = AESUtils.generateKey();
        String iv = AESUtils.generateIV();

        String encrypted = AESUtils.encrypt(plainText, key, iv);
        String decrypted = AESUtils.decrypt(encrypted, key, iv);

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void testEncrypt_nullPlainText() {
        assertThatThrownBy(() -> AESUtils.encrypt(null, AESUtils.generateKey(), AESUtils.generateIV()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEncrypt_nullKey() {
        assertThatThrownBy(() -> AESUtils.encrypt("text", null, AESUtils.generateIV()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEncrypt_nullIV() {
        assertThatThrownBy(() -> AESUtils.encrypt("text", AESUtils.generateKey(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testDecrypt_wrongKey() {
        String plainText = "hello world";
        String key1 = AESUtils.generateKey();
        String key2 = AESUtils.generateKey();
        String iv = AESUtils.generateIV();

        String encrypted = AESUtils.encrypt(plainText, key1, iv);

        assertDecryptFailsOrDiffers(encrypted, key2, iv, plainText);
    }

    @Test
    void testDecrypt_wrongIV() {
        String plainText = "hello world";
        String key = AESUtils.generateKey();
        String iv1 = AESUtils.generateIV();
        String iv2 = AESUtils.generateIV();

        String encrypted = AESUtils.encrypt(plainText, key, iv1);

        assertDecryptFailsOrDiffers(encrypted, key, iv2, plainText);
    }

    // ==================== ECB 模式测试 ====================

    @Test
    void testEncryptDecryptECB() {
        String plainText = "hello world";
        String key = AESUtils.generateKey();

        // 加密
        String encrypted = AESUtils.encryptECB(plainText, key);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plainText);

        // 解密
        String decrypted = AESUtils.decryptECB(encrypted, key);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void testEncryptECB_sameInput() {
        String plainText = "hello world";
        String key = AESUtils.generateKey();

        String encrypted1 = AESUtils.encryptECB(plainText, key);
        String encrypted2 = AESUtils.encryptECB(plainText, key);

        // ECB 模式下，相同输入产生相同输出（这也是为什么不推荐使用）
        assertThat(encrypted1).isEqualTo(encrypted2);
    }

    @Test
    void testEncryptCBC_sameInput() {
        String plainText = "hello world";
        String key = AESUtils.generateKey();
        String iv1 = AESUtils.generateIV();
        String iv2 = AESUtils.generateIV();

        String encrypted1 = AESUtils.encrypt(plainText, key, iv1);
        String encrypted2 = AESUtils.encrypt(plainText, key, iv2);

        // CBC 模式下，不同 IV 产生不同输出
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    // ==================== 字节数组测试 ====================

    @Test
    void testEncryptDecryptBytes() {
        byte[] plainBytes = "hello world".getBytes(StandardCharsets.UTF_8);
        String key = AESUtils.generateKey();
        String iv = AESUtils.generateIV();

        // 加密
        byte[] encrypted = AESUtils.encryptBytes(plainBytes, key, iv);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plainBytes);

        // 解密
        byte[] decrypted = AESUtils.decryptBytes(encrypted, key, iv);
        assertThat(decrypted).isEqualTo(plainBytes);
    }

    @Test
    void testEncryptBytes_null() {
        assertThatThrownBy(() -> AESUtils.encryptBytes(null, AESUtils.generateKey(), AESUtils.generateIV()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== 综合测试 ====================

    @Test
    void testMultipleEncryptDecrypt() {
        String key = AESUtils.generateKey();
        String iv = AESUtils.generateIV();

        String[] testTexts = {
                "hello",
                "hello world",
                "The quick brown fox jumps over the lazy dog",
                "你好世界",
                "123456",
                ""
        };

        for (String text : testTexts) {
            String encrypted = AESUtils.encrypt(text, key, iv);
            String decrypted = AESUtils.decrypt(encrypted, key, iv);
            assertThat(decrypted).isEqualTo(text);
        }
    }

    @Test
    void testPerformance() {
        String plainText = "test data";
        String key = AESUtils.generateKey();
        String iv = AESUtils.generateIV();

        int count = 10000;
        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            String encrypted = AESUtils.encrypt(plainText, key, iv);
            AESUtils.decrypt(encrypted, key, iv);
        }

        long duration = System.currentTimeMillis() - start;
        System.out.println("AES 加解密 " + count + " 次耗时: " + duration + "ms");

        assertThat(duration).isLessThan(5000);
    }

    private static void assertDecryptFailsOrDiffers(String cipherText, String key, String iv, String plainText) {
        try {
            assertThat(AESUtils.decrypt(cipherText, key, iv)).isNotEqualTo(plainText);
        } catch (RuntimeException expected) {
            assertThat(expected).hasMessageContaining("AES decryption failed");
        }
    }
}
