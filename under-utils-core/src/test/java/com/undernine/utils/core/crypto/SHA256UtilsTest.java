package com.undernine.utils.core.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * SHA256Utils 测试类
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
class SHA256UtilsTest {

    // ==================== sha256() 测试 ====================

    @Test
    void testSha256() {
        // 标准测试向量
        assertThat(SHA256Utils.sha256(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(SHA256Utils.sha256("hello world"))
                .isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
        assertThat(SHA256Utils.sha256("The quick brown fox jumps over the lazy dog"))
                .isEqualTo("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592");
    }

    @Test
    void testSha256_chinese() {
        String text = "你好世界";
        String sha256 = SHA256Utils.sha256(text);
        assertThat(sha256).isNotNull();
        assertThat(sha256).hasSize(64); // SHA-256 是 256 位，即 64 个十六进制字符
        assertThat(sha256).matches("^[0-9a-f]{64}$");
    }

    @Test
    void testSha256_null() {
        assertThat(SHA256Utils.sha256((String) null)).isNull();
    }

    @Test
    void testSha256_empty() {
        assertThat(SHA256Utils.sha256("")).isNotNull();
    }

    // ==================== sha256Upper() 测试 ====================

    @Test
    void testSha256Upper() {
        String text = "hello world";
        String sha256Upper = SHA256Utils.sha256Upper(text);
        String sha256Lower = SHA256Utils.sha256(text);

        assertThat(sha256Upper).isEqualTo(sha256Lower.toUpperCase());
        assertThat(sha256Upper).isEqualTo("B94D27B9934D3E08A52E52D7DA7DABFAC484EFE37A5380EE9088F7ACE2EFCDE9");
    }

    @Test
    void testSha256Upper_null() {
        assertThat(SHA256Utils.sha256Upper(null)).isNull();
    }

    // ==================== sha256(byte[]) 测试 ====================

    @Test
    void testSha256Bytes() {
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        String sha256 = SHA256Utils.sha256(bytes);

        assertThat(sha256).isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
    }

    @Test
    void testSha256Bytes_null() {
        assertThat(SHA256Utils.sha256((byte[]) null)).isNull();
    }

    @Test
    void testSha256Bytes_empty() {
        // 空字节数组的 SHA-256 值
        assertThat(SHA256Utils.sha256(new byte[0])).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    // ==================== sha256Bytes() 测试 ====================

    @Test
    void testSha256BytesReturnBytes() {
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] sha256Bytes = SHA256Utils.sha256Bytes(bytes);

        assertThat(sha256Bytes).isNotNull();
        assertThat(sha256Bytes).hasSize(32); // SHA-256 是 256 位，即 32 字节
    }

    @Test
    void testSha256BytesReturnBytes_null() {
        assertThat(SHA256Utils.sha256Bytes(null)).isNull();
    }

    // ==================== sha256WithSalt() 测试 ====================

    @Test
    void testSha256WithSalt() {
        String text = "password";
        String salt = "randomSalt";

        String sha256WithSalt = SHA256Utils.sha256WithSalt(text, salt);
        String expected = SHA256Utils.sha256(text + salt);

        assertThat(sha256WithSalt).isEqualTo(expected);
        assertThat(sha256WithSalt).isNotEqualTo(SHA256Utils.sha256(text)); // 加盐后不同
    }

    @Test
    void testSha256WithSalt_differentSalts() {
        String text = "password";
        String salt1 = "salt1";
        String salt2 = "salt2";

        String sha256_1 = SHA256Utils.sha256WithSalt(text, salt1);
        String sha256_2 = SHA256Utils.sha256WithSalt(text, salt2);

        assertThat(sha256_1).isNotEqualTo(sha256_2); // 不同的盐产生不同的结果
    }

    @Test
    void testSha256WithSalt_null() {
        assertThat(SHA256Utils.sha256WithSalt(null, "salt")).isNull();
        assertThat(SHA256Utils.sha256WithSalt("text", null)).isNull();
        assertThat(SHA256Utils.sha256WithSalt(null, null)).isNull();
    }

    @Test
    void testSha256WithSalt_empty() {
        assertThat(SHA256Utils.sha256WithSalt("", "salt")).isNull();
        assertThat(SHA256Utils.sha256WithSalt("text", "")).isNull();
    }

    // ==================== verify() 测试 ====================

    @Test
    void testVerify_valid() {
        String text = "hello world";
        String sha256 = SHA256Utils.sha256(text);

        assertThat(SHA256Utils.verify(text, sha256)).isTrue();
    }

    @Test
    void testVerify_invalid() {
        String text = "hello world";
        String wrongSha256 = "wrongsha256value";

        assertThat(SHA256Utils.verify(text, wrongSha256)).isFalse();
    }

    @Test
    void testVerify_caseInsensitive() {
        String text = "hello world";
        String sha256Lower = SHA256Utils.sha256(text);
        String sha256Upper = sha256Lower.toUpperCase();

        assertThat(SHA256Utils.verify(text, sha256Lower)).isTrue();
        assertThat(SHA256Utils.verify(text, sha256Upper)).isTrue();
    }

    @Test
    void testVerify_null() {
        assertThat(SHA256Utils.verify(null, "sha256")).isFalse();
        assertThat(SHA256Utils.verify("text", null)).isFalse();
        assertThat(SHA256Utils.verify(null, null)).isFalse();
    }

    // ==================== verifyWithSalt() 测试 ====================

    @Test
    void testVerifyWithSalt_valid() {
        String text = "password";
        String salt = "randomSalt";
        String sha256 = SHA256Utils.sha256WithSalt(text, salt);

        assertThat(SHA256Utils.verifyWithSalt(text, salt, sha256)).isTrue();
    }

    @Test
    void testVerifyWithSalt_invalid() {
        String text = "password";
        String salt = "randomSalt";
        String wrongSha256 = "wrongsha256value";

        assertThat(SHA256Utils.verifyWithSalt(text, salt, wrongSha256)).isFalse();
    }

    @Test
    void testVerifyWithSalt_wrongSalt() {
        String text = "password";
        String salt1 = "salt1";
        String salt2 = "salt2";
        String sha256 = SHA256Utils.sha256WithSalt(text, salt1);

        assertThat(SHA256Utils.verifyWithSalt(text, salt2, sha256)).isFalse();
    }

    @Test
    void testVerifyWithSalt_null() {
        assertThat(SHA256Utils.verifyWithSalt(null, "salt", "sha256")).isFalse();
        assertThat(SHA256Utils.verifyWithSalt("text", null, "sha256")).isFalse();
        assertThat(SHA256Utils.verifyWithSalt("text", "salt", null)).isFalse();
    }

    // ==================== sha256Multiple() 测试 ====================

    @Test
    void testSha256Multiple() {
        String text = "password";
        int times = 3;

        String result = SHA256Utils.sha256Multiple(text, times);

        // 手动验证
        String expected = SHA256Utils.sha256(text);
        expected = SHA256Utils.sha256(expected);
        expected = SHA256Utils.sha256(expected);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testSha256Multiple_once() {
        String text = "password";
        String single = SHA256Utils.sha256(text);
        String multiple = SHA256Utils.sha256Multiple(text, 1);

        assertThat(multiple).isEqualTo(single);
    }

    @Test
    void testSha256Multiple_differentTimes() {
        String text = "password";

        String hash1 = SHA256Utils.sha256Multiple(text, 1);
        String hash2 = SHA256Utils.sha256Multiple(text, 2);
        String hash3 = SHA256Utils.sha256Multiple(text, 3);

        // 不同次数产生不同结果
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash2).isNotEqualTo(hash3);
        assertThat(hash1).isNotEqualTo(hash3);
    }

    @Test
    void testSha256Multiple_invalidTimes() {
        assertThatThrownBy(() -> SHA256Utils.sha256Multiple("text", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SHA256Utils.sha256Multiple("text", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSha256Multiple_null() {
        assertThat(SHA256Utils.sha256Multiple(null, 1)).isNull();
    }

    // ==================== 综合测试 ====================

    @Test
    void testConsistency() {
        String text = "test data";

        // 多次计算应该得到相同结果
        String sha256_1 = SHA256Utils.sha256(text);
        String sha256_2 = SHA256Utils.sha256(text);
        assertThat(sha256_1).isEqualTo(sha256_2);
    }

    @Test
    void testDifferentInputs() {
        String text1 = "hello";
        String text2 = "world";

        String sha256_1 = SHA256Utils.sha256(text1);
        String sha256_2 = SHA256Utils.sha256(text2);

        assertThat(sha256_1).isNotEqualTo(sha256_2); // 不同输入产生不同结果
    }

    @Test
    void testLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("test");
        }
        String longText = sb.toString();

        String sha256 = SHA256Utils.sha256(longText);
        assertThat(sha256).isNotNull();
        assertThat(sha256).hasSize(64);
    }

    @Test
    void testSha256VsMd5() {
        String text = "hello world";

        String sha256 = SHA256Utils.sha256(text);
        String md5 = MD5Utils.md5(text);

        // SHA-256 是 64 位十六进制，MD5 是 32 位
        assertThat(sha256).hasSize(64);
        assertThat(md5).hasSize(32);
        assertThat(sha256).isNotEqualTo(md5);
    }
}
