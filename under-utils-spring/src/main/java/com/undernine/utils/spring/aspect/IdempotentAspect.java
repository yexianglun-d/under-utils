package com.undernine.utils.spring.aspect;

import com.undernine.utils.spring.annotation.Idempotent;
import com.undernine.utils.spring.idempotent.DefaultIdempotentKeyResolver;
import com.undernine.utils.spring.idempotent.IdempotencyExecution;
import com.undernine.utils.spring.idempotent.IdempotencyStore;
import com.undernine.utils.spring.idempotent.IdempotentInProgressException;
import com.undernine.utils.spring.idempotent.IdempotentKeyResolver;
import com.undernine.utils.spring.idempotent.LocalIdempotencyStore;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.time.Duration;

/**
 * 服务层业务幂等切面。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect implements AutoCloseable {

    private volatile IdempotencyStore idempotencyStore;
    private volatile boolean defaultStoreOwned;
    private IdempotentKeyResolver keyResolver = new DefaultIdempotentKeyResolver();
    private Duration defaultProcessingTtl = Duration.ofSeconds(30);
    private Duration defaultResultTtl = Duration.ofMinutes(5);

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint point, Idempotent idempotent) throws Throwable {
        String key = keyResolver.resolve(point, idempotent.namespace(), idempotent.key());
        Type returnType = returnType(point);
        Duration processingTtl = ttl(idempotent.processingTtl(), idempotent.processingTimeUnit().toMillis(1),
                defaultProcessingTtl);
        Duration resultTtl = ttl(idempotent.resultTtl(), idempotent.resultTimeUnit().toMillis(1), defaultResultTtl);
        IdempotencyStore store = getIdempotencyStore();

        IdempotencyExecution execution = store.begin(key, processingTtl, returnType);
        if (execution.isCompleted()) {
            return execution.getResult();
        }
        if (execution.isInProgress()) {
            log.warn("【业务幂等】重复调用仍在处理中: {}", key);
            throw new IdempotentInProgressException(idempotent.processingMessage());
        }

        Object result;
        try {
            result = point.proceed();
        } catch (Throwable ex) {
            if (idempotent.releaseOnFailure()) {
                store.release(key, execution.getExecutionToken());
            }
            throw ex;
        }

        boolean completed = store.complete(key, execution.getExecutionToken(), result, returnType, resultTtl);
        if (!completed) {
            log.warn("【业务幂等】首次执行结果未写入幂等完成态，key owner 已过期或被替换: {}", key);
        }
        return result;
    }

    @Autowired(required = false)
    public synchronized void setIdempotencyStore(IdempotencyStore idempotencyStore) {
        if (idempotencyStore != null) {
            closeDefaultStore();
            this.idempotencyStore = idempotencyStore;
            this.defaultStoreOwned = false;
        }
    }

    @Autowired(required = false)
    public void setKeyResolver(IdempotentKeyResolver keyResolver) {
        if (keyResolver != null) {
            this.keyResolver = keyResolver;
        }
    }

    public void setDefaultProcessingTtl(Duration defaultProcessingTtl) {
        this.defaultProcessingTtl = normalizeDuration(defaultProcessingTtl, Duration.ofSeconds(30));
    }

    public void setDefaultResultTtl(Duration defaultResultTtl) {
        this.defaultResultTtl = normalizeDuration(defaultResultTtl, Duration.ofMinutes(5));
    }

    @Override
    public synchronized void close() {
        closeDefaultStore();
    }

    private IdempotencyStore getIdempotencyStore() {
        IdempotencyStore store = idempotencyStore;
        if (store != null) {
            return store;
        }
        synchronized (this) {
            if (idempotencyStore == null) {
                idempotencyStore = new LocalIdempotencyStore();
                defaultStoreOwned = true;
            }
            return idempotencyStore;
        }
    }

    private Type returnType(ProceedingJoinPoint point) {
        if (point.getSignature() instanceof MethodSignature signature) {
            return signature.getMethod().getGenericReturnType();
        }
        return Object.class;
    }

    private Duration ttl(long value, long unitMillis, Duration defaultTtl) {
        if (value <= 0) {
            return defaultTtl;
        }
        long ttlMillis = Math.max(1L, value * unitMillis);
        return Duration.ofMillis(ttlMillis);
    }

    private Duration normalizeDuration(Duration value, Duration defaultValue) {
        if (value == null || value.isZero() || value.isNegative()) {
            return defaultValue;
        }
        return value;
    }

    private void closeDefaultStore() {
        if (defaultStoreOwned && idempotencyStore instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ex) {
                log.warn("Failed to close default idempotency store", ex);
            }
        }
        defaultStoreOwned = false;
    }
}
