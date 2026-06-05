package com.undernine.utils.spring.idempotent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.stream.Collectors;

/**
 * 默认服务层幂等 key 解析器。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class DefaultIdempotentKeyResolver implements IdempotentKeyResolver {

    private static final String DEFAULT_NAMESPACE = "idempotent";
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final ObjectMapper JSON_MAPPER = createJsonMapper();

    @Override
    public String resolve(ProceedingJoinPoint point, String namespace, String expression) {
        String actualNamespace = isNotBlank(namespace) ? namespace.trim() : DEFAULT_NAMESPACE;
        Method method = getMethod(point);
        String methodKey = methodKey(method);
        String businessKey = isNotBlank(expression)
                ? evaluateExpression(point, method, expression)
                : digest(methodKey + ":" + tryToJson(point.getArgs()));
        return String.join(":", actualNamespace, methodKey, businessKey);
    }

    private String evaluateExpression(ProceedingJoinPoint point, Method method, String expression) {
        try {
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    point.getTarget(), method, point.getArgs(), PARAMETER_NAME_DISCOVERER);
            context.setVariable("args", point.getArgs());
            Object value = PARSER.parseExpression(expression).getValue(context);
            if (value == null || String.valueOf(value).trim().isEmpty()) {
                throw new IllegalArgumentException("idempotent key expression result must not be blank");
            }
            return normalize(String.valueOf(value));
        } catch (Exception ex) {
            throw new IdempotentKeyResolveException("Failed to resolve idempotent key expression: " + expression, ex);
        }
    }

    private String tryToJson(Object value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException | RuntimeException ex) {
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

    private Method getMethod(ProceedingJoinPoint point) {
        if (point.getSignature() instanceof MethodSignature methodSignature) {
            return methodSignature.getMethod();
        }
        throw new IdempotentKeyResolveException("Idempotent key requires method signature", null);
    }

    private String methodKey(Method method) {
        String parameterTypes = Arrays.stream(method.getParameterTypes())
                .map(Class::getTypeName)
                .collect(Collectors.joining(","));
        return method.getDeclaringClass().getName() + "#" + method.getName() + "(" + parameterTypes + ")";
    }

    private String normalize(String value) {
        return value.trim()
                .replaceAll("[\\r\\n\\t ]+", "_")
                .replace(':', '_');
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

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static ObjectMapper createJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
}
