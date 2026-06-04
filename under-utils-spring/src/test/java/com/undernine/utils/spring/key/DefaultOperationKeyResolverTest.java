package com.undernine.utils.spring.key;

import com.undernine.utils.spring.context.OperationContext;
import com.undernine.utils.spring.context.OperationContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultOperationKeyResolverTest {

    private final DefaultOperationKeyResolver resolver = new DefaultOperationKeyResolver();

    @BeforeEach
    void setUp() {
        OperationContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        OperationContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void defaultKeyPrefersOperationContextUserTenantAndUri() {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(point.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("submit");
        when(point.getArgs()).thenReturn(new Object[]{"order-1"});
        OperationContext context = OperationContext.builder()
                .traceId("trace-context")
                .tenantId("tenant-context")
                .userId("user-context")
                .requestUri("/ctx/orders")
                .build();

        String key;
        try (OperationContextHolder.Scope ignored = OperationContextHolder.scope(context)) {
            key = resolver.resolve(point, "orders", "");
        }

        assertThat(key).startsWith("orders:tenant-context:user-context:/ctx/orders:submit:");
    }

    @Test
    void defaultKeySerializesJavaTimeArguments() {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(point.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("submit");
        when(point.getArgs()).thenReturn(new Object[]{LocalDateTime.of(2026, 5, 24, 15, 30)});

        String key = resolver.resolve(point, "orders", "");

        assertThat(key).matches("orders:default:anonymous:no-request:submit:[0-9a-f]{24}");
    }

    @Test
    void defaultKeyFallsBackWhenArgumentsCannotBeSerialized() {
        ProceedingJoinPoint firstPoint = pointWithArgs(new UnserializableArgument("order-a"));
        ProceedingJoinPoint secondPoint = pointWithArgs(new UnserializableArgument("order-b"));

        String firstKey = resolver.resolve(firstPoint, "orders", "");
        String secondKey = resolver.resolve(secondPoint, "orders", "");

        assertThat(firstKey).matches("orders:default:anonymous:no-request:submit:[0-9a-f]{24}");
        assertThat(secondKey).matches("orders:default:anonymous:no-request:submit:[0-9a-f]{24}");
        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    @Test
    void defaultKeyIgnoresUntrustedIdentityHeaders() {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(point.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("submit");
        when(point.getArgs()).thenReturn(new Object[]{"order-1"});
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader("X-Tenant-Id", "tenant-spoofed");
        request.addHeader("X-User-Id", "user-spoofed");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String key = resolver.resolve(point, "orders", "");

        assertThat(key).startsWith("orders:default:127.0.0.1:/api/orders:submit:");
    }

    @Test
    void expressionCanUseOperationContextVariables() throws Exception {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Target target = new Target();
        Method method = Target.class.getDeclaredMethod("submit", String.class);
        when(point.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn("submit");
        when(point.getTarget()).thenReturn(target);
        when(point.getArgs()).thenReturn(new Object[]{"order-1"});
        OperationContext context = OperationContext.builder()
                .traceId("trace-context")
                .tenantId("tenant-context")
                .userId("user-context")
                .requestMethod("POST")
                .requestUri("/ctx/orders")
                .clientIp("10.0.0.1")
                .operationName("submit-order")
                .build();

        String key;
        try (OperationContextHolder.Scope ignored = OperationContextHolder.scope(context)) {
            key = resolver.resolve(point, "orders",
                    "#traceId + ':' + #tenantId + ':' + #userId + ':' + #requestMethod + ':' + #clientIp + ':' + #operationName");
        }

        assertThat(key).isEqualTo("orders:tenant-context:user-context:/ctx/orders:submit:"
                + "trace-context:tenant-context:user-context:POST:10.0.0.1:submit-order");
    }

    private static class Target {
        @SuppressWarnings("unused")
        public String submit(String orderId) {
            return orderId;
        }
    }

    private ProceedingJoinPoint pointWithArgs(Object... args) {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(point.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("submit");
        when(point.getArgs()).thenReturn(args);
        return point;
    }

    private static final class UnserializableArgument {
        private final String value;

        private UnserializableArgument(String value) {
            this.value = value;
        }

        @SuppressWarnings("unused")
        public String getBroken() {
            throw new IllegalStateException("cannot serialize");
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
