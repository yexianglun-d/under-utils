package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.jdbc.idempotent.JdbcIdempotencyStore;
import com.undernine.utils.jdbc.idempotent.JdbcIdempotencyStoreOptions;
import com.undernine.utils.spring.idempotent.IdempotencyResultCodec;
import com.undernine.utils.spring.idempotent.IdempotencyStore;
import com.undernine.utils.starter.properties.UnderUtilsJdbcProperties;
import com.undernine.utils.starter.properties.UnderUtilsProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Under-Utils JDBC 自动配置入口。
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
@AutoConfiguration(after = UnderUtilsSpringAutoConfiguration.class)
@EnableConfigurationProperties({UnderUtilsProperties.class, UnderUtilsJdbcProperties.class})
@ConditionalOnClass({JdbcOperations.class, JdbcIdempotencyStore.class})
@ConditionalOnBean(JdbcOperations.class)
@ConditionalOnProperty(prefix = "under.utils.idempotent", name = "store", havingValue = "jdbc")
public class UnderUtilsJdbcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotencyStore.class)
    public JdbcIdempotencyStore jdbcIdempotencyStore(JdbcOperations jdbcOperations,
                                                     IdempotencyResultCodec resultCodec,
                                                     UnderUtilsProperties properties,
                                                     UnderUtilsJdbcProperties jdbcProperties) {
        JdbcIdempotencyStoreOptions options = JdbcIdempotencyStoreOptions.builder()
                .tableName(jdbcProperties.getTableName())
                .keyPrefix(properties.getIdempotent().getKeyPrefix())
                .maxBeginRetries(jdbcProperties.getMaxBeginRetries())
                .build();
        return new JdbcIdempotencyStore(jdbcOperations, resultCodec, options);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    @ConditionalOnBean(JdbcIdempotencyStore.class)
    @ConditionalOnProperty(prefix = "under.utils.idempotent.jdbc", name = "cleanup-enabled",
            havingValue = "true", matchIfMissing = true)
    public JdbcIdempotencyCleanupScheduler jdbcIdempotencyCleanupScheduler(
            JdbcIdempotencyStore idempotencyStore,
            UnderUtilsJdbcProperties properties) {
        return new JdbcIdempotencyCleanupScheduler(
                idempotencyStore,
                properties.getCleanupInitialDelay(),
                properties.getCleanupInterval()
        );
    }
}
