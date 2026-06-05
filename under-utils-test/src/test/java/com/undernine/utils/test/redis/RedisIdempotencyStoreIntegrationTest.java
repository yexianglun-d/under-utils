package com.undernine.utils.test.redis;

import com.undernine.utils.redis.idempotent.RedisIdempotencyStore;
import com.undernine.utils.spring.idempotent.IdempotencyExecution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisIdempotencyStoreIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    private static RedissonClient redissonClient;

    @BeforeAll
    static void createRedissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        redissonClient = Redisson.create(config);
    }

    @AfterAll
    static void shutdownRedissonClient() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @BeforeEach
    void cleanRedis() {
        redissonClient.getKeys().flushdb();
    }

    @Test
    void redisIdempotencyStoreReturnsCompletedResultAndExpiresIt() throws Exception {
        RedisIdempotencyStore store = new RedisIdempotencyStore(redissonClient, null, "it:idem:");

        IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(5), String.class);
        IdempotencyExecution inProgress = store.begin("order:1", Duration.ofSeconds(5), String.class);
        assertThat(store.complete("order:1", first.getExecutionToken(), "created", String.class,
                Duration.ofMillis(200))).isTrue();
        IdempotencyExecution completed = store.begin("order:1", Duration.ofSeconds(5), String.class);

        assertThat(first.isAcquired()).isTrue();
        assertThat(inProgress.isInProgress()).isTrue();
        assertThat(completed.isCompleted()).isTrue();
        assertThat(completed.getResult()).isEqualTo("created");

        Thread.sleep(260L);

        assertThat(store.begin("order:1", Duration.ofSeconds(5), String.class).isAcquired()).isTrue();
    }

    @Test
    void redisIdempotencyStoreRejectsExpiredOwnerCompletion() throws Exception {
        RedisIdempotencyStore store = new RedisIdempotencyStore(redissonClient, null, "it:idem:");

        IdempotencyExecution first = store.begin("order:2", Duration.ofMillis(50), String.class);
        Thread.sleep(80L);
        IdempotencyExecution second = store.begin("order:2", Duration.ofSeconds(5), String.class);

        assertThat(first.isAcquired()).isTrue();
        assertThat(second.isAcquired()).isTrue();
        assertThat(store.complete("order:2", first.getExecutionToken(), "old", String.class,
                Duration.ofSeconds(5))).isFalse();
        assertThat(store.complete("order:2", second.getExecutionToken(), "new", String.class,
                Duration.ofSeconds(5))).isTrue();

        IdempotencyExecution completed = store.begin("order:2", Duration.ofSeconds(5), String.class);
        assertThat(completed.isCompleted()).isTrue();
        assertThat(completed.getResult()).isEqualTo("new");
    }
}
