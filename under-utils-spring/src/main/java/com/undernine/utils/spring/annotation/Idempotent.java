package com.undernine.utils.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 服务层业务幂等注解。
 * <p>
 * 与 {@link PreventRepeat} 的入口防重复提交不同，本注解面向 MQ 重试、RPC 重试和跨服务回调等
 * 服务层重复执行场景。同一 key 首次执行中，重复调用会立即抛出处理中异常；首次成功完成后，
 * 后续相同 key 调用会直接返回第一次执行结果。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等命名空间。
     */
    String namespace() default "idempotent";

    /**
     * 幂等 key 表达式。
     * <p>
     * 为空时使用默认规则：方法签名 + 参数摘要。不为空时按 SpEL 解析，例如 {@code #args[0].orderNo}。
     * 表达式解析失败会直接抛出异常，避免用错误 key 继续执行业务。
     * </p>
     */
    String key() default "";

    /**
     * 首次执行中的状态保留时间。小于等于 0 时使用 starter 全局配置。
     */
    long processingTtl() default -1;

    /**
     * 首次执行中的状态保留时间单位。
     */
    TimeUnit processingTimeUnit() default TimeUnit.SECONDS;

    /**
     * 成功结果缓存时间。小于等于 0 时使用 starter 全局配置。
     */
    long resultTtl() default -1;

    /**
     * 成功结果缓存时间单位。
     */
    TimeUnit resultTimeUnit() default TimeUnit.SECONDS;

    /**
     * 业务方法执行失败时是否释放幂等 key。
     */
    boolean releaseOnFailure() default true;

    /**
     * 首次执行仍在处理中时抛出的异常消息。
     */
    String processingMessage() default "请求正在处理中";
}
