package com.undernine.utils.spring.key;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.undernine.utils.spring.context.CurrentTenantProvider;
import com.undernine.utils.spring.context.CurrentUserProvider;
import com.undernine.utils.spring.context.DefaultCurrentTenantProvider;
import com.undernine.utils.spring.context.DefaultCurrentUserProvider;
import com.undernine.utils.spring.context.DefaultTraceIdProvider;
import com.undernine.utils.spring.context.OperationContext;
import com.undernine.utils.spring.context.OperationContextHolder;
import com.undernine.utils.spring.context.TraceIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * 默认操作 key 解析器。
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DefaultOperationKeyResolver implements OperationKeyResolver {

    private static final String DEFAULT_NAMESPACE = "under-utils";
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final ObjectMapper JSON_MAPPER = createJsonMapper();

    private final CurrentUserProvider currentUserProvider;
    private final CurrentTenantProvider currentTenantProvider;
    private final TraceIdProvider traceIdProvider;

    public DefaultOperationKeyResolver() {
        this(new DefaultCurrentUserProvider(), new DefaultCurrentTenantProvider(), new DefaultTraceIdProvider());
    }

    public DefaultOperationKeyResolver(CurrentUserProvider currentUserProvider, CurrentTenantProvider currentTenantProvider) {
        this(currentUserProvider, currentTenantProvider, new DefaultTraceIdProvider());
    }

    public DefaultOperationKeyResolver(CurrentUserProvider currentUserProvider,
                                       CurrentTenantProvider currentTenantProvider,
                                       TraceIdProvider traceIdProvider) {
        this.currentUserProvider = currentUserProvider;
        this.currentTenantProvider = currentTenantProvider;
        this.traceIdProvider = traceIdProvider;
    }

    @Override
    public String resolve(ProceedingJoinPoint point, String namespace, String expression) {
        String actualNamespace = isNotBlank(namespace) ? namespace.trim() : DEFAULT_NAMESPACE;
        OperationContext context = OperationContextHolder.getContext();
        String userId = normalize(resolveUserId(context), "anonymous");
        String tenantId = normalize(resolveTenantId(context), "default");
        String uri = normalize(getRequestUri(), "no-request");
        String methodName = normalize(getMethodName(point), "unknown");

        String businessKey = isNotBlank(expression)
                ? evaluateExpression(point, expression)
                : digest(tryToJson(point.getArgs()));

        return String.join(":", actualNamespace, tenantId, userId, uri, methodName, businessKey);
    }

    private String evaluateExpression(ProceedingJoinPoint point, String expression) {
        try {
            Method method = getMethod(point);
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    point.getTarget(), method, point.getArgs(), PARAMETER_NAME_DISCOVERER);
            OperationContext operationContext = OperationContextHolder.getContext();
            context.setVariable("args", point.getArgs());
            context.setVariable("context", operationContext);
            context.setVariable("operationContext", operationContext);
            context.setVariable("traceId", resolveTraceId(operationContext));
            context.setVariable("userId", resolveUserId(operationContext));
            context.setVariable("tenantId", resolveTenantId(operationContext));
            context.setVariable("uri", getRequestUri());
            context.setVariable("requestUri", getRequestUri());
            context.setVariable("requestMethod", operationContext == null ? null : operationContext.getRequestMethod());
            context.setVariable("clientIp", operationContext == null ? null : operationContext.getClientIp());
            context.setVariable("operationName", operationContext == null ? null : operationContext.getOperationName());
            Object value = PARSER.parseExpression(expression).getValue(context);
            return normalize(value == null ? null : String.valueOf(value), "empty");
        } catch (Exception e) {
            return digest(expression + ":" + tryToJson(point.getArgs()));
        }
    }

    private String tryToJson(Object value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return fallbackDigestSource(value);
        } catch (RuntimeException e) {
            return fallbackDigestSource(value);
        }
    }

    private String fallbackDigestSource(Object value) {
        if (value instanceof Object[] values) {
            return Arrays.stream(values)
                    .map(this::safeValue)
                    .toList()
                    .toString();
        }
        return safeValue(value);
    }

    private String safeValue(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return value.getClass().getName() + ":" + String.valueOf(value);
        } catch (RuntimeException ex) {
            return value.getClass().getName() + ":<unprintable>";
        }
    }

    private static ObjectMapper createJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    private Method getMethod(ProceedingJoinPoint point) {
        if (point.getSignature() instanceof MethodSignature methodSignature) {
            return methodSignature.getMethod();
        }
        throw new IllegalStateException("Operation key expression requires method signature");
    }

    private String getMethodName(ProceedingJoinPoint point) {
        return point.getSignature() == null ? "unknown" : point.getSignature().getName();
    }

    private String getRequestUri() {
        OperationContext context = OperationContextHolder.getContext();
        if (context != null && isNotBlank(context.getRequestUri())) {
            return context.getRequestUri();
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request == null ? null : request.getRequestURI();
    }

    private String resolveUserId(OperationContext context) {
        if (context != null && isNotBlank(context.getUserId())) {
            return context.getUserId();
        }
        return currentUserProvider == null ? null : currentUserProvider.getCurrentUserId();
    }

    private String resolveTenantId(OperationContext context) {
        if (context != null && isNotBlank(context.getTenantId())) {
            return context.getTenantId();
        }
        return currentTenantProvider == null ? null : currentTenantProvider.getCurrentTenantId();
    }

    private String resolveTraceId(OperationContext context) {
        if (context != null && isNotBlank(context.getTraceId())) {
            return context.getTraceId();
        }
        return traceIdProvider == null ? null : traceIdProvider.getTraceId();
    }

    private String digest(String value) {
        String actualValue = value == null ? "" : value;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(actualValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 24);
        } catch (Exception e) {
            return String.valueOf(actualValue.hashCode());
        }
    }

    private String normalize(String value, String defaultValue) {
        return isNotBlank(value) ? value.trim() : defaultValue;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
