package com.undernine.utils.spring.idempotent;

import java.lang.reflect.Type;
import java.time.Duration;

/**
 * 业务幂等状态存储。
 * <p>
 * 实现需要保证 {@link #begin(String, Duration, Type)} 的原子登记语义：同一 key 只有一个调用
 * 能获得 {@link IdempotencyStatus#ACQUIRED} 状态。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public interface IdempotencyStore {

    /**
     * 开始一次幂等执行。
     *
     * @param key           幂等 key
     * @param processingTtl 首次执行中状态 TTL
     * @param resultType    业务方法返回类型
     * @return 当前 key 的登记结果。获得 {@link IdempotencyStatus#ACQUIRED} 时应携带本次执行 token。
     */
    IdempotencyExecution begin(String key, Duration processingTtl, Type resultType);

    /**
     * 标记首次执行成功完成，并保存结果。
     *
     * @param key            幂等 key
     * @param executionToken {@link #begin(String, Duration, Type)} 返回的本次执行 token
     * @param result         业务方法结果
     * @param resultType     业务方法返回类型
     * @param resultTtl      成功结果 TTL
     * @return true 表示当前执行仍是 key 的 owner，并成功写入完成结果；false 表示 owner 已过期或被替换
     */
    boolean complete(String key, String executionToken, Object result, Type resultType, Duration resultTtl);

    /**
     * 释放幂等 key。
     *
     * @param key            幂等 key
     * @param executionToken {@link #begin(String, Duration, Type)} 返回的本次执行 token
     */
    default void release(String key, String executionToken) {
        // Default no-op.
    }
}
