package com.undernine.utils.test.examples;

import com.undernine.utils.core.crypto.AESUtils;
import com.undernine.utils.core.crypto.MD5Utils;
import com.undernine.utils.core.crypto.SHA256Utils;

/**
 * 加密工具使用示例（MD5、SHA-256、AES）
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class CryptoUtilsExample {

    public static void main(String[] args) {
        System.out.println("========== 加密工具使用示例 ==========\n");

        // 1. MD5 基础功能
        md5Basic();

        // 2. MD5 加盐
        md5WithSalt();

        // 3. SHA-256 基础功能
        sha256Basic();

        // 4. SHA-256 加盐
        sha256WithSalt();

        // 5. SHA-256 多次哈希
        sha256Multiple();

        // 6. AES 对称加密
        aesEncryption();

        // 7. MD5 vs SHA-256 vs AES
        cryptoComparison();

        // 8. 实际应用场景
        practicalUseCases();
    }

    /**
     * 1. MD5 基础功能
     */
    private static void md5Basic() {
        System.out.println("1. MD5 基础功能");

        String text = "hello world";

        // 32 位小写
        String md5 = MD5Utils.md5(text);
        System.out.println("MD5 (32位小写): " + md5);

        // 32 位大写
        String md5Upper = MD5Utils.md5Upper(text);
        System.out.println("MD5 (32位大写): " + md5Upper);

        // 16 位小写（取中间 16 位）
        String md5Short = MD5Utils.md5Short(text);
        System.out.println("MD5 (16位小写): " + md5Short);

        // 16 位大写
        String md5ShortUpper = MD5Utils.md5ShortUpper(text);
        System.out.println("MD5 (16位大写): " + md5ShortUpper);

        // 验证
        boolean isValid = MD5Utils.verify(text, md5);
        System.out.println("验证结果: " + (isValid ? "✓ 通过" : "✗ 失败"));

        System.out.println();
    }

    /**
     * 2. MD5 加盐
     */
    private static void md5WithSalt() {
        System.out.println("2. MD5 加盐（仅用于兼容演示，不用于密码存储）");

        String sampleInput = "sample-input";
        String salt = "demo-random-salt";

        // 不加盐
        String md5NoSalt = MD5Utils.md5(sampleInput);
        System.out.println("输入哈希（无盐）: " + md5NoSalt);

        // 加盐
        String md5WithSalt = MD5Utils.md5WithSalt(sampleInput, salt);
        System.out.println("输入哈希（加盐）: " + md5WithSalt);

        // 验证加盐摘要
        boolean isValid = MD5Utils.verifyWithSalt(sampleInput, salt, md5WithSalt);
        System.out.println("加盐验证: " + (isValid ? "✓ 通过" : "✗ 失败"));

        // 错误的盐无法验证
        boolean wrongSalt = MD5Utils.verifyWithSalt(sampleInput, "wrongSalt", md5WithSalt);
        System.out.println("错误盐验证: " + (wrongSalt ? "✓ 通过" : "✗ 失败"));

        System.out.println();
    }

    /**
     * 3. SHA-256 基础功能
     */
    private static void sha256Basic() {
        System.out.println("3. SHA-256 基础功能");

        String text = "hello world";

        // 64 位小写
        String sha256 = SHA256Utils.sha256(text);
        System.out.println("SHA-256 (64位小写): " + sha256);

        // 64 位大写
        String sha256Upper = SHA256Utils.sha256Upper(text);
        System.out.println("SHA-256 (64位大写): " + sha256Upper);

        // 验证
        boolean isValid = SHA256Utils.verify(text, sha256);
        System.out.println("验证结果: " + (isValid ? "✓ 通过" : "✗ 失败"));

        System.out.println();
    }

    /**
     * 4. SHA-256 加盐
     */
    private static void sha256WithSalt() {
        System.out.println("4. SHA-256 加盐");

        String sampleInput = "sample-input";
        String salt = "demo-random-salt";

        // 不加盐
        String sha256NoSalt = SHA256Utils.sha256(sampleInput);
        System.out.println("输入哈希（无盐）: " + sha256NoSalt);

        // 加盐
        String sha256WithSalt = SHA256Utils.sha256WithSalt(sampleInput, salt);
        System.out.println("输入哈希（加盐）: " + sha256WithSalt);

        // 验证加盐摘要
        boolean isValid = SHA256Utils.verifyWithSalt(sampleInput, salt, sha256WithSalt);
        System.out.println("加盐验证: " + (isValid ? "✓ 通过" : "✗ 失败"));

        System.out.println();
    }

    /**
     * 5. SHA-256 多次哈希
     */
    private static void sha256Multiple() {
        System.out.println("5. SHA-256 多次哈希（演示计算成本，不替代专业密码哈希算法）");

        String sampleInput = "sample-input";

        // 单次哈希
        String hash1 = SHA256Utils.sha256(sampleInput);
        System.out.println("单次哈希: " + hash1.substring(0, 32) + "...");

        // 多次哈希（1000次）
        long start = System.currentTimeMillis();
        String hash1000 = SHA256Utils.sha256Multiple(sampleInput, 1000);
        long time1000 = System.currentTimeMillis() - start;
        System.out.println("1000次哈希: " + hash1000.substring(0, 32) + "... (耗时: " + time1000 + "ms)");

        // 多次哈希（10000次）
        start = System.currentTimeMillis();
        String hash10000 = SHA256Utils.sha256Multiple(sampleInput, 10000);
        long time10000 = System.currentTimeMillis() - start;
        System.out.println("10000次哈希: " + hash10000.substring(0, 32) + "... (耗时: " + time10000 + "ms)");

        System.out.println("\n说明: 生产密码存储应使用 BCrypt、Argon2 或 PBKDF2 等专用算法");
        System.out.println();
    }

    /**
     * 6. AES 对称加密
     */
    private static void aesEncryption() {
        System.out.println("6. AES 对称加密");

        // 生成密钥和 IV
        String key = AESUtils.generateKey();
        String iv = AESUtils.generateIV();
        System.out.println("密钥(Base64): " + key);
        System.out.println("IV(Base64): " + iv);

        // 加密
        String plainText = "这是需要加密的敏感数据";
        System.out.println("\n明文: " + plainText);

        String encrypted = AESUtils.encrypt(plainText, key, iv);
        System.out.println("密文: " + encrypted);

        // 解密
        String decrypted = AESUtils.decrypt(encrypted, key, iv);
        System.out.println("解密: " + decrypted);
        System.out.println("是否一致: " + (plainText.equals(decrypted) ? "✓" : "✗"));

        // ECB 模式（不推荐）
        System.out.println("\nECB 模式（不推荐，仅用于演示）:");
        String encryptedECB = AESUtils.encryptECB(plainText, key);
        String decryptedECB = AESUtils.decryptECB(encryptedECB, key);
        System.out.println("ECB 密文: " + encryptedECB);
        System.out.println("ECB 解密: " + decryptedECB);

        System.out.println("\n说明: CBC 模式更安全，每次加密使用不同的 IV");
        System.out.println();
    }

    /**
     * 7. 加密算法对比
     */
    private static void cryptoComparison() {
        System.out.println("7. 加密算法对比");

        String text = "hello world";

        String md5 = MD5Utils.md5(text);
        String sha256 = SHA256Utils.sha256(text);

        System.out.println("原文: " + text);
        System.out.println("MD5    (32位): " + md5);
        System.out.println("SHA-256(64位): " + sha256);

        // AES 加密
        String aesKey = AESUtils.generateKey();
        String aesIV = AESUtils.generateIV();
        String aesEncrypted = AESUtils.encrypt(text, aesKey, aesIV);
        System.out.println("AES 密文: " + aesEncrypted);

        System.out.println("\n算法类型对比:");
        System.out.println("  MD5 & SHA-256: 单向哈希算法");
        System.out.println("    - 不可逆，无法从哈希值还原原文");
        System.out.println("    - 用于数据完整性校验、非密码类摘要");
        System.out.println("    - 相同输入产生相同输出");
        System.out.println("  AES: 对称加密算法");
        System.out.println("    - 可逆，可以解密还原原文");
        System.out.println("    - 用于数据加密传输、敏感信息存储");
        System.out.println("    - 需要密钥管理，密钥泄露则数据不安全");

        System.out.println("\n安全性对比:");
        System.out.println("  MD5: ⭐⭐ (已被破解，不建议用于安全场景)");
        System.out.println("  SHA-256: ⭐⭐⭐⭐⭐ (目前安全，推荐使用)");
        System.out.println("  AES: ⭐⭐⭐⭐⭐ (密钥安全则数据安全)");

        System.out.println();
    }

    /**
     * 7. 实际应用场景
     */
    private static void practicalUseCases() {
        System.out.println("7. 实际应用场景");

        // 场景 1: 密码校验边界演示
        System.out.println("场景 1: 密码校验边界演示");
        passwordHashingBoundary();

        System.out.println();

        // 场景 2: 文件完整性校验
        System.out.println("场景 2: 文件完整性校验");
        fileIntegrityCheck();

        System.out.println();

        // 场景 3: API 签名验证
        System.out.println("场景 3: API 签名验证");
        apiSignature();

        System.out.println();

        // 场景 4: 数据去重（唯一标识）
        System.out.println("场景 4: 数据去重（唯一标识）");
        dataDeduplication();
    }

    /**
     * 场景 1: 密码校验边界演示
     */
    private static void passwordHashingBoundary() {
        // 用户注册
        String username = "zhangsan";
        String password = "demo-password";
        String salt = generateSalt(username); // 根据用户名生成盐

        // 这里只演示摘要验证流程，生产密码存储应使用 BCrypt、Argon2 或 PBKDF2。
        String storedPassword = SHA256Utils.sha256Multiple(password + salt, 5000);
        System.out.println("用户: " + username);
        System.out.println("存储摘要: " + storedPassword.substring(0, 32) + "...");

        // 用户登录验证
        String loginPassword = "demo-password";
        String computedHash = SHA256Utils.sha256Multiple(loginPassword + salt, 5000);
        boolean loginSuccess = storedPassword.equals(computedHash);

        System.out.println("登录验证: " + (loginSuccess ? "✓ 成功" : "✗ 失败"));
    }

    /**
     * 场景 2: 文件完整性校验
     */
    private static void fileIntegrityCheck() {
        // 模拟文件内容
        String fileContent = "This is a very important document...";

        // 计算文件的 MD5 和 SHA-256
        String fileMd5 = MD5Utils.md5(fileContent);
        String fileSha256 = SHA256Utils.sha256(fileContent);

        System.out.println("文件 MD5: " + fileMd5);
        System.out.println("文件 SHA-256: " + fileSha256.substring(0, 32) + "...");

        // 下载后验证
        String downloadedContent = "This is a very important document...";
        boolean md5Match = MD5Utils.verify(downloadedContent, fileMd5);
        boolean sha256Match = SHA256Utils.verify(downloadedContent, fileSha256);

        System.out.println("MD5 校验: " + (md5Match ? "✓ 通过" : "✗ 失败"));
        System.out.println("SHA-256 校验: " + (sha256Match ? "✓ 通过" : "✗ 失败"));
    }

    /**
     * 场景 3: API 签名验证
     */
    private static void apiSignature() {
        // API 请求参数
        String appId = "app123";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String data = "param1=value1&param2=value2";
        String secret = "demo-shared-secret";

        // 生成签名
        String signString = appId + timestamp + data + secret;
        String signature = SHA256Utils.sha256(signString);

        System.out.println("API 请求:");
        System.out.println("  appId: " + appId);
        System.out.println("  timestamp: " + timestamp);
        System.out.println("  data: " + data);
        System.out.println("  signature: " + signature.substring(0, 32) + "...");

        // 服务端验证
        String serverSignString = appId + timestamp + data + secret;
        String serverSignature = SHA256Utils.sha256(serverSignString);
        boolean signatureValid = signature.equals(serverSignature);

        System.out.println("签名验证: " + (signatureValid ? "✓ 通过" : "✗ 失败"));
    }

    /**
     * 场景 4: 数据去重（唯一标识）
     */
    private static void dataDeduplication() {
        // 使用 MD5 作为数据的唯一标识
        String data1 = "用户输入的内容1";
        String data2 = "用户输入的内容2";
        String data3 = "用户输入的内容1"; // 重复

        String id1 = MD5Utils.md5Short(data1);
        String id2 = MD5Utils.md5Short(data2);
        String id3 = MD5Utils.md5Short(data3);

        System.out.println("数据1 ID: " + id1);
        System.out.println("数据2 ID: " + id2);
        System.out.println("数据3 ID: " + id3);

        if (id1.equals(id3)) {
            System.out.println("✓ 检测到重复数据（数据1 和 数据3）");
        }
    }

    /**
     * 生成盐（简化版，实际应使用随机盐）
     */
    private static String generateSalt(String username) {
        return "salt_" + username + "_" + System.currentTimeMillis();
    }
}
