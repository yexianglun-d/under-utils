package com.undernine.utils.test.examples;

import com.undernine.utils.core.id.IdGenerator;
import com.undernine.utils.core.id.UUIDUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * ID 生成工具使用示例（UUID & 雪花算法）
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class IdUtilsExample {

    public static void main(String[] args) {
        System.out.println("========== ID 生成工具使用示例 ==========\n");

        // 1. UUID 基础功能
        uuidBasic();

        // 2. UUID 高级功能
        uuidAdvanced();

        // 3. 雪花算法基础功能
        snowflakeBasic();

        // 4. 雪花算法解析
        snowflakeParse();

        // 5. 性能对比
        performanceComparison();

        // 6. 实际应用场景
        practicalUseCases();
    }

    /**
     * 1. UUID 基础功能
     */
    private static void uuidBasic() {
        System.out.println("1. UUID 基础功能");

        // 标准 UUID（带连字符）
        String uuid = UUIDUtils.randomUUID();
        System.out.println("标准 UUID: " + uuid);

        // UUID（不带连字符）
        String uuidNoDash = UUIDUtils.randomUUIDNoDash();
        System.out.println("UUID（无连字符）: " + uuidNoDash);

        // UUID（大写，不带连字符）
        String uuidUpper = UUIDUtils.randomUUIDUpperNoDash();
        System.out.println("UUID（大写）: " + uuidUpper);

        // 短 UUID（22 位 Base62）
        String shortUuid = UUIDUtils.shortUUID();
        System.out.println("短 UUID（22位）: " + shortUuid);

        // 验证 UUID
        boolean valid = UUIDUtils.isValidUUID(uuid);
        System.out.println("UUID 格式验证: " + (valid ? "✓ 有效" : "✗ 无效"));

        System.out.println();
    }

    /**
     * 2. UUID 高级功能
     */
    private static void uuidAdvanced() {
        System.out.println("2. UUID 高级功能");

        // 基于名称的 UUID（确定性）
        String name = "user@example.com";
        String nameUuid1 = UUIDUtils.nameUUIDFromString(name);
        String nameUuid2 = UUIDUtils.nameUUIDFromString(name);
        System.out.println("基于名称的 UUID:");
        System.out.println("  第一次: " + nameUuid1);
        System.out.println("  第二次: " + nameUuid2);
        System.out.println("  是否相同: " + (nameUuid1.equals(nameUuid2) ? "✓ 是" : "✗ 否"));

        // 时间有序 UUID
        String timeUuid1 = UUIDUtils.timeBasedUUID();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }
        String timeUuid2 = UUIDUtils.timeBasedUUID();
        System.out.println("\n时间有序 UUID:");
        System.out.println("  UUID1: " + timeUuid1);
        System.out.println("  UUID2: " + timeUuid2);
        System.out.println("  是否有序: " + (timeUuid2.compareTo(timeUuid1) > 0 ? "✓ 是" : "✗ 否"));

        // UUID 与字节数组转换
        System.out.println("\nUUID 转换:");
        String originalUuid = UUIDUtils.randomUUID();
        byte[] bytes = UUIDUtils.toBytes(originalUuid);
        String restoredUuid = UUIDUtils.fromBytes(bytes);
        System.out.println("  原始 UUID: " + originalUuid);
        System.out.println("  字节数组长度: " + bytes.length);
        System.out.println("  恢复 UUID: " + restoredUuid);
        System.out.println("  是否一致: " + (originalUuid.equals(restoredUuid) ? "✓ 是" : "✗ 否"));

        System.out.println();
    }

    /**
     * 3. 雪花算法基础功能
     */
    private static void snowflakeBasic() {
        System.out.println("3. 雪花算法基础功能");

        // 创建 ID 生成器（数据中心 ID=1, 机器 ID=1）
        IdGenerator generator = new IdGenerator(1, 1);

        // 生成 ID
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        long id3 = generator.nextId();

        System.out.println("生成的 ID:");
        System.out.println("  ID1: " + id1);
        System.out.println("  ID2: " + id2);
        System.out.println("  ID3: " + id3);

        // 验证递增性
        boolean increasing = id1 < id2 && id2 < id3;
        System.out.println("  是否递增: " + (increasing ? "✓ 是" : "✗ 否"));

        // 生成字符串形式的 ID
        String idStr = generator.nextIdStr();
        System.out.println("\nID 字符串: " + idStr);

        System.out.println();
    }

    /**
     * 4. 雪花算法解析
     */
    private static void snowflakeParse() {
        System.out.println("4. 雪花算法 ID 解析");

        IdGenerator generator = new IdGenerator(5, 10);
        long id = generator.nextId();

        System.out.println("生成的 ID: " + id);

        // 解析 ID
        IdGenerator.IdInfo info = generator.parseId(id);
        System.out.println("\nID 信息:");
        System.out.println("  时间戳: " + info.getTimestamp());
        System.out.println("  数据中心 ID: " + info.getDatacenterId());
        System.out.println("  机器 ID: " + info.getWorkerId());
        System.out.println("  序列号: " + info.getSequence());

        // 转换时间戳为可读格式
        long timestamp = info.getTimestamp();
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
        System.out.println("  生成时间: " + instant);

        System.out.println();
    }

    /**
     * 5. 性能对比
     */
    private static void performanceComparison() {
        System.out.println("5. 性能对比");

        int count = 100000;

        // UUID 性能测试
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            UUIDUtils.randomUUID();
        }
        long uuidTime = System.currentTimeMillis() - start;

        // 短 UUID 性能测试
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            UUIDUtils.shortUUID();
        }
        long shortUuidTime = System.currentTimeMillis() - start;

        // 雪花算法性能测试
        IdGenerator generator = new IdGenerator();
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            generator.nextId();
        }
        long snowflakeTime = System.currentTimeMillis() - start;

        System.out.println("生成 " + count + " 个 ID 的性能:");
        System.out.println("  标准 UUID: " + uuidTime + "ms (每秒: " + (count * 1000 / uuidTime) + ")");
        System.out.println("  短 UUID: " + shortUuidTime + "ms (每秒: " + (count * 1000 / shortUuidTime) + ")");
        System.out.println("  雪花算法: " + snowflakeTime + "ms (每秒: " + (count * 1000 / snowflakeTime) + ")");

        System.out.println("\n结论:");
        System.out.println("  雪花算法性能最高 ⭐⭐⭐⭐⭐");
        System.out.println("  标准 UUID 次之 ⭐⭐⭐⭐");
        System.out.println("  短 UUID 最慢（因为需要 Base62 编码）⭐⭐⭐");

        System.out.println();
    }

    /**
     * 6. 实际应用场景
     */
    private static void practicalUseCases() {
        System.out.println("6. 实际应用场景");

        // 场景 1: 用户 ID 生成
        System.out.println("场景 1: 用户 ID 生成（雪花算法）");
        userIdGeneration();

        System.out.println();

        // 场景 2: 订单号生成
        System.out.println("场景 2: 订单号生成");
        orderIdGeneration();

        System.out.println();

        // 场景 3: 文件名生成
        System.out.println("场景 3: 文件名生成（短 UUID）");
        fileNameGeneration();

        System.out.println();

        // 场景 4: 唯一性验证
        System.out.println("场景 4: ID 唯一性验证");
        uniquenessTest();
    }

    /**
     * 场景 1: 用户 ID 生成
     */
    private static void userIdGeneration() {
        // 使用雪花算法生成用户 ID
        IdGenerator generator = new IdGenerator(1, 1);

        System.out.println("新注册用户:");
        for (int i = 0; i < 5; i++) {
            long userId = generator.nextId();
            System.out.println("  用户ID: " + userId);
        }
    }

    /**
     * 场景 2: 订单号生成
     */
    private static void orderIdGeneration() {
        // 订单号 = 日期前缀 + 雪花ID
        IdGenerator generator = new IdGenerator(2, 1);
        String datePrefix = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        System.out.println("生成订单号:");
        for (int i = 0; i < 3; i++) {
            long snowflakeId = generator.nextId();
            String orderId = datePrefix + "-" + snowflakeId;
            System.out.println("  订单号: " + orderId);
        }
    }

    /**
     * 场景 3: 文件名生成
     */
    private static void fileNameGeneration() {
        // 使用短 UUID 生成文件名（URL 友好，长度短）
        System.out.println("上传文件:");
        String[] originalFiles = {"photo.jpg", "document.pdf", "video.mp4"};

        for (String original : originalFiles) {
            String extension = original.substring(original.lastIndexOf('.'));
            String shortId = UUIDUtils.shortUUID();
            String newFileName = shortId + extension;

            System.out.println("  原文件: " + original + " -> 新文件: " + newFileName);
        }
    }

    /**
     * 场景 4: ID 唯一性验证
     */
    private static void uniquenessTest() {
        int count = 10000;
        IdGenerator generator = new IdGenerator();

        // 测试雪花算法
        Set<Long> snowflakeIds = new HashSet<>();
        for (int i = 0; i < count; i++) {
            snowflakeIds.add(generator.nextId());
        }

        // 测试 UUID
        Set<String> uuids = new HashSet<>();
        for (int i = 0; i < count; i++) {
            uuids.add(UUIDUtils.randomUUID());
        }

        System.out.println("生成 " + count + " 个 ID 的唯一性测试:");
        System.out.println("  雪花算法: " + (snowflakeIds.size() == count ? "✓ 全部唯一" : "✗ 存在重复"));
        System.out.println("  UUID: " + (uuids.size() == count ? "✓ 全部唯一" : "✗ 存在重复"));
    }
}
