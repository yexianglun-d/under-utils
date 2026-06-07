package com.undernine.utils.spring.idempotent;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MicrometerIdempotencyObserverTest {

    @Test
    void shouldRecordCounterAndTimerWithLowCardinalityTags() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MicrometerIdempotencyObserver observer = new MicrometerIdempotencyObserver(
                meterRegistry,
                ObservationRegistry.create()
        );

        observer.onEvent(IdempotencyEvent.of(
                IdempotencyOperation.BEGIN,
                IdempotencyOutcome.ACQUIRED,
                1_000_000L
        ));

        assertThat(meterRegistry.get(MicrometerIdempotencyObserver.DEFAULT_COUNTER_NAME)
                .tag("idempotency.operation", "begin")
                .tag("idempotency.outcome", "acquired")
                .tag("exception", "none")
                .counter()
                .count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get(MicrometerIdempotencyObserver.DEFAULT_TIMER_NAME)
                .tag("idempotency.operation", "begin")
                .tag("idempotency.outcome", "acquired")
                .tag("exception", "none")
                .timer()
                .count()).isEqualTo(1L);
        assertThat(meterRegistry.find(MicrometerIdempotencyObserver.DEFAULT_COUNTER_NAME)
                .tag("idempotency.key", "order:1")
                .counter()).isNull();
    }

    @Test
    void shouldRecordFailureExceptionTag() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MicrometerIdempotencyObserver observer = new MicrometerIdempotencyObserver(meterRegistry);

        observer.onEvent(IdempotencyEvent.failure(
                IdempotencyOperation.COMPLETE,
                1L,
                new IllegalStateException("boom")
        ));

        assertThat(meterRegistry.get(MicrometerIdempotencyObserver.DEFAULT_COUNTER_NAME)
                .tag("idempotency.operation", "complete")
                .tag("idempotency.outcome", "failure")
                .tag("exception", "IllegalStateException")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    @Test
    void shouldRejectBlankMeterNames() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        assertThatThrownBy(() -> new MicrometerIdempotencyObserver(
                meterRegistry,
                ObservationRegistry.NOOP,
                " ",
                MicrometerIdempotencyObserver.DEFAULT_TIMER_NAME,
                MicrometerIdempotencyObserver.DEFAULT_OBSERVATION_NAME
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
