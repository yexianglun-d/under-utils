package com.undernine.utils.test.examples;

import com.undernine.utils.core.time.LocalDateTimeUtils;

import java.time.LocalDateTime;

/**
 * LocalDateTimeUtils 使用示例
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class LocalDateTimeUtilsExample {

    public static void main(String[] args) {
        System.out.println("========== LocalDateTimeUtils 测试示例 ==========");

        // 获取当前时间
        System.out.println("\n1. 获取当前时间:");
        String now = LocalDateTimeUtils.now();
        String today = LocalDateTimeUtils.today();
        System.out.println("当前时间: " + now);
        System.out.println("当前日期: " + today);

        // 格式化时间
        System.out.println("\n2. 格式化时间:");
        LocalDateTime dateTime = LocalDateTime.now();
        String formatted1 = LocalDateTimeUtils.format(dateTime);
        String formatted2 = LocalDateTimeUtils.format(dateTime, "yyyy年MM月dd日 HH时mm分ss秒");
        System.out.println("标准格式: " + formatted1);
        System.out.println("自定义格式: " + formatted2);

        // 解析时间字符串
        System.out.println("\n3. 解析时间字符串:");
        String timeStr = "2024-12-02 17:30:00";
        LocalDateTime parsed = LocalDateTimeUtils.parseDateTime(timeStr);
        System.out.println("解析 \"" + timeStr + "\": " + parsed);

        // 安全解析（不抛异常）
        System.out.println("\n4. 安全解析:");
        String invalidStr = "invalid-date";
        LocalDateTime result1 = LocalDateTimeUtils.tryParseDateTime(invalidStr);
        LocalDateTime result2 = LocalDateTimeUtils.tryParseDateTime("2024-12-02 17:30:00");
        System.out.println("解析无效字符串 \"" + invalidStr + "\": " + result1);
        System.out.println("解析有效字符串: " + result2);

        // 自定义格式解析
        System.out.println("\n5. 自定义格式解析:");
        String customStr = "2024/12/02 17:30:00";
        LocalDateTime parsed2 = LocalDateTimeUtils.parseDateTime(customStr, "yyyy/MM/dd HH:mm:ss");
        System.out.println("解析 \"" + customStr + "\": " + parsed2);

        System.out.println("\n========== 测试完成 ==========");
    }
}
