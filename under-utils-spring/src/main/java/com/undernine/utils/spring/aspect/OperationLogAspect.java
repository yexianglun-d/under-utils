package com.undernine.utils.spring.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undernine.utils.spring.annotation.OperationLog;
import com.undernine.utils.spring.context.OperationContext;
import com.undernine.utils.spring.context.OperationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 操作日志切面。
 * <p>
 * 仅提供兼容维护的轻量日志输出。该类不再声明为 Spring 组件，业务如仍需使用，应显式
 * {@code @Import(OperationLogAspect.class)} 或注册为 {@code @Bean}。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 * @deprecated 轻量日志切面保留为兼容 API，不作为 Under-Utils 后续工程模式主线能力演进。
 */
@Slf4j
@Aspect
@Deprecated(since = "1.0.0")
public class OperationLogAspect {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final boolean trustedProxyHeaders;

    public OperationLogAspect() {
        this(false);
    }

    public OperationLogAspect(boolean trustedProxyHeaders) {
        this.trustedProxyHeaders = trustedProxyHeaders;
    }

    public boolean isTrustedProxyHeaders() {
        return trustedProxyHeaders;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationContextHolder.Scope operationScope = openOperationScope(operationLog);

        try {
            String username = getCurrentUsername();
            String ip = getClientIp();
            String uri = getRequestUri();
            boolean recordParams = operationLog.recordParams();
            boolean recordResult = operationLog.recordResult();

            log.info("【操作日志】模块: {}, 类型: {}, 内容: {}, 用户: {}, IP: {}, URI: {}",
                operationLog.module(), operationLog.type().getDescription(),
                operationLog.content(), username, ip, uri);

            if (recordParams) {
                Object[] args = point.getArgs();
                if (args != null && args.length > 0) {
                    try {
                        log.info("【操作日志】请求参数: {}", MAPPER.writeValueAsString(args));
                    } catch (Exception ignored) {}
                }
            }

            try {
                Object result = point.proceed();

                if (recordResult && result != null) {
                    try {
                        log.info("【操作日志】返回结果: {}", MAPPER.writeValueAsString(result));
                    } catch (Exception ignored) {}
                }

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("【操作日志】操作成功, 耗时: {}ms", elapsed);
                return result;
            } catch (Throwable e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("【操作日志】操作失败, 耗时: {}ms, 错误: {}", elapsed, e.getMessage());
                throw e;
            }
        } finally {
            if (operationScope != null) {
                operationScope.close();
            }
        }
    }

    private String getCurrentUsername() {
        OperationContext context = OperationContextHolder.getContext();
        if (context != null && isNotBlank(context.getUserId())) {
            return context.getUserId();
        }
        return "system";
    }

    private String getClientIp() {
        OperationContext context = OperationContextHolder.getContext();
        if (context != null && isNotBlank(context.getClientIp())) {
            return context.getClientIp();
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        
        HttpServletRequest request = attrs.getRequest();
        String ip = null;
        if (trustedProxyHeaders) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private String getRequestUri() {
        OperationContext context = OperationContextHolder.getContext();
        if (context != null && isNotBlank(context.getRequestUri())) {
            return context.getRequestUri();
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest().getRequestURI() : "unknown";
    }

    private OperationContextHolder.Scope openOperationScope(OperationLog operationLog) {
        OperationContext context = OperationContextHolder.getContext();
        if (context == null) {
            return null;
        }
        String operationName = resolveOperationName(operationLog);
        if (!isNotBlank(operationName)) {
            return null;
        }
        return OperationContextHolder.scope(context.withOperationName(operationName));
    }

    private String resolveOperationName(OperationLog operationLog) {
        if (operationLog == null) {
            return null;
        }
        if (isNotBlank(operationLog.content())) {
            return operationLog.content();
        }
        if (isNotBlank(operationLog.module())) {
            return operationLog.module();
        }
        return operationLog.type() == null ? null : operationLog.type().getDescription();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
