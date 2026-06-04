package com.undernine.utils.spring.context;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTraceIdProviderTest {

    private final DefaultTraceIdProvider provider = new DefaultTraceIdProvider();

    @BeforeEach
    void setUp() {
        tearDown();
    }

    @AfterEach
    void tearDown() {
        OperationContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
        MDC.remove("traceId");
    }

    @Test
    void prefersOperationContextTraceId() {
        MDC.put("traceId", "mdc-trace");
        OperationContext context = OperationContext.builder().traceId("context-trace").build();

        try (OperationContextHolder.Scope ignored = OperationContextHolder.scope(context)) {
            assertThat(provider.getTraceId()).isEqualTo("context-trace");
        }
    }

    @Test
    void fallsBackToRequestHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Trace-Id")).thenReturn("request-trace");
        RequestContextHolder.setRequestAttributes(attributes);

        assertThat(provider.getTraceId()).isEqualTo("request-trace");
    }
}
