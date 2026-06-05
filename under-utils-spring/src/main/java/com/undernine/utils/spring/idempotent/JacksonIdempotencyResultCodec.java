package com.undernine.utils.spring.idempotent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.reflect.Type;

/**
 * 基于 Jackson 的幂等结果编解码器。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class JacksonIdempotencyResultCodec implements IdempotencyResultCodec {

    private final ObjectMapper objectMapper;

    public JacksonIdempotencyResultCodec() {
        this(createDefaultMapper());
    }

    public JacksonIdempotencyResultCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? createDefaultMapper() : objectMapper;
    }

    @Override
    public String serialize(Object result, Type resultType) {
        if (result == null || resultType == Void.TYPE || resultType == Void.class) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new IdempotencyException("Failed to serialize idempotent result", ex);
        }
    }

    @Override
    public Object deserialize(String payload, Type resultType) {
        if (payload == null || resultType == Void.TYPE || resultType == Void.class) {
            return null;
        }
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(resultType);
            return objectMapper.readValue(payload, javaType);
        } catch (JsonProcessingException ex) {
            throw new IdempotencyException("Failed to deserialize idempotent result", ex);
        }
    }

    private static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
