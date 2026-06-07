package com.undernine.utils.spring.aspect;

import com.undernine.utils.spring.annotation.Idempotent;
import com.undernine.utils.spring.idempotent.IdempotencyEvent;
import com.undernine.utils.spring.idempotent.IdempotencyExecution;
import com.undernine.utils.spring.idempotent.IdempotencyException;
import com.undernine.utils.spring.idempotent.IdempotencyObserver;
import com.undernine.utils.spring.idempotent.IdempotencyOutcome;
import com.undernine.utils.spring.idempotent.IdempotencyResultCodec;
import com.undernine.utils.spring.idempotent.IdempotencyStore;
import com.undernine.utils.spring.idempotent.IdempotentInProgressException;
import com.undernine.utils.spring.idempotent.LocalIdempotencyStore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotentAspectTest {

    private IdempotentAspect aspect;
    private LocalIdempotencyStore store;
    private ProceedingJoinPoint point;
    private Idempotent idempotent;

    @BeforeEach
    void setUp() throws Exception {
        store = new LocalIdempotencyStore();
        aspect = new IdempotentAspect();
        aspect.setIdempotencyStore(store);
        aspect.setKeyResolver((joinPoint, namespace, expression) -> "order:1");

        point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = SampleService.class.getMethod("createOrder");
        when(point.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);

        idempotent = mock(Idempotent.class);
        when(idempotent.namespace()).thenReturn("order");
        when(idempotent.key()).thenReturn("#args[0]");
        when(idempotent.processingTtl()).thenReturn(-1L);
        when(idempotent.processingTimeUnit()).thenReturn(TimeUnit.SECONDS);
        when(idempotent.resultTtl()).thenReturn(-1L);
        when(idempotent.resultTimeUnit()).thenReturn(TimeUnit.SECONDS);
        when(idempotent.releaseOnFailure()).thenReturn(true);
        when(idempotent.processingMessage()).thenReturn("处理中");
    }

    @AfterEach
    void tearDown() {
        store.close();
    }

    @Test
    void shouldReturnFirstResultForCompletedDuplicate() throws Throwable {
        when(point.proceed()).thenReturn("created");

        Object first = aspect.around(point, idempotent);
        Object duplicate = aspect.around(point, idempotent);

        assertThat(first).isEqualTo("created");
        assertThat(duplicate).isEqualTo("created");
        verify(point, times(1)).proceed();
    }

    @Test
    void shouldThrowInProgressWhenFirstCallNotCompleted() {
        store.begin("order:1", java.time.Duration.ofSeconds(1), String.class);

        assertThatThrownBy(() -> aspect.around(point, idempotent))
                .isInstanceOf(IdempotentInProgressException.class)
                .hasMessage("处理中");
    }

    @Test
    void shouldReleaseKeyWhenBusinessFails() throws Throwable {
        when(point.proceed())
                .thenThrow(new IllegalStateException("boom"))
                .thenReturn("created");

        assertThatThrownBy(() -> aspect.around(point, idempotent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        Object retry = aspect.around(point, idempotent);

        assertThat(retry).isEqualTo("created");
        verify(point, times(2)).proceed();
    }

    @Test
    void shouldObserveIdempotentExecutionLifecycle() throws Throwable {
        RecordingObserver observer = new RecordingObserver();
        aspect.setIdempotencyObserver(observer);
        when(point.proceed()).thenReturn("created");

        aspect.around(point, idempotent);
        aspect.around(point, idempotent);

        assertThat(observer.events)
                .extracting(IdempotencyEvent::getOutcome)
                .containsExactly(
                        IdempotencyOutcome.ACQUIRED,
                        IdempotencyOutcome.SUCCESS,
                        IdempotencyOutcome.SUCCESS,
                        IdempotencyOutcome.COMPLETED
                );
    }

    @Test
    void shouldKeepBusinessExceptionWhenReleaseFails() throws Throwable {
        IdempotencyStore failingReleaseStore = new IdempotencyStore() {
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

            @Override
            public void release(String key, String executionToken) {
                throw new IdempotencyException("release failed");
            }
        };
        aspect.setIdempotencyStore(failingReleaseStore);
        when(point.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.around(point, idempotent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom")
                .satisfies(ex -> assertThat(ex.getSuppressed())
                        .hasSize(1))
                .satisfies(ex -> assertThat(ex.getSuppressed()[0])
                        .isInstanceOf(IdempotencyException.class));
    }

    @Test
    void shouldNotReleaseKeyWhenCompleteFailsAfterBusinessSucceeded() throws Throwable {
        store.close();
        store = new LocalIdempotencyStore(100, Duration.ofSeconds(1), "under-utils:idempotent:",
                new IdempotencyResultCodec() {
                    @Override
                    public String serialize(Object result, Type resultType) {
                        throw new IdempotencyException("serialize failed");
                    }

                    @Override
                    public Object deserialize(String payload, Type resultType) {
                        return payload;
                    }
                });
        aspect.setIdempotencyStore(store);
        when(point.proceed()).thenReturn("created");

        assertThatThrownBy(() -> aspect.around(point, idempotent))
                .isInstanceOf(IdempotencyException.class)
                .hasMessage("serialize failed");
        assertThatThrownBy(() -> aspect.around(point, idempotent))
                .isInstanceOf(IdempotentInProgressException.class);
        verify(point, times(1)).proceed();
    }

    static class SampleService {
        public String createOrder() {
            return "created";
        }
    }

    private static final class RecordingObserver implements IdempotencyObserver {
        private final List<IdempotencyEvent> events = new ArrayList<>();

        @Override
        public void onEvent(IdempotencyEvent event) {
            events.add(event);
        }
    }
}
