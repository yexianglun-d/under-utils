package com.undernine.utils.spring.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class OperationContextSnapshotTest {

    @BeforeEach
    void setUp() {
        tearDown();
    }

    @AfterEach
    void tearDown() {
        OperationContextHolder.clear();
    }

    @Test
    void wrapRunnablePropagatesCapturedContextAndRestoresPrevious() {
        OperationContext previous = context("previous");
        OperationContext captured = context("captured");
        OperationContext modified = context("modified");

        OperationContextHolder.setContext(captured);
        Runnable wrapped = OperationContextSnapshot.capture().wrap(() -> {
            assertThat(OperationContextHolder.getContext()).isSameAs(captured);
            OperationContextHolder.setContext(modified);
        });

        OperationContextHolder.setContext(previous);

        wrapped.run();

        assertThat(OperationContextHolder.getContext()).isSameAs(previous);
    }

    @Test
    void wrapCallablePropagatesCapturedContext() throws Exception {
        OperationContext captured = context("callable");
        OperationContextHolder.setContext(captured);

        Callable<String> wrapped = OperationContextSnapshot.capture()
                .wrap(() -> OperationContextHolder.getContext().getTraceId());

        OperationContextHolder.clear();

        assertThat(wrapped.call()).isEqualTo("callable");
        assertThat(OperationContextHolder.getContext()).isNull();
    }

    @Test
    void wrapSupplierPropagatesCapturedContext() {
        OperationContext captured = context("supplier");
        OperationContextHolder.setContext(captured);

        Supplier<String> wrapped = OperationContextSnapshot.capture()
                .wrapSupplier(() -> OperationContextHolder.getContext().getTraceId());

        OperationContextHolder.clear();

        assertThat(wrapped.get()).isEqualTo("supplier");
        assertThat(OperationContextHolder.getContext()).isNull();
    }

    @Test
    void emptySnapshotClearsContextInsideTaskAndRestoresPrevious() {
        OperationContext previous = context("previous");

        OperationContextHolder.clear();
        Runnable wrapped = OperationContextSnapshot.capture().wrap(() ->
                assertThat(OperationContextHolder.getContext()).isNull());

        OperationContextHolder.setContext(previous);

        wrapped.run();

        assertThat(OperationContextHolder.getContext()).isSameAs(previous);
    }

    private OperationContext context(String traceId) {
        return OperationContext.builder().traceId(traceId).build();
    }
}
