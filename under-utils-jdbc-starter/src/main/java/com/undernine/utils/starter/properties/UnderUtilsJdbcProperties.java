package com.undernine.utils.starter.properties;

import com.undernine.utils.jdbc.idempotent.JdbcIdempotencyStoreOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Under-Utils JDBC 自动配置属性。
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
@ConfigurationProperties(prefix = "under.utils.idempotent.jdbc")
public class UnderUtilsJdbcProperties {

    /**
     * 幂等状态表名。只支持未加引号的 table 或 schema.table，避免 SQL 注入。
     */
    private String tableName = JdbcIdempotencyStoreOptions.DEFAULT_TABLE_NAME;

    /**
     * begin 阶段碰到并发变更时的最大重试次数。
     */
    private int maxBeginRetries = JdbcIdempotencyStoreOptions.DEFAULT_MAX_BEGIN_RETRIES;

    /**
     * 是否启动过期幂等记录清理任务。仅在 {@code under.utils.idempotent.store=jdbc} 时生效。
     */
    private boolean cleanupEnabled = true;

    /**
     * 清理任务首次执行延迟。
     */
    private Duration cleanupInitialDelay = Duration.ofMinutes(1);

    /**
     * 清理任务执行间隔。
     */
    private Duration cleanupInterval = Duration.ofMinutes(1);

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getMaxBeginRetries() {
        return maxBeginRetries;
    }

    public void setMaxBeginRetries(int maxBeginRetries) {
        this.maxBeginRetries = maxBeginRetries;
    }

    public boolean isCleanupEnabled() {
        return cleanupEnabled;
    }

    public void setCleanupEnabled(boolean cleanupEnabled) {
        this.cleanupEnabled = cleanupEnabled;
    }

    public Duration getCleanupInitialDelay() {
        return cleanupInitialDelay;
    }

    public void setCleanupInitialDelay(Duration cleanupInitialDelay) {
        this.cleanupInitialDelay = cleanupInitialDelay;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }
}
