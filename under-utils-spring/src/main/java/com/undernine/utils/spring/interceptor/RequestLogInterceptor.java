package com.undernine.utils.spring.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * HTTP请求日志拦截器
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "REQUEST_START_TIME";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MASKED_HEADER_VALUE = "******";
    private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "api-key",
            "x-auth-token",
            "x-access-token",
            "x-refresh-token",
            "x-csrf-token",
            "x-xsrf-token"
    );
    private final boolean trustedProxyHeaders;

    public RequestLogInterceptor() {
        this(false);
    }

    public RequestLogInterceptor(boolean trustedProxyHeaders) {
        this.trustedProxyHeaders = trustedProxyHeaders;
    }

    public boolean isTrustedProxyHeaders() {
        return trustedProxyHeaders;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String ip = getClientIp(request);
        
        log.info("【HTTP请求】{} {} 来自IP: {}", method, uri, ip);
        
        // 记录请求头
        if (log.isDebugEnabled()) {
            Map<String, String> headers = getHeaders(request);
            log.debug("【请求头】{}", headers);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME);
        long elapsed = startTime == null ? -1L : System.currentTimeMillis() - startTime;
        int status = response.getStatus();
        String ip = getClientIp(request);

        log.info("【HTTP响应】{} {} - 状态码: {}, IP: {}, 耗时: {}ms",
            request.getMethod(), request.getRequestURI(), status, ip, elapsed);

        if (ex != null) {
            log.error("【请求异常】{}", ex.getMessage(), ex);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String ip = null;
        if (trustedProxyHeaders) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) ip = remoteAddr;
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return headers;
        }
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, maskHeaderValue(name, request.getHeader(name)));
        }
        return headers;
    }

    private String maskHeaderValue(String name, String value) {
        if (name != null && SENSITIVE_HEADER_NAMES.contains(name.toLowerCase(Locale.ROOT))) {
            return MASKED_HEADER_VALUE;
        }
        return value;
    }
}
