package com.undernine.utils.core.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * MD5Utils 测试类
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
class MD5UtilsTest {

    // ==================== md5() 测试 ====================

    @Test
    void testMd5() {
        // 标准测试向量
        assertThat(MD5Utils.md5("")).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
        assertThat(MD5Utils.md5("hello world")).isEqualTo("5eb63bbbe01eeed093cb22bb8f5acdc3");
        assertThat(MD5Utils.md5("The quick brown fox jumps over the lazy dog"))
                .isEqualTo("9e107d9d372bb6826bd81d3542a419d6");
    }

    @Test
    void testMd5_chinese() {
        String text = "你好世界";
        String md5 = MD5Utils.md5(text);
        assertThat(md5).isNotNull();
        assertThat(md5).hasSize(32);
        assertThat(md5).matches("^[0-9a-f]{32}$");
    }

    @Test
    void testMd5_null() {
        assertThat(MD5Utils.md5((String) null)).isNull();
    }

    @Test
    void testMd5_empty() {
        assertThat(MD5Utils.md5("")).isNotNull();
    }

    // ==================== md5Upper() 测试 ====================

    @Test
    void testMd5Upper() {
        String text = "hello world";
        String md5Upper = MD5Utils.md5Upper(text);
        String md5Lower = MD5Utils.md5(text);

        assertThat(md5Upper).isEqualTo(md5Lower.toUpperCase());
        assertThat(md5Upper).isEqualTo("5EB63BBBE01EEED093CB22BB8F5ACDC3");
    }

    @Test
    void testMd5Upper_null() {
        assertThat(MD5Utils.md5Upper(null)).isNull();
    }

    // ==================== md5Short() 测试 ====================

    @Test
    void testMd5Short() {
        String text = "hello world";
        String md5Full = MD5Utils.md5(text); // 5eb63bbbe01eeed093cb22bb8f5acdc3
        String md5Short = MD5Utils.md5Short(text);

        assertThat(md5Short).hasSize(16);
        assertThat(md5Short).isEqualTo(md5Full.substring(8, 24));
        assertThat(md5Short).isEqualTo("e01eeed093cb22bb");
    }

    @Test
    void testMd5Short_null() {
        assertThat(MD5Utils.md5Short(null)).isNull();
    }

    // ==================== md5ShortUpper() 测试 ====================

    @Test
    void testMd5ShortUpper() {
        String text = "hello world";
        String md5ShortUpper = MD5Utils.md5ShortUpper(text);
        String md5Short = MD5Utils.md5Short(text);

        assertThat(md5ShortUpper).isEqualTo(md5Short.toUpperCase());
        assertThat(md5ShortUpper).isEqualTo("E01EEED093CB22BB");
    }

    @Test
    void testMd5ShortUpper_null() {
        assertThat(MD5Utils.md5ShortUpper(null)).isNull();
    }

    // ==================== md5(byte[]) 测试 ====================

    @Test
    void testMd5Bytes() {
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        String md5 = MD5Utils.md5(bytes);

        assertThat(md5).isEqualTo("5eb63bbbe01eeed093cb22bb8f5acdc3");
    }

    @Test
    void testMd5Bytes_null() {
        assertThat(MD5Utils.md5((byte[]) null)).isNull();
    }

    @Test
    void testMd5Bytes_empty() {
        // 空字节数组的 MD5 值
        assertThat(MD5Utils.md5(new byte[0])).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
    }

    // ==================== md5Bytes() 测试 ====================

    @Test
    void testMd5BytesReturnBytes() {
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] md5Bytes = MD5Utils.md5Bytes(bytes);

        assertThat(md5Bytes).isNotNull();
        assertThat(md5Bytes).hasSize(16); // MD5 是 128 位，即 16 字节
    }

    @Test
    void testMd5BytesReturnBytes_null() {
        assertThat(MD5Utils.md5Bytes(null)).isNull();
    }

    // ==================== md5WithSalt() 测试 ====================

    @Test
    void testMd5WithSalt() {
        String text = "password";
        String salt = "randomSalt";

        String md5WithSalt = MD5Utils.md5WithSalt(text, salt);
        String expected = MD5Utils.md5(text + salt);

        assertThat(md5WithSalt).isEqualTo(expected);
        assertThat(md5WithSalt).isNotEqualTo(MD5Utils.md5(text)); // 加盐后不同
    }

    @Test
    void testMd5WithSalt_differentSalts() {
        String text = "password";
        String salt1 = "salt1";
        String salt2 = "salt2";

        String md5_1 = MD5Utils.md5WithSalt(text, salt1);
        String md5_2 = MD5Utils.md5WithSalt(text, salt2);

        assertThat(md5_1).isNotEqualTo(md5_2); // 不同的盐产生不同的结果
    }

    @Test
    void testMd5WithSalt_null() {
        assertThat(MD5Utils.md5WithSalt(null, "salt")).isNull();
        assertThat(MD5Utils.md5WithSalt("text", null)).isNull();
        assertThat(MD5Utils.md5WithSalt(null, null)).isNull();
    }

    @Test
    void testMd5WithSalt_empty() {
        assertThat(MD5Utils.md5WithSalt("", "salt")).isNull();
        assertThat(MD5Utils.md5WithSalt("text", "")).isNull();
    }

    // ==================== verify() 测试 ====================

    @Test
    void testVerify_valid() {
        String text = "hello world";
        String md5 = MD5Utils.md5(text);

        assertThat(MD5Utils.verify(text, md5)).isTrue();
    }

    @Test
    void testVerify_invalid() {
        String text = "hello world";
        String wrongMd5 = "wrongmd5value";

        assertThat(MD5Utils.verify(text, wrongMd5)).isFalse();
    }

    @Test
    void testVerify_caseInsensitive() {
        String text = "hello world";
        String md5Lower = MD5Utils.md5(text);
        String md5Upper = md5Lower.toUpperCase();

        assertThat(MD5Utils.verify(text, md5Lower)).isTrue();
        assertThat(MD5Utils.verify(text, md5Upper)).isTrue();
    }

    @Test
    void testVerify_null() {
        assertThat(MD5Utils.verify(null, "md5")).isFalse();
        assertThat(MD5Utils.verify("text", null)).isFalse();
        assertThat(MD5Utils.verify(null, null)).isFalse();
    }

    // ==================== verifyWithSalt() 测试 ====================

    @Test
    void testVerifyWithSalt_valid() {
        String text = "password";
        String salt = "randomSalt";
        String md5 = MD5Utils.md5WithSalt(text, salt);

        assertThat(MD5Utils.verifyWithSalt(text, salt, md5)).isTrue();
    }

    @Test
    void testVerifyWithSalt_invalid() {
        String text = "password";
        String salt = "randomSalt";
        String wrongMd5 = "wrongmd5value";

        assertThat(MD5Utils.verifyWithSalt(text, salt, wrongMd5)).isFalse();
    }

    @Test
    void testVerifyWithSalt_wrongSalt() {
        String text = "password";
        String salt1 = "salt1";
        String salt2 = "salt2";
        String md5 = MD5Utils.md5WithSalt(text, salt1);

        assertThat(MD5Utils.verifyWithSalt(text, salt2, md5)).isFalse();
    }

    @Test
    void testVerifyWithSalt_null() {
        assertThat(MD5Utils.verifyWithSalt(null, "salt", "md5")).isFalse();
        assertThat(MD5Utils.verifyWithSalt("text", null, "md5")).isFalse();
        assertThat(MD5Utils.verifyWithSalt("text", "salt", null)).isFalse();
    }

    // ==================== 综合测试 ====================

    @Test
    void testConsistency() {
        String text = "test data";

        // 多次计算应该得到相同结果
        String md5_1 = MD5Utils.md5(text);
        String md5_2 = MD5Utils.md5(text);
        assertThat(md5_1).isEqualTo(md5_2);
    }

    @Test
    void testDifferentInputs() {
        String text1 = "hello";
        String text2 = "world";

        String md5_1 = MD5Utils.md5(text1);
        String md5_2 = MD5Utils.md5(text2);

        assertThat(md5_1).isNotEqualTo(md5_2); // 不同输入产生不同结果
    }

    @Test
    void testLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("test");
        }
        String longText = sb.toString();

        String md5 = MD5Utils.md5(longText);
        assertThat(md5).isNotNull();
        assertThat(md5).hasSize(32);
    }
}
