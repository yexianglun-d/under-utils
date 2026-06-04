package com.undernine.utils.test.examples;

import com.undernine.utils.core.validation.ValidationUtils;

/**
 * ValidationUtils 使用示例
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class ValidationUtilsExample {

    public static void main(String[] args) {
        System.out.println("========== ValidationUtils 使用示例 ==========\n");

        // 1. 手机号校验
        phoneValidation();

        // 2. 邮箱校验
        emailValidation();

        // 3. 身份证号校验
        idCardValidation();

        // 4. URL 校验
        urlValidation();

        // 5. IP 地址校验
        ipValidation();

        // 6. 中文校验
        chineseValidation();

        // 7. 数字校验
        numberValidation();

        // 8. 范围校验
        rangeValidation();

        // 9. 字符校验
        characterValidation();

        // 10. 实际应用场景
        practicalUseCases();
    }

    /**
     * 1. 手机号校验
     */
    private static void phoneValidation() {
        System.out.println("1. 手机号校验");

        String[] phones = {
                "13812345678",  // 有效
                "15987654321",  // 有效
                "19912345678",  // 有效
                "12345678901",  // 无效（不是以 13-19 开头）
                "138123456789", // 无效（12 位）
                "1381234567"    // 无效（10 位）
        };

        for (String phone : phones) {
            boolean valid = ValidationUtils.isPhone(phone);
            System.out.println(phone + " -> " + (valid ? "✓ 有效" : "✗ 无效"));
        }
        System.out.println();
    }

    /**
     * 2. 邮箱校验
     */
    private static void emailValidation() {
        System.out.println("2. 邮箱校验");

        String[] emails = {
                "user@example.com",       // 有效
                "test.user@example.com",  // 有效
                "user+tag@example.co.uk", // 有效
                "invalid-email",          // 无效
                "@example.com",           // 无效
                "user@"                   // 无效
        };

        for (String email : emails) {
            boolean valid = ValidationUtils.isEmail(email);
            System.out.println(email + " -> " + (valid ? "✓ 有效" : "✗ 无效"));
        }
        System.out.println();
    }

    /**
     * 3. 身份证号校验
     */
    private static void idCardValidation() {
        System.out.println("3. 身份证号校验（含校验位验证）");

        String[] idCards = {
                "110101199003074796", // 有效（示例）
                "31010519900307869X", // 有效（示例）
                "123456789012345678", // 无效（格式错误）
                "11010119900307479"   // 无效（17 位）
        };

        for (String idCard : idCards) {
            boolean valid = ValidationUtils.isIdCard(idCard);
            System.out.println(idCard + " -> " + (valid ? "✓ 有效" : "✗ 无效"));
        }
        System.out.println();
    }

    /**
     * 4. URL 校验
     */
    private static void urlValidation() {
        System.out.println("4. URL 校验");

        String[] urls = {
                "https://www.example.com",         // 有效
                "http://example.com/path",         // 有效
                "https://example.com:8080/path",   // 有效
                "ftp://ftp.example.com/file.txt",  // 有效
                "not a url",                       // 无效
                "www.example.com"                  // 无效（缺少协议）
        };

        for (String url : urls) {
            boolean valid = ValidationUtils.isUrl(url);
            System.out.println(url + " -> " + (valid ? "✓ 有效" : "✗ 无效"));
        }
        System.out.println();
    }

    /**
     * 5. IP 地址校验
     */
    private static void ipValidation() {
        System.out.println("5. IP 地址校验");

        String[] ips = {
                "192.168.1.1",     // 有效
                "127.0.0.1",       // 有效
                "255.255.255.255", // 有效
                "256.1.1.1",       // 无效（超出范围）
                "192.168.1",       // 无效（缺少段）
                "abc.def.ghi.jkl"  // 无效（非数字）
        };

        for (String ip : ips) {
            boolean valid = ValidationUtils.isIpv4(ip);
            System.out.println(ip + " -> " + (valid ? "✓ 有效" : "✗ 无效"));
        }
        System.out.println();
    }

    /**
     * 6. 中文校验
     */
    private static void chineseValidation() {
        System.out.println("6. 中文校验");

        String[] strings = {
                "中文",      // 全中文
                "测试数据",   // 全中文
                "abc中文",   // 包含中文
                "中文123",   // 包含中文
                "english"   // 不含中文
        };

        for (String str : strings) {
            boolean isChinese = ValidationUtils.isChinese(str);
            boolean containsChinese = ValidationUtils.containsChinese(str);
            System.out.println(str + " -> 全中文: " + (isChinese ? "✓" : "✗") +
                    ", 包含中文: " + (containsChinese ? "✓" : "✗"));
        }
        System.out.println();
    }

    /**
     * 7. 数字校验
     */
    private static void numberValidation() {
        System.out.println("7. 数字校验");

        String[] numbers = {
                "123",     // 整数
                "-456",    // 负整数
                "123.45",  // 小数
                "-67.89",  // 负小数
                "abc",     // 非数字
                "12.34.56" // 多个小数点
        };

        for (String num : numbers) {
            boolean isInteger = ValidationUtils.isInteger(num);
            boolean isDecimal = ValidationUtils.isDecimal(num);
            boolean isNumber = ValidationUtils.isNumber(num);
            System.out.println(num + " -> 整数: " + (isInteger ? "✓" : "✗") +
                    ", 小数: " + (isDecimal ? "✓" : "✗") +
                    ", 数字: " + (isNumber ? "✓" : "✗"));
        }
        System.out.println();
    }

    /**
     * 8. 范围校验
     */
    private static void rangeValidation() {
        System.out.println("8. 范围校验");

        // 数值范围
        System.out.println("数值范围校验（1-100）:");
        int[] values = {0, 1, 50, 100, 101};
        for (int value : values) {
            boolean inRange = ValidationUtils.isInRange(value, 1, 100);
            System.out.println("  " + value + " -> " + (inRange ? "✓ 在范围内" : "✗ 超出范围"));
        }

        // 字符串长度范围
        System.out.println("\n字符串长度范围校验（3-10）:");
        String[] strings = {"ab", "abc", "hello", "hello world"};
        for (String str : strings) {
            boolean lengthOk = ValidationUtils.isLengthInRange(str, 3, 10);
            System.out.println("  \"" + str + "\" (长度: " + str.length() + ") -> " +
                    (lengthOk ? "✓ 符合" : "✗ 不符合"));
        }
        System.out.println();
    }

    /**
     * 9. 字符校验
     */
    private static void characterValidation() {
        System.out.println("9. 字符校验");

        String[] strings = {
                "abc",      // 纯字母
                "ABC",      // 纯字母
                "abc123",   // 字母+数字
                "abc-123",  // 含特殊字符
                "123"       // 纯数字
        };

        for (String str : strings) {
            boolean isAlpha = ValidationUtils.isAlpha(str);
            boolean isAlphanumeric = ValidationUtils.isAlphanumeric(str);
            System.out.println(str + " -> 纯字母: " + (isAlpha ? "✓" : "✗") +
                    ", 字母或数字: " + (isAlphanumeric ? "✓" : "✗"));
        }
        System.out.println();
    }

    /**
     * 10. 实际应用场景
     */
    private static void practicalUseCases() {
        System.out.println("10. 实际应用场景");

        // 场景 1: 用户注册信息验证
        System.out.println("场景 1: 用户注册信息验证");
        UserRegistration registration = new UserRegistration(
                "张三",
                "13812345678",
                "zhangsan@example.com",
                "password123"
        );
        System.out.println(validateUserRegistration(registration));

        System.out.println();

        // 场景 2: 表单数据验证
        System.out.println("场景 2: 表单数据验证");
        FormData form = new FormData(
                "用户反馈",
                "这是一条用户反馈信息",
                "https://example.com/callback"
        );
        System.out.println(validateFormData(form));

        System.out.println();

        // 场景 3: API 参数验证
        System.out.println("场景 3: API 参数验证");
        ApiRequest request = new ApiRequest(
                100,
                10,
                "name"
        );
        System.out.println(validateApiRequest(request));
    }

    /**
     * 验证用户注册信息
     */
    private static String validateUserRegistration(UserRegistration reg) {
        StringBuilder result = new StringBuilder();
        result.append("验证用户注册信息:\n");

        // 姓名验证：必须是中文，2-10 个字符
        boolean nameValid = ValidationUtils.isChinese(reg.name) &&
                ValidationUtils.isLengthInRange(reg.name, 2, 10);
        result.append("  姓名: ").append(reg.name)
                .append(" -> ").append(nameValid ? "✓" : "✗").append("\n");

        // 手机号验证
        boolean phoneValid = ValidationUtils.isPhone(reg.phone);
        result.append("  手机号: ").append(reg.phone)
                .append(" -> ").append(phoneValid ? "✓" : "✗").append("\n");

        // 邮箱验证
        boolean emailValid = ValidationUtils.isEmail(reg.email);
        result.append("  邮箱: ").append(reg.email)
                .append(" -> ").append(emailValid ? "✓" : "✗").append("\n");

        // 密码验证：6-20 位字母或数字
        boolean passwordValid = ValidationUtils.isAlphanumeric(reg.password) &&
                ValidationUtils.isLengthInRange(reg.password, 6, 20);
        result.append("  密码: ").append(maskSecret(reg.password))
                .append(" -> ").append(passwordValid ? "✓" : "✗").append("\n");

        boolean allValid = nameValid && phoneValid && emailValid && passwordValid;
        result.append("  整体验证: ").append(allValid ? "✓ 通过" : "✗ 失败");

        return result.toString();
    }

    private static String maskSecret(String value) {
        return value == null || value.isEmpty() ? "" : "******";
    }

    /**
     * 验证表单数据
     */
    private static String validateFormData(FormData form) {
        StringBuilder result = new StringBuilder();
        result.append("验证表单数据:\n");

        // 标题验证：1-50 字符
        boolean titleValid = ValidationUtils.isLengthInRange(form.title, 1, 50);
        result.append("  标题长度: ").append(form.title.length())
                .append(" -> ").append(titleValid ? "✓" : "✗").append("\n");

        // 内容验证：10-500 字符
        boolean contentValid = ValidationUtils.isLengthInRange(form.content, 10, 500);
        result.append("  内容长度: ").append(form.content.length())
                .append(" -> ").append(contentValid ? "✓" : "✗").append("\n");

        // 回调 URL 验证
        boolean urlValid = ValidationUtils.isUrl(form.callbackUrl);
        result.append("  回调URL: ").append(form.callbackUrl)
                .append(" -> ").append(urlValid ? "✓" : "✗").append("\n");

        boolean allValid = titleValid && contentValid && urlValid;
        result.append("  整体验证: ").append(allValid ? "✓ 通过" : "✗ 失败");

        return result.toString();
    }

    /**
     * 验证 API 请求参数
     */
    private static String validateApiRequest(ApiRequest request) {
        StringBuilder result = new StringBuilder();
        result.append("验证 API 请求参数:\n");

        // 页码验证：1-1000
        boolean pageValid = ValidationUtils.isInRange(request.page, 1, 1000);
        result.append("  页码: ").append(request.page)
                .append(" -> ").append(pageValid ? "✓" : "✗").append("\n");

        // 每页大小验证：1-100
        boolean sizeValid = ValidationUtils.isInRange(request.size, 1, 100);
        result.append("  每页大小: ").append(request.size)
                .append(" -> ").append(sizeValid ? "✓" : "✗").append("\n");

        // 排序字段验证：只能包含字母
        boolean sortValid = ValidationUtils.isAlpha(request.sort);
        result.append("  排序字段: ").append(request.sort)
                .append(" -> ").append(sortValid ? "✓" : "✗").append("\n");

        boolean allValid = pageValid && sizeValid && sortValid;
        result.append("  整体验证: ").append(allValid ? "✓ 通过" : "✗ 失败");

        return result.toString();
    }

    // ==================== 测试数据类 ====================

    static class UserRegistration {
        String name;
        String phone;
        String email;
        String password;

        UserRegistration(String name, String phone, String email, String password) {
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.password = password;
        }
    }

    static class FormData {
        String title;
        String content;
        String callbackUrl;

        FormData(String title, String content, String callbackUrl) {
            this.title = title;
            this.content = content;
            this.callbackUrl = callbackUrl;
        }
    }

    static class ApiRequest {
        int page;
        int size;
        String sort;

        ApiRequest(int page, int size, String sort) {
            this.page = page;
            this.size = size;
            this.sort = sort;
        }
    }
}
