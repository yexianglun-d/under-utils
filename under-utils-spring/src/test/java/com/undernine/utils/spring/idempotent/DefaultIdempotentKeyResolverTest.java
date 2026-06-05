package com.undernine.utils.spring.idempotent;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultIdempotentKeyResolverTest {

    private DefaultIdempotentKeyResolver resolver;
    private ProceedingJoinPoint point;

    @BeforeEach
    void setUp() throws Exception {
        resolver = new DefaultIdempotentKeyResolver();
        point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = SampleService.class.getMethod("handle", String.class);
        when(point.getSignature()).thenReturn(signature);
        when(point.getTarget()).thenReturn(new SampleService());
        when(point.getArgs()).thenReturn(new Object[]{"A001"});
        when(signature.getMethod()).thenReturn(method);
    }

    @Test
    void shouldResolveExplicitExpression() {
        String key = resolver.resolve(point, "order", "#args[0]");

        assertThat(key).isEqualTo("order:"
                + SampleService.class.getName()
                + "#handle(java.lang.String):A001");
    }

    @Test
    void shouldResolveStableDefaultKey() {
        String first = resolver.resolve(point, "order", "");
        String second = resolver.resolve(point, "order", "");

        assertThat(first).isEqualTo(second);
        assertThat(first).startsWith("order:" + SampleService.class.getName() + "#handle(java.lang.String):");
    }

    @Test
    void shouldUseParameterTypesInDefaultMethodSignature() throws Exception {
        MethodSignature signature = mock(MethodSignature.class);
        Method overloadedMethod = SampleService.class.getMethod("handle", Object.class);
        when(point.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(overloadedMethod);

        String key = resolver.resolve(point, "order", "");

        assertThat(key).startsWith("order:" + SampleService.class.getName() + "#handle(java.lang.Object):");
    }

    @Test
    void shouldFailWhenExplicitExpressionCannotResolve() {
        assertThatThrownBy(() -> resolver.resolve(point, "order", "#missing.value"))
                .isInstanceOf(IdempotentKeyResolveException.class)
                .hasMessageContaining("Failed to resolve idempotent key expression");
    }

    static class SampleService {
        public String handle(String orderNo) {
            return orderNo;
        }

        public String handle(Object orderNo) {
            return String.valueOf(orderNo);
        }
    }
}
