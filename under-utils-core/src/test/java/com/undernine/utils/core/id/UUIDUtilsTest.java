package com.undernine.utils.core.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * UUIDUtils 测试类
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
class UUIDUtilsTest {

    // ==================== randomUUID() 测试 ====================

    @Test
    void testRandomUUID() {
        String uuid = UUIDUtils.randomUUID();

        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(36); // 标准 UUID 长度：32 + 4 个连字符
        assertThat(uuid).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    void testRandomUUID_uniqueness() {
        // 生成多个 UUID，验证唯一性
        Set<String> uuids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            uuids.add(UUIDUtils.randomUUID());
        }
        assertThat(uuids).hasSize(1000); // 所有 UUID 都不相同
    }

    // ==================== randomUUIDNoDash() 测试 ====================

    @Test
    void testRandomUUIDNoDash() {
        String uuid = UUIDUtils.randomUUIDNoDash();

        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(32); // 不含连字符：32 个字符
        assertThat(uuid).matches("^[0-9a-f]{32}$");
        assertThat(uuid).doesNotContain("-");
    }

    // ==================== randomUUIDUpperNoDash() 测试 ====================

    @Test
    void testRandomUUIDUpperNoDash() {
        String uuid = UUIDUtils.randomUUIDUpperNoDash();

        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(32);
        assertThat(uuid).matches("^[0-9A-F]{32}$");
        assertThat(uuid).doesNotContain("-");
        assertThat(uuid).isUpperCase();
    }

    // ==================== shortUUID() 测试 ====================

    @Test
    void testShortUUID() {
        String shortUuid = UUIDUtils.shortUUID();

        assertThat(shortUuid).isNotNull();
        assertThat(shortUuid).hasSize(22); // Base62 编码后的长度
        assertThat(shortUuid).matches("^[0-9A-Za-z]{22}$");
    }

    @Test
    void testShortUUID_uniqueness() {
        Set<String> shortUuids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            shortUuids.add(UUIDUtils.shortUUID());
        }
        assertThat(shortUuids).hasSize(1000);
    }

    // ==================== nameUUIDFromString() 测试 ====================

    @Test
    void testNameUUIDFromString() {
        String name = "test";
        String uuid = UUIDUtils.nameUUIDFromString(name);

        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(36);
        assertThat(UUIDUtils.isValidUUID(uuid)).isTrue();
    }

    @Test
    void testNameUUIDFromString_deterministic() {
        // 相同的名称应该生成相同的 UUID
        String name = "test";
        String uuid1 = UUIDUtils.nameUUIDFromString(name);
        String uuid2 = UUIDUtils.nameUUIDFromString(name);

        assertThat(uuid1).isEqualTo(uuid2);
    }

    @Test
    void testNameUUIDFromString_differentNames() {
        // 不同的名称应该生成不同的 UUID
        String uuid1 = UUIDUtils.nameUUIDFromString("test1");
        String uuid2 = UUIDUtils.nameUUIDFromString("test2");

        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    void testNameUUIDFromString_null() {
        assertThatThrownBy(() -> UUIDUtils.nameUUIDFromString(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== nameUUIDFromStringNoDash() 测试 ====================

    @Test
    void testNameUUIDFromStringNoDash() {
        String uuid = UUIDUtils.nameUUIDFromStringNoDash("test");

        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(32);
        assertThat(uuid).doesNotContain("-");
    }

    // ==================== timeBasedUUID() 测试 ====================

    @Test
    void testTimeBasedUUID() {
        String uuid = UUIDUtils.timeBasedUUID();

        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(32);
        assertThat(uuid).matches("^[0-9a-f]{32}$");
    }

    @Test
    void testTimeBasedUUID_ordering() throws InterruptedException {
        // 验证时间顺序性
        String uuid1 = UUIDUtils.timeBasedUUID();
        Thread.sleep(10); // 等待 10ms
        String uuid2 = UUIDUtils.timeBasedUUID();

        // uuid2 应该大于 uuid1（字典序）
        assertThat(uuid2.compareTo(uuid1)).isGreaterThan(0);
    }

    // ==================== timeBasedUUIDWithDash() 测试 ====================

    @Test
    void testTimeBasedUUIDWithDash() {
        String uuid = UUIDUtils.timeBasedUUIDWithDash();

        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(36);
        assertThat(uuid).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    // ==================== isValidUUID() 测试 ====================

    @Test
    void testIsValidUUID_valid() {
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";
        assertThat(UUIDUtils.isValidUUID(validUuid)).isTrue();

        // 使用 randomUUID 生成的也应该是有效的
        assertThat(UUIDUtils.isValidUUID(UUIDUtils.randomUUID())).isTrue();
    }

    @Test
    void testIsValidUUID_invalid() {
        assertThat(UUIDUtils.isValidUUID("invalid-uuid")).isFalse();
        assertThat(UUIDUtils.isValidUUID("12345678-1234-1234-1234-1234567890")).isFalse(); // 长度不对
        assertThat(UUIDUtils.isValidUUID("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")).isFalse(); // 非十六进制
    }

    @Test
    void testIsValidUUID_null() {
        assertThat(UUIDUtils.isValidUUID(null)).isFalse();
    }

    @Test
    void testIsValidUUID_empty() {
        assertThat(UUIDUtils.isValidUUID("")).isFalse();
    }

    // ==================== toBytes() 测试 ====================

    @Test
    void testToBytes() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        byte[] bytes = UUIDUtils.toBytes(uuid);

        assertThat(bytes).isNotNull();
        assertThat(bytes).hasSize(16); // UUID 是 128 位，即 16 字节
    }

    @Test
    void testToBytes_invalid() {
        assertThatThrownBy(() -> UUIDUtils.toBytes("invalid-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== fromBytes() 测试 ====================

    @Test
    void testFromBytes() {
        String originalUuid = "550e8400-e29b-41d4-a716-446655440000";
        byte[] bytes = UUIDUtils.toBytes(originalUuid);
        String restoredUuid = UUIDUtils.fromBytes(bytes);

        assertThat(restoredUuid).isEqualTo(originalUuid);
    }

    @Test
    void testFromBytes_invalid() {
        assertThatThrownBy(() -> UUIDUtils.fromBytes(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UUIDUtils.fromBytes(new byte[8]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== 综合测试 ====================

    @Test
    void testRoundTrip() {
        // UUID -> bytes -> UUID
        String original = UUIDUtils.randomUUID();
        byte[] bytes = UUIDUtils.toBytes(original);
        String restored = UUIDUtils.fromBytes(bytes);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void testDifferentMethods() {
        // 不同方法生成的 UUID 应该不同（除了 nameUUID）
        String random1 = UUIDUtils.randomUUID();
        String random2 = UUIDUtils.randomUUID();
        String timeBased = UUIDUtils.timeBasedUUID();
        String shortUuid = UUIDUtils.shortUUID();

        assertThat(random1).isNotEqualTo(random2);
        assertThat(random1.replace("-", "")).isNotEqualTo(timeBased);
    }

    @Test
    void testPerformance() {
        // 性能测试：生成 10000 个 UUID
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            UUIDUtils.randomUUID();
        }
        long duration = System.currentTimeMillis() - start;

        System.out.println("生成 10000 个 UUID 耗时: " + duration + "ms");
        assertThat(duration).isLessThan(1000); // 应该在 1 秒内完成
    }
}
