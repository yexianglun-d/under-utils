package com.undernine.utils.spring.idempotent;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalIdempotencyStoreTest {

    @Test
    void shouldReturnInProgressBeforeFirstCallCompletes() {
        try (LocalIdempotencyStore store = new LocalIdempotencyStore()) {
            IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(1), String.class);
            IdempotencyExecution second = store.begin("order:1", Duration.ofSeconds(1), String.class);

            assertThat(first.isAcquired()).isTrue();
            assertThat(first.getExecutionToken()).isNotBlank();
            assertThat(second.isInProgress()).isTrue();
        }
    }

    @Test
    void shouldReturnCompletedResultAfterFirstCallCompletes() {
        try (LocalIdempotencyStore store = new LocalIdempotencyStore()) {
            IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(1), String.class);
            assertThat(first.isAcquired()).isTrue();

            assertThat(store.complete("order:1", first.getExecutionToken(), "created", String.class,
                    Duration.ofSeconds(1))).isTrue();
            IdempotencyExecution duplicate = store.begin("order:1", Duration.ofSeconds(1), String.class);

            assertThat(duplicate.isCompleted()).isTrue();
            assertThat(duplicate.getResult()).isEqualTo("created");
        }
    }

    @Test
    void shouldAllowRetryAfterRelease() {
        try (LocalIdempotencyStore store = new LocalIdempotencyStore()) {
            IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(1), String.class);
            assertThat(first.isAcquired()).isTrue();

            store.release("order:1", first.getExecutionToken());

            assertThat(store.begin("order:1", Duration.ofSeconds(1), String.class).isAcquired()).isTrue();
        }
    }

    @Test
    void shouldExpireProcessingEntry() throws Exception {
        try (LocalIdempotencyStore store = new LocalIdempotencyStore()) {
            assertThat(store.begin("order:1", Duration.ofMillis(1), String.class).isAcquired()).isTrue();

            Thread.sleep(10);

            assertThat(store.begin("order:1", Duration.ofSeconds(1), String.class).isAcquired()).isTrue();
        }
    }

    @Test
    void shouldRejectWhenLocalCapacityIsFull() {
        try (LocalIdempotencyStore store = new LocalIdempotencyStore(1, Duration.ofSeconds(1))) {
            assertThat(store.begin("order:1", Duration.ofSeconds(1), String.class).isAcquired()).isTrue();

            assertThatThrownBy(() -> store.begin("order:2", Duration.ofSeconds(1), String.class))
                    .isInstanceOf(IdempotencyException.class)
                    .hasMessage("Local idempotency store is full");
        }
    }

    @Test
    void shouldIgnoreCompletionFromExpiredOwner() throws Exception {
        try (LocalIdempotencyStore store = new LocalIdempotencyStore()) {
            IdempotencyExecution first = store.begin("order:1", Duration.ofMillis(1), String.class);
            Thread.sleep(10);
            IdempotencyExecution second = store.begin("order:1", Duration.ofSeconds(1), String.class);

            assertThat(store.complete("order:1", first.getExecutionToken(), "old", String.class,
                    Duration.ofSeconds(1))).isFalse();
            assertThat(store.complete("order:1", second.getExecutionToken(), "new", String.class,
                    Duration.ofSeconds(1))).isTrue();

            IdempotencyExecution duplicate = store.begin("order:1", Duration.ofSeconds(1), String.class);
            assertThat(duplicate.getResult()).isEqualTo("new");
        }
    }

    @Test
    void shouldDeserializeCompletedResultForDuplicateCall() {
        try (LocalIdempotencyStore store = new LocalIdempotencyStore()) {
            IdempotencyExecution first = store.begin("order:1", Duration.ofSeconds(1), Result.class);
            Result result = new Result("A001");

            assertThat(store.complete("order:1", first.getExecutionToken(), result, Result.class,
                    Duration.ofSeconds(1))).isTrue();
            IdempotencyExecution duplicate = store.begin("order:1", Duration.ofSeconds(1), Result.class);

            assertThat(duplicate.getResult()).isEqualTo(result);
            assertThat(duplicate.getResult()).isNotSameAs(result);
        }
    }

    record Result(String orderNo) {
    }
}
