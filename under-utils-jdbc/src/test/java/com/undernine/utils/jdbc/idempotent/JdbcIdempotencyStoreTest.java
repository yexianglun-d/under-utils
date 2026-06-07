package com.undernine.utils.jdbc.idempotent;

import com.undernine.utils.spring.idempotent.IdempotencyExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcIdempotencyStoreTest {

    private MutableClock clock;
    private JdbcIdempotencyStore store;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:idem_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
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
        clock = new MutableClock(Instant.parse("2026-06-05T00:00:00Z"));
        store = new JdbcIdempotencyStore(
                jdbcTemplate,
                null,
                JdbcIdempotencyStoreOptions.defaults(),
                clock
        );
    }

    @Test
    void shouldReturnInProgressWhenFirstExecutionStillProcessing() {
        IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(30), String.class);
        IdempotencyExecution duplicate = store.begin("order:1", Duration.ofSeconds(30), String.class);

        assertThat(first.isAcquired()).isTrue();
        assertThat(first.getExecutionToken()).isNotBlank();
        assertThat(duplicate.isInProgress()).isTrue();
    }

    @Test
    void shouldReturnCompletedResultAfterFirstExecutionCompleted() {
        IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(30), String.class);

        boolean completed = store.complete(
                "order:1",
                first.getExecutionToken(),
                "created",
                String.class,
                Duration.ofMinutes(5)
        );
        IdempotencyExecution duplicate = store.begin("order:1", Duration.ofSeconds(30), String.class);

        assertThat(completed).isTrue();
        assertThat(duplicate.isCompleted()).isTrue();
        assertThat(duplicate.getResult()).isEqualTo("created");
    }

    @Test
    void shouldLetExpiredProcessingOwnerBeReplaced() {
        IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(1), String.class);
        clock.advance(Duration.ofSeconds(2));

        IdempotencyExecution second = store.begin("order:1", Duration.ofSeconds(30), String.class);
        boolean oldOwnerCompleted = store.complete(
                "order:1",
                first.getExecutionToken(),
                "old",
                String.class,
                Duration.ofMinutes(5)
        );
        boolean newOwnerCompleted = store.complete(
                "order:1",
                second.getExecutionToken(),
                "new",
                String.class,
                Duration.ofMinutes(5)
        );

        assertThat(second.isAcquired()).isTrue();
        assertThat(oldOwnerCompleted).isFalse();
        assertThat(newOwnerCompleted).isTrue();
        assertThat(store.begin("order:1", Duration.ofSeconds(30), String.class).getResult()).isEqualTo("new");
    }

    @Test
    void shouldReleaseProcessingOwnerForRetry() {
        IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(30), String.class);

        store.release("order:1", first.getExecutionToken());
        IdempotencyExecution retry = store.begin("order:1", Duration.ofSeconds(30), String.class);

        assertThat(retry.isAcquired()).isTrue();
        assertThat(retry.getExecutionToken()).isNotEqualTo(first.getExecutionToken());
    }

    @Test
    void shouldSupportNullAndVoidResult() {
        IdempotencyExecution nullOwner = store.begin("nullable", Duration.ofSeconds(30), String.class);
        IdempotencyExecution voidOwner = store.begin("void", Duration.ofSeconds(30), Void.TYPE);

        assertThat(store.complete("nullable", nullOwner.getExecutionToken(), null, String.class, Duration.ofMinutes(5)))
                .isTrue();
        assertThat(store.complete("void", voidOwner.getExecutionToken(), null, Void.TYPE, Duration.ofMinutes(5)))
                .isTrue();

        assertThat(store.begin("nullable", Duration.ofSeconds(30), String.class).getResult()).isNull();
        assertThat(store.begin("void", Duration.ofSeconds(30), Void.TYPE).getResult()).isNull();
    }

    @Test
    void shouldCleanupExpiredRecords() {
        IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(1), String.class);
        assertThat(store.complete("order:1", first.getExecutionToken(), "created", String.class, Duration.ofSeconds(1)))
                .isTrue();
        clock.advance(Duration.ofSeconds(2));

        assertThat(store.cleanupExpired()).isEqualTo(1);
        assertThat(store.begin("order:1", Duration.ofSeconds(30), String.class).isAcquired()).isTrue();
    }

    @Test
    void shouldRejectUnsafeTableName() {
        assertThatThrownBy(() -> JdbcIdempotencyStoreOptions.builder()
                .tableName("under_utils_idempotency where 1=1")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableName");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
