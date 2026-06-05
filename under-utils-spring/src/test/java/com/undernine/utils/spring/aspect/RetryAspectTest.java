package com.undernine.utils.spring.aspect;

import com.undernine.utils.spring.annotation.Retry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * RetryAspect 测试类
 *
 * @author deng
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"deprecation", "unchecked"})
class RetryAspectTest {

    @InjectMocks
    private RetryAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Retry retry;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
    }

    @Test
    void testSuccessOnFirstAttempt() throws Throwable {
        when(retry.maxAttempts()).thenReturn(3);
        when(retry.delay()).thenReturn(100L);
        when(retry.exceptions()).thenReturn(new Class[]{Exception.class});
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.around(joinPoint, retry);

        assertThat(result).isEqualTo("success");
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void testSuccessOnSecondAttempt() throws Throwable {
        when(retry.maxAttempts()).thenReturn(3);
        when(retry.delay()).thenReturn(10L);
        when(retry.exceptions()).thenReturn(new Class[]{IOException.class});
        when(joinPoint.proceed())
            .thenThrow(new IOException("第一次失败"))
            .thenReturn("success");

        Object result = aspect.around(joinPoint, retry);

        assertThat(result).isEqualTo("success");
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void testFailAfterMaxAttempts() throws Throwable {
        when(retry.maxAttempts()).thenReturn(3);
        when(retry.delay()).thenReturn(10L);
        when(retry.exceptions()).thenReturn(new Class[]{IOException.class});
        when(joinPoint.proceed()).thenThrow(new IOException("持续失败"));

        assertThatThrownBy(() -> aspect.around(joinPoint, retry))
            .isInstanceOf(IOException.class)
            .hasMessage("持续失败");

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void testNoRetryForNonMatchingException() throws Throwable {
        when(retry.maxAttempts()).thenReturn(3);
        when(retry.delay()).thenReturn(10L);
        when(retry.exceptions()).thenReturn(new Class[]{IOException.class});
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("不匹配的异常"));

        assertThatThrownBy(() -> aspect.around(joinPoint, retry))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("不匹配的异常");

        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void testInvalidMaxAttemptsFallsBackToSingleAttempt() throws Throwable {
        when(retry.maxAttempts()).thenReturn(0);
        when(retry.delay()).thenReturn(-1L);
        when(retry.exceptions()).thenReturn(new Class[]{IOException.class});
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.around(joinPoint, retry);

        assertThat(result).isEqualTo("success");
        verify(joinPoint, times(1)).proceed();
    }
}
