package com.undernine.utils.jdbc.idempotent;

import com.undernine.utils.spring.idempotent.IdempotencyException;
import com.undernine.utils.spring.idempotent.IdempotencyExecution;
import com.undernine.utils.spring.idempotent.IdempotencyResultCodec;
import com.undernine.utils.spring.idempotent.JacksonIdempotencyResultCodec;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcOperations;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 基于关系型数据库的业务幂等状态存储。
 * <p>
 * 该实现不自动建表，调用方需要按 README 中的 DDL 显式创建表。所有业务 key 和结果内容均使用
 * PreparedStatement 参数绑定，唯一拼接的表名来自 {@link JdbcIdempotencyStoreOptions} 的白名单校验。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
public class JdbcIdempotencyStore implements com.undernine.utils.spring.idempotent.IdempotencyStore {

    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final JdbcOperations jdbcOperations;
    private final IdempotencyResultCodec resultCodec;
    private final JdbcIdempotencyStoreOptions options;
    private final Clock clock;
    private final String insertProcessingSql;
    private final String selectSql;
    private final String updateExpiredToProcessingSql;
    private final String updateCompletedSql;
    private final String releaseSql;
    private final String cleanupExpiredSql;

    public JdbcIdempotencyStore(JdbcOperations jdbcOperations) {
        this(jdbcOperations, new JacksonIdempotencyResultCodec());
    }

    public JdbcIdempotencyStore(JdbcOperations jdbcOperations, IdempotencyResultCodec resultCodec) {
        this(jdbcOperations, resultCodec, JdbcIdempotencyStoreOptions.defaults());
    }

    public JdbcIdempotencyStore(JdbcOperations jdbcOperations,
                                IdempotencyResultCodec resultCodec,
                                JdbcIdempotencyStoreOptions options) {
        this(jdbcOperations, resultCodec, options, Clock.systemUTC());
    }

    JdbcIdempotencyStore(JdbcOperations jdbcOperations,
                         IdempotencyResultCodec resultCodec,
                         JdbcIdempotencyStoreOptions options,
                         Clock clock) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "jdbcOperations must not be null");
        this.resultCodec = resultCodec == null ? new JacksonIdempotencyResultCodec() : resultCodec;
        this.options = options == null ? JdbcIdempotencyStoreOptions.defaults() : options;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        String tableName = this.options.getTableName();
        this.insertProcessingSql = "insert into " + tableName
                + " (idem_key, status, execution_token, result_payload, expire_at, created_at, updated_at)"
                + " values (?, ?, ?, ?, ?, ?, ?)";
        this.selectSql = "select status, execution_token, result_payload, expire_at from " + tableName
                + " where idem_key = ?";
        this.updateExpiredToProcessingSql = "update " + tableName
                + " set status = ?, execution_token = ?, result_payload = ?, expire_at = ?, updated_at = ?"
                + " where idem_key = ? and expire_at <= ?";
        this.updateCompletedSql = "update " + tableName
                + " set status = ?, execution_token = ?, result_payload = ?, expire_at = ?, updated_at = ?"
                + " where idem_key = ? and status = ? and execution_token = ? and expire_at > ?";
        this.releaseSql = "delete from " + tableName
                + " where idem_key = ? and status = ? and execution_token = ?";
        this.cleanupExpiredSql = "delete from " + tableName + " where expire_at <= ?";
    }

    @Override
    public IdempotencyExecution begin(String key, Duration processingTtl, Type resultType) {
        String storeKey = storeKey(key);
        Duration ttl = normalizeTtl(processingTtl);
        for (int i = 0; i < options.getMaxBeginRetries(); i++) {
            Instant now = clock.instant();
            String executionToken = UUID.randomUUID().toString();
            if (insertProcessing(storeKey, executionToken, now, ttl)) {
                return IdempotencyExecution.acquired(executionToken);
            }

            Record record = find(storeKey);
            if (record == null) {
                continue;
            }
            if (!record.expireAt().isAfter(now)) {
                if (claimExpired(storeKey, executionToken, now, ttl)) {
                    return IdempotencyExecution.acquired(executionToken);
                }
                continue;
            }
            if (STATUS_COMPLETED.equals(record.status())) {
                return IdempotencyExecution.completed(resultCodec.deserialize(record.resultPayload(), resultType));
            }
            return IdempotencyExecution.inProgress();
        }
        throw new IdempotencyException("Failed to acquire idempotent key after JDBC retries");
    }

    @Override
    public boolean complete(String key,
                            String executionToken,
                            Object result,
                            Type resultType,
                            Duration resultTtl) {
        String storeKey = storeKey(key);
        Instant now = clock.instant();
        String resultPayload = resultCodec.serialize(result, resultType);
        int updated = jdbcOperations.update(
                updateCompletedSql,
                STATUS_COMPLETED,
                null,
                resultPayload,
                timestamp(now.plus(normalizeTtl(resultTtl))),
                timestamp(now),
                storeKey,
                STATUS_PROCESSING,
                executionToken,
                timestamp(now)
        );
        return updated == 1;
    }

    @Override
    public void release(String key, String executionToken) {
        jdbcOperations.update(releaseSql, storeKey(key), STATUS_PROCESSING, executionToken);
    }

    /**
     * 清理已过期的幂等记录。
     *
     * @return 删除行数
     */
    public int cleanupExpired() {
        return jdbcOperations.update(cleanupExpiredSql, timestamp(clock.instant()));
    }

    private boolean insertProcessing(String storeKey, String executionToken, Instant now, Duration ttl) {
        try {
            jdbcOperations.update(
                    insertProcessingSql,
                    storeKey,
                    STATUS_PROCESSING,
                    executionToken,
                    null,
                    timestamp(now.plus(ttl)),
                    timestamp(now),
                    timestamp(now)
            );
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    private boolean claimExpired(String storeKey, String executionToken, Instant now, Duration ttl) {
        int updated = jdbcOperations.update(
                updateExpiredToProcessingSql,
                STATUS_PROCESSING,
                executionToken,
                null,
                timestamp(now.plus(ttl)),
                timestamp(now),
                storeKey,
                timestamp(now)
        );
        return updated == 1;
    }

    private Record find(String storeKey) {
        List<Record> records = jdbcOperations.query(selectSql, this::mapRecord, storeKey);
        return records.isEmpty() ? null : records.get(0);
    }

    private Record mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new Record(
                resultSet.getString("status"),
                resultSet.getString("execution_token"),
                resultSet.getString("result_payload"),
                resultSet.getTimestamp("expire_at").toInstant()
        );
    }

    private String storeKey(String key) {
        return options.getKeyPrefix() + Objects.requireNonNull(key, "key must not be null");
    }

    private Duration normalizeTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Duration.ofMillis(1);
        }
        return ttl;
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private record Record(String status, String executionToken, String resultPayload, Instant expireAt) {
    }
}
