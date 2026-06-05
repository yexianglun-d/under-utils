package com.undernine.utils.spring.idempotent;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 幂等 key 解析器。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public interface IdempotentKeyResolver {

    /**
     * 解析幂等 key。
     *
     * @param point      切点
     * @param namespace  命名空间
     * @param expression key 表达式，可为空
     * @return 幂等 key
     */
    String resolve(ProceedingJoinPoint point, String namespace, String expression);
}
