package com.undernine.utils.security.mask;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * Jackson 响应脱敏序列化器。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class MaskingJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final MaskType type;
    private final String customRule;

    public MaskingJsonSerializer() {
        this(null, null);
    }

    public MaskingJsonSerializer(MaskType type, String customRule) {
        this.type = type;
        this.customRule = customRule;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String masked = type == MaskType.CUSTOM
                ? MaskingUtils.custom(value, customRule)
                : MaskingUtils.mask(value, type);
        gen.writeString(masked);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            Mask mask = property.getAnnotation(Mask.class);
            if (mask == null) {
                mask = property.getContextAnnotation(Mask.class);
            }
            if (mask != null) {
                return new MaskingJsonSerializer(mask.type(), mask.customRule());
            }
        }
        return prov.findNullValueSerializer(null);
    }
}
