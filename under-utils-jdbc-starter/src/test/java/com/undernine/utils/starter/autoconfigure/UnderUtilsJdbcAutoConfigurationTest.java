package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.jdbc.idempotent.JdbcIdempotencyStore;
import com.undernine.utils.spring.aspect.IdempotentAspect;
import com.undernine.utils.spring.idempotent.IdempotencyExecution;
import com.undernine.utils.spring.idempotent.IdempotencyStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UnderUtilsJdbcAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    UnderUtilsSpringAutoConfiguration.class,
                    UnderUtilsJdbcAutoConfiguration.class));

    @Test
    void shouldAutoConfigureJdbcIdempotencyStoreWhenRequested() {
        contextRunner
                .withBean(JdbcOperations.class, () -> new JdbcTemplate(new DriverManagerDataSource(
                        "jdbc:h2:mem:starter_idem;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        ""
                )))
                .withPropertyValues("under.utils.idempotent.store=jdbc")
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotencyStore.class);
                    assertThat(context.getBean(IdempotencyStore.class)).isInstanceOf(JdbcIdempotencyStore.class);
                    assertThat(context).hasSingleBean(JdbcIdempotencyCleanupScheduler.class);
                    assertThat(context).hasSingleBean(IdempotentAspect.class);
                });
    }

    @Test
    void shouldBackOffWhenUserIdempotencyStoreExists() {
        IdempotencyStore customStore = new IdempotencyStore() {
            @Override
            public IdempotencyExecution begin(String key, Duration processingTtl, Type resultType) {
                return IdempotencyExecution.acquired("owner");
            }

            @Override
            public boolean complete(String key,
                                    String executionToken,
                                    Object result,
                                    Type resultType,
                                    Duration resultTtl) {
                return true;
            }
        };

        contextRunner
                .withBean(JdbcOperations.class, () -> new JdbcTemplate(new DriverManagerDataSource(
                        "jdbc:h2:mem:starter_backoff;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        ""
                )))
                .withBean(IdempotencyStore.class, () -> customStore)
                .withPropertyValues("under.utils.idempotent.store=jdbc")
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotencyStore.class);
                    assertThat(context.getBean(IdempotencyStore.class)).isSameAs(customStore);
                });
    }

    @Test
    void shouldNotAutoConfigureCleanupSchedulerWhenJdbcStoreNotSelected() {
        JdbcOperations jdbcOperations = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:starter_not_jdbc;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        ));

        contextRunner
                .withBean(JdbcOperations.class, () -> jdbcOperations)
                .withBean(JdbcIdempotencyStore.class, () -> new JdbcIdempotencyStore(jdbcOperations))
                .run(context -> assertThat(context).doesNotHaveBean(JdbcIdempotencyCleanupScheduler.class));
    }

    @Test
    void shouldFailWhenJdbcStoreRequestedWithoutJdbcOperations() {
        contextRunner
                .withPropertyValues("under.utils.idempotent.store=jdbc")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldDisableJdbcCleanupSchedulerWhenConfigured() {
        contextRunner
                .withBean(JdbcOperations.class, () -> new JdbcTemplate(new DriverManagerDataSource(
                        "jdbc:h2:mem:starter_cleanup_disabled;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        ""
                )))
                .withPropertyValues(
                        "under.utils.idempotent.store=jdbc",
                        "under.utils.idempotent.jdbc.cleanup-enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotencyStore.class);
                    assertThat(context).doesNotHaveBean(JdbcIdempotencyCleanupScheduler.class);
                });
    }

    @Test
    void shouldCleanupExpiredJdbcIdempotencyRows() {
        contextRunner
                .withBean(JdbcOperations.class, () -> new JdbcTemplate(new DriverManagerDataSource(
                        "jdbc:h2:mem:starter_cleanup;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        ""
                )))
                .withPropertyValues("under.utils.idempotent.store=jdbc")
                .run(context -> {
                    JdbcOperations jdbcOperations = context.getBean(JdbcOperations.class);
                    jdbcOperations.execute("""
                            create table under_utils_idempotency (
                                idem_key varchar(512) primary key,
                                status varchar(32) not null,
                                execution_token varchar(128),
                                result_payload clob,
                                expire_at timestamp not null,
                                created_at timestamp not null,
                                updated_at timestamp not null
                            )
                            """);
                    Instant now = Instant.now();
                    jdbcOperations.update(
                            "insert into under_utils_idempotency"
                                    + " (idem_key, status, execution_token, result_payload, expire_at, created_at, updated_at)"
                                    + " values (?, ?, ?, ?, ?, ?, ?)",
                            "expired",
                            "COMPLETED",
                            null,
                            null,
                            Timestamp.from(now.minus(Duration.ofDays(1))),
                            Timestamp.from(now.minus(Duration.ofDays(2))),
                            Timestamp.from(now.minus(Duration.ofDays(2)))
                    );

                    int deleted = context.getBean(JdbcIdempotencyCleanupScheduler.class).cleanupOnce();

                    assertThat(deleted).isEqualTo(1);
                    assertThat(jdbcOperations.queryForObject(
                            "select count(*) from under_utils_idempotency", Integer.class)).isZero();
                });
    }

    @Test
    void shouldFailFastOnUnsafeJdbcTableName() {
        contextRunner
                .withBean(JdbcOperations.class, () -> new JdbcTemplate(new DriverManagerDataSource(
                        "jdbc:h2:mem:starter_invalid_table;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        ""
                )))
                .withPropertyValues(
                        "under.utils.idempotent.store=jdbc",
                        "under.utils.idempotent.jdbc.table-name=idem where 1=1"
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
