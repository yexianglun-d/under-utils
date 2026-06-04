package com.undernine.utils.spring.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * RequestLogInterceptor 测试类
 *
 * @author deng
 */
@ExtendWith(MockitoExtension.class)
class RequestLogInterceptorTest {

    @InjectMocks
    private RequestLogInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void testPreHandle() {
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(request).setAttribute(eq("REQUEST_START_TIME"), anyLong());
    }

    @Test
    void testAfterCompletion() {
        when(response.getStatus()).thenReturn(200);
        request.setAttribute("REQUEST_START_TIME", System.currentTimeMillis());

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(response).getStatus();
    }

    @Test
    void testAfterCompletionWithException() {
        when(response.getStatus()).thenReturn(500);
        request.setAttribute("REQUEST_START_TIME", System.currentTimeMillis());
        Exception exception = new RuntimeException("测试异常");

        interceptor.afterCompletion(request, response, new Object(), exception);

        verify(response).getStatus();
    }

    @Test
    void testGetClientIpFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        RequestLogInterceptor trustedInterceptor = new RequestLogInterceptor(true);
        boolean result = trustedInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(trustedInterceptor.isTrustedProxyHeaders()).isTrue();
        verify(request).getHeader("X-Forwarded-For");
    }

    @Test
    void testIgnoreXForwardedForByDefault() {
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(interceptor.isTrustedProxyHeaders()).isFalse();
        verify(request, never()).getHeader("X-Forwarded-For");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMaskSensitiveHeaders() throws Exception {
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of(
                "Authorization", "Cookie", "X-Api-Key", "X-Request-Id"
        )));
        when(request.getHeader("Authorization")).thenReturn("Bearer secret-token");
        when(request.getHeader("Cookie")).thenReturn("SESSION=secret");
        when(request.getHeader("X-Api-Key")).thenReturn("api-secret");
        when(request.getHeader("X-Request-Id")).thenReturn("req-001");
        Method method = RequestLogInterceptor.class.getDeclaredMethod("getHeaders", HttpServletRequest.class);
        method.setAccessible(true);

        Map<String, String> headers = (Map<String, String>) method.invoke(interceptor, request);

        assertThat(headers)
                .containsEntry("Authorization", "******")
                .containsEntry("Cookie", "******")
                .containsEntry("X-Api-Key", "******")
                .containsEntry("X-Request-Id", "req-001");
    }
}
