package com.undernine.utils.core.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ValidationUtils 测试类
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
class ValidationUtilsTest {

    // ==================== isPhone() 测试 ====================

    @Test
    void testIsPhone_valid() {
        assertThat(ValidationUtils.isPhone("13812345678")).isTrue();
        assertThat(ValidationUtils.isPhone("13000000000")).isTrue();
        assertThat(ValidationUtils.isPhone("14812345678")).isTrue();
        assertThat(ValidationUtils.isPhone("15812345678")).isTrue();
        assertThat(ValidationUtils.isPhone("16812345678")).isTrue();
        assertThat(ValidationUtils.isPhone("17812345678")).isTrue();
        assertThat(ValidationUtils.isPhone("18812345678")).isTrue();
        assertThat(ValidationUtils.isPhone("19812345678")).isTrue();
    }

    @Test
    void testIsPhone_invalid() {
        assertThat(ValidationUtils.isPhone("12812345678")).isFalse(); // 不支持 12 开头
        assertThat(ValidationUtils.isPhone("1381234567")).isFalse();  // 10 位
        assertThat(ValidationUtils.isPhone("138123456789")).isFalse(); // 12 位
        assertThat(ValidationUtils.isPhone("abc12345678")).isFalse(); // 包含字母
        assertThat(ValidationUtils.isPhone("138-1234-5678")).isFalse(); // 包含分隔符
    }

    @Test
    void testIsPhone_null() {
        assertThat(ValidationUtils.isPhone(null)).isFalse();
    }

    @Test
    void testIsPhone_empty() {
        assertThat(ValidationUtils.isPhone("")).isFalse();
        assertThat(ValidationUtils.isPhone("   ")).isFalse();
    }

    // ==================== isEmail() 测试 ====================

    @Test
    void testIsEmail_valid() {
        assertThat(ValidationUtils.isEmail("user@example.com")).isTrue();
        assertThat(ValidationUtils.isEmail("test.user@example.com")).isTrue();
        assertThat(ValidationUtils.isEmail("user+tag@example.co.uk")).isTrue();
        assertThat(ValidationUtils.isEmail("user_name@example-domain.com")).isTrue();
        assertThat(ValidationUtils.isEmail("123@example.com")).isTrue();
    }

    @Test
    void testIsEmail_invalid() {
        assertThat(ValidationUtils.isEmail("invalid-email")).isFalse();
        assertThat(ValidationUtils.isEmail("@example.com")).isFalse();
        assertThat(ValidationUtils.isEmail("user@")).isFalse();
        assertThat(ValidationUtils.isEmail("user@.com")).isFalse();
        assertThat(ValidationUtils.isEmail("user example@test.com")).isFalse(); // 包含空格
    }

    @Test
    void testIsEmail_null() {
        assertThat(ValidationUtils.isEmail(null)).isFalse();
    }

    @Test
    void testIsEmail_empty() {
        assertThat(ValidationUtils.isEmail("")).isFalse();
        assertThat(ValidationUtils.isEmail("   ")).isFalse();
    }

    // ==================== isIdCard() 测试 ====================

    @Test
    void testIsIdCard_valid() {
        // 这些是格式正确且校验位正确的身份证号（可能是虚构的）
        assertThat(ValidationUtils.isIdCard("110101199003074792")).isTrue();
        assertThat(ValidationUtils.isIdCard("31010519900307869X")).isTrue();
    }

    @Test
    void testIsIdCard_invalidFormat() {
        assertThat(ValidationUtils.isIdCard("12345678901234567")).isFalse();  // 17 位
        assertThat(ValidationUtils.isIdCard("1234567890123456789")).isFalse(); // 19 位
        assertThat(ValidationUtils.isIdCard("11010119900307479a")).isFalse(); // 非法字符
        assertThat(ValidationUtils.isIdCard("010101199003074796")).isFalse(); // 地区码错误
    }

    @Test
    void testIsIdCard_invalidDate() {
        assertThat(ValidationUtils.isIdCard("110101199013074796")).isFalse(); // 月份 13
        assertThat(ValidationUtils.isIdCard("110101199002304796")).isFalse(); // 日期 30（2月）
    }

    @Test
    void testIsIdCard_null() {
        assertThat(ValidationUtils.isIdCard(null)).isFalse();
    }

    @Test
    void testIsIdCard_empty() {
        assertThat(ValidationUtils.isIdCard("")).isFalse();
        assertThat(ValidationUtils.isIdCard("   ")).isFalse();
    }

    // ==================== isUrl() 测试 ====================

    @Test
    void testIsUrl_valid() {
        assertThat(ValidationUtils.isUrl("http://www.example.com")).isTrue();
        assertThat(ValidationUtils.isUrl("https://www.example.com")).isTrue();
        assertThat(ValidationUtils.isUrl("https://example.com/path/to/page")).isTrue();
        assertThat(ValidationUtils.isUrl("https://example.com:8080/path")).isTrue();
        assertThat(ValidationUtils.isUrl("https://example.com?query=value")).isTrue();
        assertThat(ValidationUtils.isUrl("ftp://ftp.example.com/file.txt")).isTrue();
    }

    @Test
    void testIsUrl_invalid() {
        assertThat(ValidationUtils.isUrl("not a url")).isFalse();
        assertThat(ValidationUtils.isUrl("www.example.com")).isFalse(); // 缺少协议
        assertThat(ValidationUtils.isUrl("http://")).isFalse();
        assertThat(ValidationUtils.isUrl("://example.com")).isFalse();
    }

    @Test
    void testIsUrl_null() {
        assertThat(ValidationUtils.isUrl(null)).isFalse();
    }

    @Test
    void testIsUrl_empty() {
        assertThat(ValidationUtils.isUrl("")).isFalse();
        assertThat(ValidationUtils.isUrl("   ")).isFalse();
    }

    // ==================== isIpv4() 测试 ====================

    @Test
    void testIsIpv4_valid() {
        assertThat(ValidationUtils.isIpv4("192.168.1.1")).isTrue();
        assertThat(ValidationUtils.isIpv4("127.0.0.1")).isTrue();
        assertThat(ValidationUtils.isIpv4("0.0.0.0")).isTrue();
        assertThat(ValidationUtils.isIpv4("255.255.255.255")).isTrue();
        assertThat(ValidationUtils.isIpv4("10.0.0.1")).isTrue();
    }

    @Test
    void testIsIpv4_invalid() {
        assertThat(ValidationUtils.isIpv4("256.1.1.1")).isFalse(); // 超出范围
        assertThat(ValidationUtils.isIpv4("192.168.1")).isFalse();  // 缺少段
        assertThat(ValidationUtils.isIpv4("192.168.1.1.1")).isFalse(); // 多余段
        assertThat(ValidationUtils.isIpv4("abc.def.ghi.jkl")).isFalse(); // 非数字
    }

    @Test
    void testIsIpv4_null() {
        assertThat(ValidationUtils.isIpv4(null)).isFalse();
    }

    @Test
    void testIsIpv4_empty() {
        assertThat(ValidationUtils.isIpv4("")).isFalse();
        assertThat(ValidationUtils.isIpv4("   ")).isFalse();
    }

    // ==================== isChinese() 测试 ====================

    @Test
    void testIsChinese_valid() {
        assertThat(ValidationUtils.isChinese("中文")).isTrue();
        assertThat(ValidationUtils.isChinese("测试")).isTrue();
        assertThat(ValidationUtils.isChinese("你好世界")).isTrue();
    }

    @Test
    void testIsChinese_invalid() {
        assertThat(ValidationUtils.isChinese("中文abc")).isFalse();
        assertThat(ValidationUtils.isChinese("abc")).isFalse();
        assertThat(ValidationUtils.isChinese("123")).isFalse();
        assertThat(ValidationUtils.isChinese("中文123")).isFalse();
    }

    @Test
    void testIsChinese_null() {
        assertThat(ValidationUtils.isChinese(null)).isFalse();
    }

    @Test
    void testIsChinese_empty() {
        assertThat(ValidationUtils.isChinese("")).isFalse();
        assertThat(ValidationUtils.isChinese("   ")).isFalse();
    }

    // ==================== containsChinese() 测试 ====================

    @Test
    void testContainsChinese_true() {
        assertThat(ValidationUtils.containsChinese("中文")).isTrue();
        assertThat(ValidationUtils.containsChinese("abc中文")).isTrue();
        assertThat(ValidationUtils.containsChinese("123测试456")).isTrue();
        assertThat(ValidationUtils.containsChinese("hello世界")).isTrue();
    }

    @Test
    void testContainsChinese_false() {
        assertThat(ValidationUtils.containsChinese("abc")).isFalse();
        assertThat(ValidationUtils.containsChinese("123")).isFalse();
        assertThat(ValidationUtils.containsChinese("hello world")).isFalse();
    }

    @Test
    void testContainsChinese_null() {
        assertThat(ValidationUtils.containsChinese(null)).isFalse();
    }

    @Test
    void testContainsChinese_empty() {
        assertThat(ValidationUtils.containsChinese("")).isFalse();
    }

    // ==================== isInteger() 测试 ====================

    @Test
    void testIsInteger_valid() {
        assertThat(ValidationUtils.isInteger("123")).isTrue();
        assertThat(ValidationUtils.isInteger("0")).isTrue();
        assertThat(ValidationUtils.isInteger("-456")).isTrue();
        assertThat(ValidationUtils.isInteger("999999999")).isTrue();
    }

    @Test
    void testIsInteger_invalid() {
        assertThat(ValidationUtils.isInteger("123.45")).isFalse();
        assertThat(ValidationUtils.isInteger("abc")).isFalse();
        assertThat(ValidationUtils.isInteger("12.0")).isFalse();
        assertThat(ValidationUtils.isInteger("1,234")).isFalse();
    }

    @Test
    void testIsInteger_null() {
        assertThat(ValidationUtils.isInteger(null)).isFalse();
    }

    @Test
    void testIsInteger_empty() {
        assertThat(ValidationUtils.isInteger("")).isFalse();
        assertThat(ValidationUtils.isInteger("   ")).isFalse();
    }

    // ==================== isDecimal() 测试 ====================

    @Test
    void testIsDecimal_valid() {
        assertThat(ValidationUtils.isDecimal("123.45")).isTrue();
        assertThat(ValidationUtils.isDecimal("0.1")).isTrue();
        assertThat(ValidationUtils.isDecimal("-67.89")).isTrue();
        assertThat(ValidationUtils.isDecimal("999.999")).isTrue();
    }

    @Test
    void testIsDecimal_invalid() {
        assertThat(ValidationUtils.isDecimal("123")).isFalse(); // 整数不是小数
        assertThat(ValidationUtils.isDecimal("abc")).isFalse();
        assertThat(ValidationUtils.isDecimal(".5")).isFalse(); // 缺少整数部分
        assertThat(ValidationUtils.isDecimal("5.")).isFalse(); // 缺少小数部分
    }

    @Test
    void testIsDecimal_null() {
        assertThat(ValidationUtils.isDecimal(null)).isFalse();
    }

    @Test
    void testIsDecimal_empty() {
        assertThat(ValidationUtils.isDecimal("")).isFalse();
        assertThat(ValidationUtils.isDecimal("   ")).isFalse();
    }

    // ==================== isNumber() 测试 ====================

    @Test
    void testIsNumber_valid() {
        assertThat(ValidationUtils.isNumber("123")).isTrue();
        assertThat(ValidationUtils.isNumber("123.45")).isTrue();
        assertThat(ValidationUtils.isNumber("0")).isTrue();
        assertThat(ValidationUtils.isNumber("0.0")).isTrue();
        assertThat(ValidationUtils.isNumber("-456")).isTrue();
        assertThat(ValidationUtils.isNumber("-67.89")).isTrue();
    }

    @Test
    void testIsNumber_invalid() {
        assertThat(ValidationUtils.isNumber("abc")).isFalse();
        assertThat(ValidationUtils.isNumber("12.34.56")).isFalse();
        assertThat(ValidationUtils.isNumber("1,234")).isFalse();
    }

    @Test
    void testIsNumber_null() {
        assertThat(ValidationUtils.isNumber(null)).isFalse();
    }

    @Test
    void testIsNumber_empty() {
        assertThat(ValidationUtils.isNumber("")).isFalse();
        assertThat(ValidationUtils.isNumber("   ")).isFalse();
    }

    // ==================== isInRange() 测试 ====================

    @Test
    void testIsInRange_valid() {
        assertThat(ValidationUtils.isInRange(50, 1, 100)).isTrue();
        assertThat(ValidationUtils.isInRange(1, 1, 100)).isTrue();  // 边界
        assertThat(ValidationUtils.isInRange(100, 1, 100)).isTrue(); // 边界
        assertThat(ValidationUtils.isInRange(50.5, 1, 100)).isTrue();
        assertThat(ValidationUtils.isInRange(-5, -10, 10)).isTrue();
    }

    @Test
    void testIsInRange_invalid() {
        assertThat(ValidationUtils.isInRange(0, 1, 100)).isFalse();
        assertThat(ValidationUtils.isInRange(101, 1, 100)).isFalse();
        assertThat(ValidationUtils.isInRange(150, 1, 100)).isFalse();
    }

    @Test
    void testIsInRange_null() {
        assertThat(ValidationUtils.isInRange(null, 1, 100)).isFalse();
        assertThat(ValidationUtils.isInRange(50, null, 100)).isFalse();
        assertThat(ValidationUtils.isInRange(50, 1, null)).isFalse();
    }

    // ==================== isLengthInRange() 测试 ====================

    @Test
    void testIsLengthInRange_valid() {
        assertThat(ValidationUtils.isLengthInRange("hello", 1, 10)).isTrue();
        assertThat(ValidationUtils.isLengthInRange("a", 1, 10)).isTrue(); // 边界
        assertThat(ValidationUtils.isLengthInRange("abcdefghij", 1, 10)).isTrue(); // 边界
        assertThat(ValidationUtils.isLengthInRange("", 0, 10)).isTrue();
    }

    @Test
    void testIsLengthInRange_invalid() {
        assertThat(ValidationUtils.isLengthInRange("hello", 1, 3)).isFalse();
        assertThat(ValidationUtils.isLengthInRange("", 1, 10)).isFalse();
        assertThat(ValidationUtils.isLengthInRange("too long string", 1, 5)).isFalse();
    }

    @Test
    void testIsLengthInRange_null() {
        assertThat(ValidationUtils.isLengthInRange(null, 1, 10)).isFalse();
    }

    // ==================== isAlpha() 测试 ====================

    @Test
    void testIsAlpha_valid() {
        assertThat(ValidationUtils.isAlpha("abc")).isTrue();
        assertThat(ValidationUtils.isAlpha("ABC")).isTrue();
        assertThat(ValidationUtils.isAlpha("abcABC")).isTrue();
    }

    @Test
    void testIsAlpha_invalid() {
        assertThat(ValidationUtils.isAlpha("abc123")).isFalse();
        assertThat(ValidationUtils.isAlpha("abc-def")).isFalse();
        assertThat(ValidationUtils.isAlpha("abc def")).isFalse();
        assertThat(ValidationUtils.isAlpha("123")).isFalse();
    }

    @Test
    void testIsAlpha_null() {
        assertThat(ValidationUtils.isAlpha(null)).isFalse();
    }

    @Test
    void testIsAlpha_empty() {
        assertThat(ValidationUtils.isAlpha("")).isFalse();
    }

    // ==================== isAlphanumeric() 测试 ====================

    @Test
    void testIsAlphanumeric_valid() {
        assertThat(ValidationUtils.isAlphanumeric("abc123")).isTrue();
        assertThat(ValidationUtils.isAlphanumeric("ABC123")).isTrue();
        assertThat(ValidationUtils.isAlphanumeric("abc")).isTrue();
        assertThat(ValidationUtils.isAlphanumeric("123")).isTrue();
    }

    @Test
    void testIsAlphanumeric_invalid() {
        assertThat(ValidationUtils.isAlphanumeric("abc-123")).isFalse();
        assertThat(ValidationUtils.isAlphanumeric("abc 123")).isFalse();
        assertThat(ValidationUtils.isAlphanumeric("abc_123")).isFalse();
    }

    @Test
    void testIsAlphanumeric_null() {
        assertThat(ValidationUtils.isAlphanumeric(null)).isFalse();
    }

    @Test
    void testIsAlphanumeric_empty() {
        assertThat(ValidationUtils.isAlphanumeric("")).isFalse();
    }

    // ==================== matches() 测试 ====================

    @Test
    void testMatches_valid() {
        assertThat(ValidationUtils.matches("abc123", "^[a-z0-9]+$")).isTrue();
        assertThat(ValidationUtils.matches("hello", "^[a-z]+$")).isTrue();
        assertThat(ValidationUtils.matches("123", "^\\d+$")).isTrue();
    }

    @Test
    void testMatches_invalid() {
        assertThat(ValidationUtils.matches("abc123", "^[a-z]+$")).isFalse();
        assertThat(ValidationUtils.matches("ABC", "^[a-z]+$")).isFalse();
    }

    @Test
    void testMatches_null() {
        assertThat(ValidationUtils.matches(null, "^[a-z]+$")).isFalse();
        assertThat(ValidationUtils.matches("abc", null)).isFalse();
        assertThat(ValidationUtils.matches(null, null)).isFalse();
    }

    // ==================== 综合测试 ====================

    @Test
    void testMultipleValidations() {
        String phone = "13812345678";
        assertThat(ValidationUtils.isPhone(phone)).isTrue();
        assertThat(ValidationUtils.isInteger(phone)).isTrue(); // 手机号也是数字
        assertThat(ValidationUtils.isAlphanumeric(phone)).isTrue();
        assertThat(ValidationUtils.isLengthInRange(phone, 11, 11)).isTrue();
    }
}
