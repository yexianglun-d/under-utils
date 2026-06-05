package com.undernine.utils.redis.idempotent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undernine.utils.spring.idempotent.IdempotencyException;
import com.undernine.utils.spring.idempotent.IdempotencyExecution;
import com.undernine.utils.spring.idempotent.IdempotencyResultCodec;
import com.undernine.utils.spring.idempotent.IdempotencyStore;
import com.undernine.utils.spring.idempotent.JacksonIdempotencyResultCodec;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 基于 Redisson 的分布式业务幂等状态存储。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String DEFAULT_PREFIX = "under-utils:idempotent:";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final int MAX_BEGIN_RETRIES = 3;
    private static final String COMPLETE_IF_OWNER_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              redis.call('psetex', KEYS[1], tonumber(ARGV[3]), ARGV[2])
              return 1
            end
            return 0
            """;
    private static final String RELEASE_IF_OWNER_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            end
            return 0
            """;

    private final RedissonClient redissonClient;
    private final IdempotencyResultCodec resultCodec;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public RedisIdempotencyStore(RedissonClient redissonClient) {
        this(redissonClient, new JacksonIdempotencyResultCodec(), DEFAULT_PREFIX);
    }

    public RedisIdempotencyStore(RedissonClient redissonClient, IdempotencyResultCodec resultCodec) {
        this(redissonClient, resultCodec, DEFAULT_PREFIX);
    }

    public RedisIdempotencyStore(RedissonClient redissonClient,
                                 IdempotencyResultCodec resultCodec,
                                 String keyPrefix) {
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient must not be null");
        this.resultCodec = resultCodec == null ? new JacksonIdempotencyResultCodec() : resultCodec;
        this.keyPrefix = keyPrefix == null ? DEFAULT_PREFIX : keyPrefix;
        this.objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public IdempotencyExecution begin(String key, Duration processingTtl, Type resultType) {
        Duration ttl = normalizeTtl(processingTtl);
        RBucket<String> bucket = bucket(key);
        for (int i = 0; i < MAX_BEGIN_RETRIES; i++) {
            String executionToken = UUID.randomUUID().toString();
            boolean acquired = bucket.setIfAbsent(writeRecord(Record.processing(executionToken)), ttl);
            if (acquired) {
                return IdempotencyExecution.acquired(executionToken);
            }

            String payload = bucket.get();
            if (payload == null) {
                continue;
            }
            Record record = readRecord(payload);
            if (STATUS_COMPLETED.equals(record.getStatus())) {
                return IdempotencyExecution.completed(resultCodec.deserialize(record.getResultPayload(), resultType));
            }
            return IdempotencyExecution.inProgress();
        }
        throw new IdempotencyException("Failed to acquire idempotent key after Redis retries");
    }

    @Override
    public boolean complete(String key,
                            String executionToken,
                            Object result,
                            Type resultType,
                            Duration resultTtl) {
        String redisKey = redisKey(key);
        String expectedPayload = writeRecord(Record.processing(executionToken));
        String completedPayload = writeRecord(Record.completed(resultCodec.serialize(result, resultType)));
        Long updated = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                COMPLETE_IF_OWNER_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of((Object) redisKey),
                expectedPayload,
                completedPayload,
                String.valueOf(normalizeTtl(resultTtl).toMillis())
        );
        return updated != null && updated == 1L;
    }

    @Override
    public void release(String key, String executionToken) {
        Long deleted = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                RELEASE_IF_OWNER_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of((Object) redisKey(key)),
                writeRecord(Record.processing(executionToken))
        );
        if (deleted == null) {
            throw new IdempotencyException("Failed to release idempotent key in Redis");
        }
    }

    private RBucket<String> bucket(String key) {
        return redissonClient.getBucket(redisKey(key), StringCodec.INSTANCE);
    }

    private String redisKey(String key) {
        return keyPrefix + key;
    }

    private Duration normalizeTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Duration.ofMillis(1);
        }
        return ttl;
    }

    private String writeRecord(Record record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException ex) {
            throw new IdempotencyException("Failed to serialize idempotent record", ex);
        }
    }

    private Record readRecord(String payload) {
        try {
            return objectMapper.readValue(payload, Record.class);
        } catch (JsonProcessingException ex) {
            throw new IdempotencyException("Failed to deserialize idempotent record", ex);
        }
    }

    private static final class Record {
        private String status;
        private String executionToken;
        private String resultPayload;

        private Record() {
        }

        private Record(String status, String executionToken, String resultPayload) {
            this.status = status;
            this.executionToken = executionToken;
            this.resultPayload = resultPayload;
        }

        private static Record processing(String executionToken) {
            return new Record(STATUS_PROCESSING, executionToken, null);
        }

        private static Record completed(String resultPayload) {
            return new Record(STATUS_COMPLETED, null, resultPayload);
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getExecutionToken() {
            return executionToken;
        }

        public void setExecutionToken(String executionToken) {
            this.executionToken = executionToken;
        }

        public String getResultPayload() {
            return resultPayload;
        }

        public void setResultPayload(String resultPayload) {
            this.resultPayload = resultPayload;
        }
    }
}
