package com.undernine.utils.spring.idempotent;

import java.lang.reflect.Type;

/**
 * 幂等结果编解码器。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public interface IdempotencyResultCodec {

    /**
     * 序列化业务结果。
     *
     * @param result     业务结果
     * @param resultType 业务返回类型
     * @return 序列化结果
     */
    String serialize(Object result, Type resultType);

    /**
     * 反序列化业务结果。
     *
     * @param payload    序列化结果
     * @param resultType 业务返回类型
     * @return 业务结果
     */
    Object deserialize(String payload, Type resultType);
}
