package com.undernine.utils.redis.idempotent;

import com.undernine.utils.spring.idempotent.IdempotencyExecution;
import com.undernine.utils.spring.idempotent.JacksonIdempotencyResultCodec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisIdempotencyStoreTest {

    @Test
    void shouldAcquireWhenRedisKeyIsAbsent() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = bucket(redissonClient);
        when(bucket.setIfAbsent(anyString(), eq(Duration.ofSeconds(1)))).thenReturn(true);
        RedisIdempotencyStore store = new RedisIdempotencyStore(redissonClient);

        IdempotencyExecution execution = store.begin("order:1", Duration.ofSeconds(1), String.class);

        assertThat(execution.isAcquired()).isTrue();
        assertThat(execution.getExecutionToken()).isNotBlank();
        verify(redissonClient).getBucket("under-utils:idempotent:order:1", StringCodec.INSTANCE);
    }

    @Test
    void shouldReturnInProgressWhenRedisRecordIsProcessing() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = bucket(redissonClient);
        when(bucket.setIfAbsent(anyString(), eq(Duration.ofSeconds(1)))).thenReturn(false);
        when(bucket.get()).thenReturn("{\"status\":\"PROCESSING\"}");
        RedisIdempotencyStore store = new RedisIdempotencyStore(redissonClient);

        IdempotencyExecution execution = store.begin("order:1", Duration.ofSeconds(1), String.class);

        assertThat(execution.isInProgress()).isTrue();
    }

    @Test
    void shouldReturnCompletedResultWhenRedisRecordIsCompleted() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = bucket(redissonClient);
        when(bucket.setIfAbsent(anyString(), eq(Duration.ofSeconds(1)))).thenReturn(false);
        when(bucket.get()).thenReturn("{\"status\":\"COMPLETED\",\"resultPayload\":\"\\\"created\\\"\"}");
        RedisIdempotencyStore store = new RedisIdempotencyStore(redissonClient);

        IdempotencyExecution execution = store.begin("order:1", Duration.ofSeconds(1), String.class);

        assertThat(execution.isCompleted()).isTrue();
        assertThat(execution.getResult()).isEqualTo("created");
    }

    @Test
    void shouldWriteCompletedRecordWithResultTtl() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        bucket(redissonClient);
        RScript script = script(redissonClient);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                eq(List.of((Object) "idem:order:1")),
                anyString(),
                anyString(),
                eq(String.valueOf(Duration.ofMinutes(5).toMillis()))
        )).thenReturn(1L);
        RedisIdempotencyStore store = new RedisIdempotencyStore(
                redissonClient,
                new JacksonIdempotencyResultCodec(),
                "idem:");

        boolean completed = store.complete("order:1", "token-1", "created", String.class, Duration.ofMinutes(5));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(script).eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                eq(List.of((Object) "idem:order:1")),
                anyString(),
                payloadCaptor.capture(),
                eq(String.valueOf(Duration.ofMinutes(5).toMillis()))
        );
        assertThat(completed).isTrue();
        assertThat(payloadCaptor.getValue()).contains("\"status\":\"COMPLETED\"");
        assertThat(payloadCaptor.getValue()).contains("\\\"created\\\"");
    }

    @Test
    void shouldReturnFalseWhenCompletionOwnerDoesNotMatch() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        bucket(redissonClient);
        RScript script = script(redissonClient);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                eq(List.of((Object) "idem:order:1")),
                anyString(),
                anyString(),
                eq(String.valueOf(Duration.ofMinutes(5).toMillis()))
        )).thenReturn(0L);
        RedisIdempotencyStore store = new RedisIdempotencyStore(redissonClient, null, "idem:");

        boolean completed = store.complete("order:1", "stale-token", "created", String.class, Duration.ofMinutes(5));

        assertThat(completed).isFalse();
    }

    @Test
    void shouldReleaseRedisKey() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        bucket(redissonClient);
        RScript script = script(redissonClient);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                eq(List.of((Object) "under-utils:idempotent:order:1")),
                anyString()
        )).thenReturn(1L);
        RedisIdempotencyStore store = new RedisIdempotencyStore(redissonClient);

        store.release("order:1", "token-1");

        verify(script).eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                eq(List.of((Object) "under-utils:idempotent:order:1")),
                anyString()
        );
    }

    @SuppressWarnings("unchecked")
    private RBucket<String> bucket(RedissonClient redissonClient) {
        RBucket<String> bucket = mock(RBucket.class);
            when(redissonClient.<String>getBucket(anyString(), same(StringCodec.INSTANCE))).thenReturn(bucket);
        return bucket;
    }

    private RScript script(RedissonClient redissonClient) {
        RScript script = mock(RScript.class);
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
        return script;
    }
}
