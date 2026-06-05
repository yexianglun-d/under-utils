package com.undernine.utils.security.mask;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 响应字段脱敏注解。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize(using = MaskingJsonSerializer.class)
public @interface Mask {

    /**
     * 脱敏类型。
     */
    MaskType type();

    /**
     * 自定义脱敏规则，格式为 {@code prefixLen,suffixLen}。
     */
    String customRule() default "";
}
