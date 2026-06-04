package com.undernine.utils.spring.context;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * HTTP 请求操作上下文过滤器。
 * <p>
 * 进入请求时构建 {@link OperationContext}，结束时恢复/清理 ThreadLocal 和 MDC。
 * 该类只暴露能力，不负责自动注册；业务应用或 starter 可自行装配到过滤器链。
 * 默认不信任客户端传入的用户、租户和代理 IP Header；只有部署在可信网关之后且 Header 已清洗时，
 * 才应开启可信身份 Header 或可信代理 Header。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class OperationContextFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private static final String DEFAULT_TENANT = "default";
    private static final String ANONYMOUS = "anonymous";

    private final List<OperationContextCustomizer> customizers = new ArrayList<>();
    private final boolean trustedIdentityHeaders;
    private final boolean trustedProxyHeaders;

    public OperationContextFilter() {
        this(null, false, false);
    }

    public OperationContextFilter(Collection<OperationContextCustomizer> customizers) {
        this(customizers, false, false);
    }

    public OperationContextFilter(Collection<OperationContextCustomizer> customizers, boolean trustedIdentityHeaders) {
        this(customizers, trustedIdentityHeaders, trustedIdentityHeaders);
    }

    public OperationContextFilter(Collection<OperationContextCustomizer> customizers,
                                  boolean trustedIdentityHeaders,
                                  boolean trustedProxyHeaders) {
        this.trustedIdentityHeaders = trustedIdentityHeaders;
        this.trustedProxyHeaders = trustedProxyHeaders;
        setCustomizers(customizers);
    }

    public void setCustomizers(Collection<OperationContextCustomizer> customizers) {
        this.customizers.clear();
        if (customizers != null) {
            this.customizers.addAll(customizers);
        }
    }

    public boolean isTrustedIdentityHeaders() {
        return trustedIdentityHeaders;
    }

    public boolean isTrustedProxyHeaders() {
        return trustedProxyHeaders;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        OperationContext context = buildContext(httpRequest);
        if (response instanceof HttpServletResponse httpResponse && isNotBlank(context.getTraceId())) {
            httpResponse.setHeader(TRACE_ID_HEADER, context.getTraceId());
        }

        String previousTraceId = MDC.get(TRACE_ID_MDC_KEY);
        if (isNotBlank(context.getTraceId())) {
            MDC.put(TRACE_ID_MDC_KEY, context.getTraceId());
        } else {
            MDC.remove(TRACE_ID_MDC_KEY);
        }

        try (OperationContextHolder.Scope ignored = OperationContextHolder.scope(context)) {
            chain.doFilter(request, response);
        } finally {
            if (isNotBlank(previousTraceId)) {
                MDC.put(TRACE_ID_MDC_KEY, previousTraceId);
            } else {
                MDC.remove(TRACE_ID_MDC_KEY);
            }
        }
    }

    protected OperationContext buildContext(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        OperationContext.Builder builder = OperationContext.builder()
                .traceId(resolveTraceId(request))
                .tenantId(resolveTenantId(request))
                .userId(resolveUserId(request))
                .requestMethod(method)
                .requestUri(uri)
                .clientIp(resolveClientIp(request))
                .operationName(defaultOperationName(method, uri));

        for (OperationContextCustomizer customizer : customizers) {
            customizer.customize(builder, request);
        }
        return builder.build();
    }

    protected String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        return isNotBlank(traceId) ? traceId.trim() : generateTraceId();
    }

    protected String resolveTenantId(HttpServletRequest request) {
        if (trustedIdentityHeaders) {
            String tenantId = request.getHeader(TENANT_ID_HEADER);
            if (isNotBlank(tenantId)) {
                return tenantId.trim();
            }
        }
        return DEFAULT_TENANT;
    }

    protected String resolveUserId(HttpServletRequest request) {
        if (trustedIdentityHeaders) {
            String userId = request.getHeader(USER_ID_HEADER);
            if (isNotBlank(userId)) {
                return userId.trim();
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return isNotBlank(remoteAddr) ? remoteAddr.trim() : ANONYMOUS;
    }

    protected String resolveClientIp(HttpServletRequest request) {
        String ip = null;
        if (trustedProxyHeaders) {
            ip = request.getHeader("X-Forwarded-For");
            if (!isNotBlank(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
        }
        if (!isNotBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    protected String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String defaultOperationName(String method, String uri) {
        if (isNotBlank(method) && isNotBlank(uri)) {
            return method.trim() + " " + uri.trim();
        }
        return isNotBlank(uri) ? uri.trim() : null;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
