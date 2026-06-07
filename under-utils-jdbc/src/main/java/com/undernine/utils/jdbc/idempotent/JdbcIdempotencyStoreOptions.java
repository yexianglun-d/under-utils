package com.undernine.utils.jdbc.idempotent;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * JDBC 幂等存储配置。
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
public final class JdbcIdempotencyStoreOptions {

    public static final String DEFAULT_TABLE_NAME = "under_utils_idempotency";
    public static final String DEFAULT_KEY_PREFIX = "under-utils:idempotent:";
    public static final int DEFAULT_MAX_BEGIN_RETRIES = 3;

    private static final Pattern TABLE_NAME_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");

    private final String tableName;
    private final String keyPrefix;
    private final int maxBeginRetries;

    private JdbcIdempotencyStoreOptions(Builder builder) {
        this.tableName = validateTableName(builder.tableName);
        this.keyPrefix = builder.keyPrefix == null ? DEFAULT_KEY_PREFIX : builder.keyPrefix;
        if (builder.maxBeginRetries <= 0) {
            throw new IllegalArgumentException("maxBeginRetries must be greater than 0");
        }
        this.maxBeginRetries = builder.maxBeginRetries;
    }

    public static JdbcIdempotencyStoreOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTableName() {
        return tableName;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public int getMaxBeginRetries() {
        return maxBeginRetries;
    }

    private static String validateTableName(String tableName) {
        String actual = Objects.requireNonNull(tableName, "tableName must not be null").trim();
        if (!TABLE_NAME_PATTERN.matcher(actual).matches()) {
            throw new IllegalArgumentException("tableName must be an unquoted table name or schema.table name");
        }
        return actual;
    }

    public static final class Builder {
        private String tableName = DEFAULT_TABLE_NAME;
        private String keyPrefix = DEFAULT_KEY_PREFIX;
        private int maxBeginRetries = DEFAULT_MAX_BEGIN_RETRIES;

        private Builder() {
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        public Builder maxBeginRetries(int maxBeginRetries) {
            this.maxBeginRetries = maxBeginRetries;
            return this;
        }

        public JdbcIdempotencyStoreOptions build() {
            return new JdbcIdempotencyStoreOptions(this);
        }
    }
}
