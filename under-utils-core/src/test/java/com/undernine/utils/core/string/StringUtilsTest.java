package com.undernine.utils.core.string;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StringUtils 单元测试类
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
class StringUtilsTest {

    @Test
    void testIsEmpty() {
        // 正常场景
        assertThat(StringUtils.isEmpty(null)).isTrue();
        assertThat(StringUtils.isEmpty("")).isTrue();
        assertThat(StringUtils.isEmpty("   ")).isTrue();
        assertThat(StringUtils.isEmpty("hello")).isFalse();
        assertThat(StringUtils.isEmpty("  hello  ")).isFalse();
    }

    @Test
    void testIsNotEmpty() {
        // 正常场景
        assertThat(StringUtils.isNotEmpty("hello")).isTrue();
        assertThat(StringUtils.isNotEmpty("  hello  ")).isTrue();
        assertThat(StringUtils.isNotEmpty(null)).isFalse();
        assertThat(StringUtils.isNotEmpty("")).isFalse();
        assertThat(StringUtils.isNotEmpty("   ")).isFalse();
    }

    @Test
    void testIsBlank() {
        // 正常场景
        assertThat(StringUtils.isBlank(null)).isTrue();
        assertThat(StringUtils.isBlank("")).isTrue();
        assertThat(StringUtils.isBlank(" ")).isTrue();
        assertThat(StringUtils.isBlank("   ")).isTrue();
        assertThat(StringUtils.isBlank("\t")).isTrue();
        assertThat(StringUtils.isBlank("\n")).isTrue();
        assertThat(StringUtils.isBlank("hello")).isFalse();
        assertThat(StringUtils.isBlank("  hello  ")).isFalse();
    }

    @Test
    void testIsNotBlank() {
        // 正常场景
        assertThat(StringUtils.isNotBlank("hello")).isTrue();
        assertThat(StringUtils.isNotBlank("  hello  ")).isTrue();
        assertThat(StringUtils.isNotBlank(null)).isFalse();
        assertThat(StringUtils.isNotBlank("")).isFalse();
        assertThat(StringUtils.isNotBlank("   ")).isFalse();
    }

    @Test
    void testTrim() {
        // 正常场景
        assertThat(StringUtils.trim(null)).isNull();
        assertThat(StringUtils.trim("")).isEmpty();
        assertThat(StringUtils.trim("  ")).isEmpty();
        assertThat(StringUtils.trim("hello")).isEqualTo("hello");
        assertThat(StringUtils.trim("  hello  ")).isEqualTo("hello");
    }

    @Test
    void testTrimToNull() {
        // 正常场景
        assertThat(StringUtils.trimToNull(null)).isNull();
        assertThat(StringUtils.trimToNull("")).isNull();
        assertThat(StringUtils.trimToNull("  ")).isNull();
        assertThat(StringUtils.trimToNull("hello")).isEqualTo("hello");
        assertThat(StringUtils.trimToNull("  hello  ")).isEqualTo("hello");
    }

    @Test
    void testTrimToEmpty() {
        // 正常场景
        assertThat(StringUtils.trimToEmpty(null)).isEmpty();
        assertThat(StringUtils.trimToEmpty("")).isEmpty();
        assertThat(StringUtils.trimToEmpty("  ")).isEmpty();
        assertThat(StringUtils.trimToEmpty("hello")).isEqualTo("hello");
        assertThat(StringUtils.trimToEmpty("  hello  ")).isEqualTo("hello");
    }

    @Test
    void testDefaultIfEmpty() {
        // 正常场景
        assertThat(StringUtils.defaultIfEmpty(null, "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfEmpty("", "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfEmpty("  ", "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfEmpty("hello", "default")).isEqualTo("hello");
        assertThat(StringUtils.defaultIfEmpty("  hello  ", "default")).isEqualTo("  hello  ");
    }

    @Test
    void testDefaultIfBlank() {
        // 正常场景
        assertThat(StringUtils.defaultIfBlank(null, "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfBlank("", "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfBlank("  ", "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfBlank("\t", "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfBlank("hello", "default")).isEqualTo("hello");
        assertThat(StringUtils.defaultIfBlank("  hello  ", "default")).isEqualTo("  hello  ");
    }
}
