package com.undernine.utils.spring.idempotent;

/**
 * 业务幂等观察者 SPI。
 *
 * @author Under-Utils Team
 * @version 1.0.5
 * @since 1.0.5
 */
public interface IdempotencyObserver {

    /**
     * 接收一次幂等事件。
     *
     * @param event 幂等事件
     */
    void onEvent(IdempotencyEvent event);

    /**
     * 返回空观察者。
     *
     * @return 空观察者
     */
    static IdempotencyObserver noop() {
        return NoopIdempotencyObserver.INSTANCE;
    }

    final class NoopIdempotencyObserver implements IdempotencyObserver {
        private static final NoopIdempotencyObserver INSTANCE = new NoopIdempotencyObserver();

        private NoopIdempotencyObserver() {
        }

        @Override
        public void onEvent(IdempotencyEvent event) {
            // No-op.
        }
    }
}
