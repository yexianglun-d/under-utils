package com.undernine.utils.spring.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OperationContextTaskDecoratorTest {

    @BeforeEach
    void setUp() {
        tearDown();
    }

    @AfterEach
    void tearDown() {
        OperationContextHolder.clear();
    }

    @Test
    void decorateCapturesContextWhenDecoratingRunnable() {
        OperationContext context = OperationContext.builder().traceId("decorated").build();
        AtomicReference<OperationContext> seenContext = new AtomicReference<>();
        OperationContextTaskDecorator decorator = new OperationContextTaskDecorator();

        Runnable decorated;
        try (OperationContextHolder.Scope ignored = OperationContextHolder.scope(context)) {
            decorated = decorator.decorate(() ->
                    seenContext.set(OperationContextHolder.getContext()));
        }

        decorated.run();

        assertThat(seenContext.get()).isSameAs(context);
        assertThat(OperationContextHolder.getContext()).isNull();
    }
}
