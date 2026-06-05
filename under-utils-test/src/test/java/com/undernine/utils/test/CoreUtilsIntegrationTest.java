package com.undernine.utils.test;

import com.undernine.utils.core.string.StringUtils;
import com.undernine.utils.core.time.LocalDateTimeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 核心工具类集成测试
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("核心工具类集成测试")
@SuppressWarnings("deprecation")
class CoreUtilsIntegrationTest {

    @Test
    @DisplayName("测试 StringUtils 基本功能")
    void testStringUtils() {
        // isEmpty
        assertThat(StringUtils.isEmpty(null)).isTrue();
        assertThat(StringUtils.isEmpty("")).isTrue();
        assertThat(StringUtils.isEmpty("  ")).isTrue();
        assertThat(StringUtils.isEmpty("hello")).isFalse();

        // isBlank
        assertThat(StringUtils.isBlank("   ")).isTrue();
        assertThat(StringUtils.isBlank("\t\n")).isTrue();

        // trim
        assertThat(StringUtils.trim("  hello  ")).isEqualTo("hello");
        assertThat(StringUtils.trimToNull("   ")).isNull();

        // defaultIfEmpty
        assertThat(StringUtils.defaultIfEmpty(null, "default")).isEqualTo("default");
        assertThat(StringUtils.defaultIfEmpty("value", "default")).isEqualTo("value");
    }

    @Test
    @DisplayName("测试 LocalDateTimeUtils 基本功能")
    void testLocalDateTimeUtils() {
        // 当前时间
        String now = LocalDateTimeUtils.now();
        assertThat(now).isNotNull();
        assertThat(now).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");

        // 格式化
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 2, 17, 30, 0);
        String formatted = LocalDateTimeUtils.format(dateTime);
        assertThat(formatted).isEqualTo("2024-12-02 17:30:00");

        // 解析
        LocalDateTime parsed = LocalDateTimeUtils.parseDateTime("2024-12-02 17:30:00");
        assertThat(parsed).isEqualTo(dateTime);

        // 安全解析
        LocalDateTime invalid = LocalDateTimeUtils.tryParseDateTime("invalid");
        assertThat(invalid).isNull();

        LocalDateTime valid = LocalDateTimeUtils.tryParseDateTime("2024-12-02 17:30:00");
        assertThat(valid).isNotNull();
    }

    @Test
    @DisplayName("测试工具类组合使用")
    void testUtilsCombination() {
        // 组合使用多个工具类
        String input = "  2024-12-02 17:30:00  ";
        String trimmed = StringUtils.trim(input);
        assertThat(trimmed).isEqualTo("2024-12-02 17:30:00");

        LocalDateTime dateTime = LocalDateTimeUtils.parseDateTime(trimmed);
        assertThat(dateTime).isNotNull();

        String formatted = LocalDateTimeUtils.format(dateTime, "yyyy/MM/dd HH:mm");
        assertThat(formatted).isEqualTo("2024/12/02 17:30");
    }
}
