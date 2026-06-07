package com.undernine.utils.spring.idempotent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 将业务幂等事件桥接到 Micrometer 的观察者。
 * <p>
 * 只写入操作、结果和异常类型等低基数 tag，不记录幂等 key、方法签名或业务参数。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
public final class MicrometerIdempotencyObserver implements IdempotencyObserver {

    public static final String DEFAULT_COUNTER_NAME = "under.utils.idempotency.operations";
    public static final String DEFAULT_TIMER_NAME = "under.utils.idempotency.duration";
    public static final String DEFAULT_OBSERVATION_NAME = "under.utils.idempotency";

    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;
    private final String counterName;
    private final String timerName;
    private final String observationName;

    public MicrometerIdempotencyObserver(MeterRegistry meterRegistry) {
        this(meterRegistry, ObservationRegistry.NOOP);
    }

    public MicrometerIdempotencyObserver(MeterRegistry meterRegistry,
                                         ObservationRegistry observationRegistry) {
        this(meterRegistry, observationRegistry, DEFAULT_COUNTER_NAME, DEFAULT_TIMER_NAME, DEFAULT_OBSERVATION_NAME);
    }

    public MicrometerIdempotencyObserver(MeterRegistry meterRegistry,
                                         ObservationRegistry observationRegistry,
                                         String counterName,
                                         String timerName,
                                         String observationName) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.observationRegistry = observationRegistry == null ? ObservationRegistry.NOOP : observationRegistry;
        this.counterName = requireText(counterName, "counterName");
        this.timerName = requireText(timerName, "timerName");
        this.observationName = requireText(observationName, "observationName");
    }

    @Override
    public void onEvent(IdempotencyEvent event) {
        IdempotencyEvent safeEvent = Objects.requireNonNull(event, "event must not be null");
        Tags tags = Tags.of(
                "idempotency.operation", normalize(safeEvent.getOperation().name()),
                "idempotency.outcome", normalize(safeEvent.getOutcome().name()),
                "exception", exceptionName(safeEvent.getError())
        );

        Counter.builder(counterName)
                .description("Under-Utils idempotency operation count")
                .tags(tags)
                .register(meterRegistry)
                .increment();

        Timer.builder(timerName)
                .description("Under-Utils idempotency operation duration")
                .tags(tags)
                .register(meterRegistry)
                .record(safeEvent.getDurationNanos(), TimeUnit.NANOSECONDS);

        Observation observation = Observation.createNotStarted(observationName, observationRegistry)
                .contextualName("idempotency " + normalize(safeEvent.getOperation().name()))
                .lowCardinalityKeyValue("idempotency.operation", normalize(safeEvent.getOperation().name()))
                .lowCardinalityKeyValue("idempotency.outcome", normalize(safeEvent.getOutcome().name()))
                .lowCardinalityKeyValue("exception", exceptionName(safeEvent.getError()));
        if (safeEvent.getError() != null) {
            observation.error(safeEvent.getError());
        }
        observation.start();
        observation.stop();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String exceptionName(Throwable error) {
        return error == null ? "none" : error.getClass().getSimpleName();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
