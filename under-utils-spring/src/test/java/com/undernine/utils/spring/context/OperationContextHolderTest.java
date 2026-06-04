package com.undernine.utils.spring.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationContextHolderTest {

    @BeforeEach
    void setUp() {
        tearDown();
    }

    @AfterEach
    void tearDown() {
        OperationContextHolder.clear();
    }

    @Test
    void setContextAndClear() {
        OperationContext context = OperationContext.builder()
                .traceId(" trace-1 ")
                .tenantId("tenant-a")
                .userId("user-a")
                .attribute("role", "admin")
                .build();

        OperationContextHolder.setContext(context);

        assertThat(OperationContextHolder.getContext()).isSameAs(context);
        assertThat(context.getTraceId()).isEqualTo("trace-1");
        assertThat(context.getAttribute("role", String.class)).isEqualTo("admin");
        assertThatThrownBy(() -> context.getAttributes().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);

        OperationContextHolder.clear();

        assertThat(OperationContextHolder.getContext()).isNull();
    }

    @Test
    void scopeRestoresPreviousContext() {
        OperationContext previous = OperationContext.builder().traceId("previous").build();
        OperationContext current = OperationContext.builder().traceId("current").build();
        OperationContextHolder.setContext(previous);

        try (OperationContextHolder.Scope ignored = OperationContextHolder.scope(current)) {
            assertThat(OperationContextHolder.getContext()).isSameAs(current);
        }

        assertThat(OperationContextHolder.getContext()).isSameAs(previous);
    }

    @Test
    void scopeClearsWhenNoPreviousContext() {
        OperationContext current = OperationContext.builder().traceId("current").build();

        OperationContextHolder.Scope scope = OperationContextHolder.scope(current);
        assertThat(OperationContextHolder.getContext()).isSameAs(current);

        scope.close();
        scope.close();

        assertThat(OperationContextHolder.getContext()).isNull();
    }
}
