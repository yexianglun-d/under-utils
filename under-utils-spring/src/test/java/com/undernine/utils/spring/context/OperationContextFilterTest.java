package com.undernine.utils.spring.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OperationContextFilterTest {

    @AfterEach
    void tearDown() {
        OperationContextHolder.clear();
        MDC.remove(OperationContextFilter.TRACE_ID_MDC_KEY);
    }

    @Test
    void buildsContextFromTraceHeaderAndClearsAfterRequest() throws Exception {
        OperationContextFilter filter = new OperationContextFilter(List.of(), true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader(OperationContextFilter.TRACE_ID_HEADER, " trace-header ");
        request.addHeader(OperationContextFilter.TENANT_ID_HEADER, "tenant-a");
        request.addHeader(OperationContextFilter.USER_ID_HEADER, "user-a");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<OperationContext> seenContext = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            seenContext.set(OperationContextHolder.getContext());
            assertThat(MDC.get(OperationContextFilter.TRACE_ID_MDC_KEY)).isEqualTo("trace-header");
        });

        OperationContext context = seenContext.get();
        assertThat(context.getTraceId()).isEqualTo("trace-header");
        assertThat(context.getTenantId()).isEqualTo("tenant-a");
        assertThat(context.getUserId()).isEqualTo("user-a");
        assertThat(context.getRequestMethod()).isEqualTo("GET");
        assertThat(context.getRequestUri()).isEqualTo("/api/orders");
        assertThat(context.getClientIp()).isEqualTo("10.0.0.1");
        assertThat(context.getOperationName()).isEqualTo("GET /api/orders");
        assertThat(response.getHeader(OperationContextFilter.TRACE_ID_HEADER)).isEqualTo("trace-header");
        assertThat(OperationContextHolder.getContext()).isNull();
        assertThat(MDC.get(OperationContextFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void ignoresIdentityHeadersByDefault() throws Exception {
        OperationContextFilter filter = new OperationContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader(OperationContextFilter.TENANT_ID_HEADER, "tenant-a");
        request.addHeader(OperationContextFilter.USER_ID_HEADER, "user-a");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<OperationContext> seenContext = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                seenContext.set(OperationContextHolder.getContext()));

        assertThat(seenContext.get().getTenantId()).isEqualTo("default");
        assertThat(seenContext.get().getUserId()).isEqualTo("127.0.0.1");
    }

    @Test
    void generatesTraceIdWhenHeaderMissing() throws Exception {
        OperationContextFilter filter = new OperationContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceId = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                traceId.set(OperationContextHolder.getContext().getTraceId()));

        assertThat(traceId.get()).isNotBlank().hasSize(32);
        assertThat(response.getHeader(OperationContextFilter.TRACE_ID_HEADER)).isEqualTo(traceId.get());
        assertThat(OperationContextHolder.getContext()).isNull();
    }

    @Test
    void appliesCustomizers() throws Exception {
        OperationContextFilter filter = new OperationContextFilter(List.of((builder, request) ->
                builder.operationName("custom-operation").attribute("channel", request.getHeader("X-Channel"))));
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/orders/1");
        request.addHeader("X-Channel", "mobile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<OperationContext> seenContext = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                seenContext.set(OperationContextHolder.getContext()));

        assertThat(seenContext.get().getOperationName()).isEqualTo("custom-operation");
        assertThat(seenContext.get().getAttribute("channel", String.class)).isEqualTo("mobile");
        assertThat(OperationContextHolder.getContext()).isNull();
    }
}
