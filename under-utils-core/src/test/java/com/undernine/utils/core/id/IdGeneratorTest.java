package com.undernine.utils.core.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * IdGenerator 测试类
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
class IdGeneratorTest {

    // ==================== 构造方法测试 ====================

    @Test
    void testConstructor_default() {
        IdGenerator generator = new IdGenerator();
        long id = generator.nextId();
        assertThat(id).isPositive();
    }

    @Test
    void testConstructor_withParams() {
        IdGenerator generator = new IdGenerator(1, 1);
        long id = generator.nextId();
        assertThat(id).isPositive();
        assertThat(generator.isNodeIdFullyConfigured()).isTrue();
    }

    @Test
    void testConstructor_defaultReadsSystemProperties() {
        String oldDatacenter = System.getProperty("under.utils.id.datacenter-id");
        String oldWorker = System.getProperty("under.utils.id.worker-id");
        try {
            System.setProperty("under.utils.id.datacenter-id", "2");
            System.setProperty("under.utils.id.worker-id", "3");

            IdGenerator generator = new IdGenerator();
            IdGenerator.IdInfo info = generator.parseId(generator.nextId());

            assertThat(info.getDatacenterId()).isEqualTo(2);
            assertThat(info.getWorkerId()).isEqualTo(3);
            assertThat(generator.isDatacenterIdConfigured()).isTrue();
            assertThat(generator.isWorkerIdConfigured()).isTrue();
            assertThat(generator.isNodeIdFullyConfigured()).isTrue();
        } finally {
            restoreProperty("under.utils.id.datacenter-id", oldDatacenter);
            restoreProperty("under.utils.id.worker-id", oldWorker);
        }
    }

    @Test
    void testConstructor_strictModeRequiresConfiguredNodeIds() {
        String oldDatacenter = System.getProperty("under.utils.id.datacenter-id");
        String oldWorker = System.getProperty("under.utils.id.worker-id");
        String oldRequire = System.getProperty("under.utils.id.require-configured-node-id");
        try {
            System.clearProperty("under.utils.id.datacenter-id");
            System.clearProperty("under.utils.id.worker-id");
            System.setProperty("under.utils.id.require-configured-node-id", "true");

            assertThatThrownBy(IdGenerator::new)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires explicit datacenter and worker IDs");
        } finally {
            restoreProperty("under.utils.id.datacenter-id", oldDatacenter);
            restoreProperty("under.utils.id.worker-id", oldWorker);
            restoreProperty("under.utils.id.require-configured-node-id", oldRequire);
        }
    }

    @Test
    void testConstructor_strictModeAllowsConfiguredNodeIds() {
        String oldDatacenter = System.getProperty("under.utils.id.datacenter-id");
        String oldWorker = System.getProperty("under.utils.id.worker-id");
        String oldRequire = System.getProperty("under.utils.id.require-configured-node-id");
        try {
            System.setProperty("under.utils.id.datacenter-id", "4");
            System.setProperty("under.utils.id.worker-id", "5");
            System.setProperty("under.utils.id.require-configured-node-id", "true");

            IdGenerator generator = new IdGenerator();
            IdGenerator.IdInfo info = generator.parseId(generator.nextId());

            assertThat(info.getDatacenterId()).isEqualTo(4);
            assertThat(info.getWorkerId()).isEqualTo(5);
            assertThat(generator.isNodeIdFullyConfigured()).isTrue();
        } finally {
            restoreProperty("under.utils.id.datacenter-id", oldDatacenter);
            restoreProperty("under.utils.id.worker-id", oldWorker);
            restoreProperty("under.utils.id.require-configured-node-id", oldRequire);
        }
    }

    @Test
    void testConstructor_defaultRejectsInvalidSystemProperties() {
        String oldWorker = System.getProperty("under.utils.id.worker-id");
        try {
            System.setProperty("under.utils.id.worker-id", "32");

            assertThatThrownBy(IdGenerator::new)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("under.utils.id.worker-id");
        } finally {
            restoreProperty("under.utils.id.worker-id", oldWorker);
        }
    }

    @Test
    void testConstructor_invalidDatacenterId() {
        assertThatThrownBy(() -> new IdGenerator(32, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdGenerator(-1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConstructor_invalidWorkerId() {
        assertThatThrownBy(() -> new IdGenerator(1, 32))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdGenerator(1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== nextId() 测试 ====================

    @Test
    void testNextId() {
        IdGenerator generator = new IdGenerator();
        long id = generator.nextId();

        assertThat(id).isPositive();
        assertThat(id).isGreaterThan(0);
    }

    @Test
    void testNextId_uniqueness() {
        IdGenerator generator = new IdGenerator();
        Set<Long> ids = new HashSet<>();

        // 生成 10000 个 ID
        for (int i = 0; i < 10000; i++) {
            long id = generator.nextId();
            assertThat(ids.add(id)).isTrue(); // 每个 ID 都应该是唯一的
        }

        assertThat(ids).hasSize(10000);
    }

    @Test
    void testNextId_increasing() {
        IdGenerator generator = new IdGenerator();

        long id1 = generator.nextId();
        long id2 = generator.nextId();
        long id3 = generator.nextId();

        // ID 应该递增
        assertThat(id2).isGreaterThan(id1);
        assertThat(id3).isGreaterThan(id2);
    }

    // ==================== nextIdStr() 测试 ====================

    @Test
    void testNextIdStr() {
        IdGenerator generator = new IdGenerator();
        String idStr = generator.nextIdStr();

        assertThat(idStr).isNotNull();
        assertThat(idStr).isNotEmpty();
        assertThat(idStr).matches("^\\d+$"); // 应该是纯数字字符串
    }

    @Test
    void testNextIdStr_consistency() {
        IdGenerator generator = new IdGenerator();
        long id = generator.nextId();
        String idStr = generator.nextIdStr();

        // 两次生成的 ID 应该不同
        assertThat(Long.parseLong(idStr)).isNotEqualTo(id);
    }

    // ==================== parseId() 测试 ====================

    @Test
    void testParseId() {
        IdGenerator generator = new IdGenerator(5, 10);
        long id = generator.nextId();

        IdGenerator.IdInfo info = generator.parseId(id);

        assertThat(info).isNotNull();
        assertThat(info.getId()).isEqualTo(id);
        assertThat(info.getTimestamp()).isGreaterThan(0);
        assertThat(info.getDatacenterId()).isEqualTo(5);
        assertThat(info.getWorkerId()).isEqualTo(10);
        assertThat(info.getSequence()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testParseId_multipleIds() {
        IdGenerator generator = new IdGenerator(3, 7);

        for (int i = 0; i < 100; i++) {
            long id = generator.nextId();
            IdGenerator.IdInfo info = generator.parseId(id);

            assertThat(info.getDatacenterId()).isEqualTo(3);
            assertThat(info.getWorkerId()).isEqualTo(7);
        }
    }

    // ==================== 并发测试 ====================

    @Test
    void testConcurrency() throws InterruptedException {
        IdGenerator generator = new IdGenerator();
        int threadCount = 10;
        int idsPerThread = 1000;
        Set<Long> ids = new HashSet<>();
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 用于收集 ID（需要同步）
        Object lock = new Object();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = generator.nextId();
                        synchronized (lock) {
                            ids.add(id);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 验证所有 ID 都是唯一的
        assertThat(ids).hasSize(threadCount * idsPerThread);
    }

    // ==================== 性能测试 ====================

    @Test
    void testPerformance() {
        AtomicLong timestamp = new AtomicLong(System.currentTimeMillis());
        IdGenerator generator = new IdGenerator(1, 1, timestamp::getAndIncrement);
        int count = 100000;

        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            generator.nextId();
        }
        long duration = System.currentTimeMillis() - start;

        // 性能应该很好
        assertThat(duration).isLessThan(5000);
    }

    @Test
    void testNextId_rejectsClockBackwards() {
        AtomicLong timestamp = new AtomicLong(System.currentTimeMillis());
        IdGenerator generator = new IdGenerator(1, 1, timestamp::get);

        generator.nextId();
        timestamp.decrementAndGet();

        assertThatThrownBy(generator::nextId)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Clock moved backwards");
    }

    // ==================== 边界测试 ====================

    @Test
    void testBoundary_maxDatacenterAndWorker() {
        // 测试最大的数据中心 ID 和机器 ID
        IdGenerator generator = new IdGenerator(31, 31);
        long id = generator.nextId();

        IdGenerator.IdInfo info = generator.parseId(id);
        assertThat(info.getDatacenterId()).isEqualTo(31);
        assertThat(info.getWorkerId()).isEqualTo(31);
    }

    @Test
    void testBoundary_minDatacenterAndWorker() {
        // 测试最小的数据中心 ID 和机器 ID
        IdGenerator generator = new IdGenerator(0, 0);
        long id = generator.nextId();

        IdGenerator.IdInfo info = generator.parseId(id);
        assertThat(info.getDatacenterId()).isEqualTo(0);
        assertThat(info.getWorkerId()).isEqualTo(0);
    }

    @Test
    void testSequence_overflow() {
        // 在同一毫秒内生成大量 ID，测试序列号溢出
        IdGenerator generator = new IdGenerator();

        // 快速生成 5000 个 ID（可能在同一毫秒内）
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            ids.add(generator.nextId());
        }

        // 所有 ID 都应该唯一
        assertThat(ids).hasSize(5000);
    }

    // ==================== 综合测试 ====================

    @Test
    void testMultipleGenerators() {
        // 多个生成器使用不同的机器 ID
        IdGenerator gen1 = new IdGenerator(0, 1);
        IdGenerator gen2 = new IdGenerator(0, 2);
        IdGenerator gen3 = new IdGenerator(1, 1);

        long id1 = gen1.nextId();
        long id2 = gen2.nextId();
        long id3 = gen3.nextId();

        // 不同生成器生成的 ID 应该不同
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id2).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id3);

        // 验证机器 ID
        assertThat(gen1.parseId(id1).getWorkerId()).isEqualTo(1);
        assertThat(gen2.parseId(id2).getWorkerId()).isEqualTo(2);
        assertThat(gen3.parseId(id3).getDatacenterId()).isEqualTo(1);
    }

    @Test
    void testIdInfo_toString() {
        IdGenerator generator = new IdGenerator(5, 10);
        long id = generator.nextId();
        IdGenerator.IdInfo info = generator.parseId(id);

        String str = info.toString();
        assertThat(str).contains("id=");
        assertThat(str).contains("timestamp=");
        assertThat(str).contains("datacenterId=5");
        assertThat(str).contains("workerId=10");
    }

    private static void restoreProperty(String name, String oldValue) {
        if (oldValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, oldValue);
        }
    }
}
