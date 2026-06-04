package com.undernine.utils.test.examples;

import com.undernine.utils.core.string.StringUtils;

/**
 * StringUtils 使用示例
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class StringUtilsExample {

    public static void main(String[] args) {
        System.out.println("========== StringUtils 测试示例 ==========");

        // 测试 isEmpty
        String str1 = null;
        String str2 = "";
        String str3 = "   ";
        String str4 = "Hello";

        System.out.println("\n1. isEmpty 测试:");
        System.out.println("isEmpty(null): " + StringUtils.isEmpty(str1));
        System.out.println("isEmpty(\"\"): " + StringUtils.isEmpty(str2));
        System.out.println("isEmpty(\"   \"): " + StringUtils.isEmpty(str3));
        System.out.println("isEmpty(\"Hello\"): " + StringUtils.isEmpty(str4));

        // 测试 isBlank
        System.out.println("\n2. isBlank 测试:");
        System.out.println("isBlank(null): " + StringUtils.isBlank(str1));
        System.out.println("isBlank(\"\"): " + StringUtils.isBlank(str2));
        System.out.println("isBlank(\"   \"): " + StringUtils.isBlank(str3));
        System.out.println("isBlank(\"Hello\"): " + StringUtils.isBlank(str4));

        // 测试 trim
        String str5 = "  Hello World  ";
        System.out.println("\n3. trim 测试:");
        System.out.println("trim(\"  Hello World  \"): \"" + StringUtils.trim(str5) + "\"");
        System.out.println("trimToNull(\"   \"): " + StringUtils.trimToNull(str3));
        System.out.println("trimToEmpty(null): \"" + StringUtils.trimToEmpty(str1) + "\"");

        // 测试 defaultIfEmpty
        System.out.println("\n4. defaultIfEmpty 测试:");
        System.out.println("defaultIfEmpty(null, \"默认值\"): " + StringUtils.defaultIfEmpty(str1, "默认值"));
        System.out.println("defaultIfEmpty(\"\", \"默认值\"): " + StringUtils.defaultIfEmpty(str2, "默认值"));
        System.out.println("defaultIfEmpty(\"Hello\", \"默认值\"): " + StringUtils.defaultIfEmpty(str4, "默认值"));

        // 测试 defaultIfBlank
        System.out.println("\n5. defaultIfBlank 测试:");
        System.out.println("defaultIfBlank(null, \"默认值\"): " + StringUtils.defaultIfBlank(str1, "默认值"));
        System.out.println("defaultIfBlank(\"   \", \"默认值\"): " + StringUtils.defaultIfBlank(str3, "默认值"));
        System.out.println("defaultIfBlank(\"Hello\", \"默认值\"): " + StringUtils.defaultIfBlank(str4, "默认值"));

        System.out.println("\n========== 测试完成 ==========");
    }
}
