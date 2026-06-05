package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.spring.aspect.PreventRepeatAspect;
import com.undernine.utils.spring.aspect.RateLimitAspect;
import com.undernine.utils.spring.aspect.IdempotentAspect;
import com.undernine.utils.spring.context.CurrentTenantProvider;
import com.undernine.utils.spring.context.CurrentUserProvider;
import com.undernine.utils.spring.context.DefaultCurrentTenantProvider;
import com.undernine.utils.spring.context.DefaultCurrentUserProvider;
import com.undernine.utils.spring.context.OperationContextFilter;
import com.undernine.utils.spring.context.OperationContextTaskDecorator;
import com.undernine.utils.spring.context.TraceIdProvider;
import com.undernine.utils.spring.exception.GlobalExceptionHandler;
import com.undernine.utils.spring.idempotent.IdempotencyResultCodec;
import com.undernine.utils.spring.idempotent.IdempotencyExecution;
import com.undernine.utils.spring.idempotent.IdempotencyException;
import com.undernine.utils.spring.idempotent.IdempotencyStore;
import com.undernine.utils.spring.idempotent.IdempotentKeyResolver;
import com.undernine.utils.spring.idempotent.LocalIdempotencyStore;
import com.undernine.utils.spring.key.OperationKeyResolver;
import com.undernine.utils.spring.ratelimit.LocalRateLimitStore;
import com.undernine.utils.spring.ratelimit.RateLimitStore;
import com.undernine.utils.spring.repeat.LocalRepeatSubmitStore;
import com.undernine.utils.spring.repeat.RepeatSubmitStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UnderUtilsSpringAutoConfiguration 测试。
 *
 * @author Under-Utils Team
 */
class UnderUtilsSpringAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UnderUtilsSpringAutoConfiguration.class));

    @Test
    void shouldAutoConfigureDefaultLocalComponents() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CurrentUserProvider.class);
            assertThat(context).hasSingleBean(CurrentTenantProvider.class);
            assertThat(context).hasSingleBean(TraceIdProvider.class);
            assertThat(context).hasSingleBean(OperationKeyResolver.class);
            assertThat(context).hasSingleBean(IdempotentKeyResolver.class);
            assertThat(context).hasSingleBean(IdempotencyResultCodec.class);
            assertThat(context).hasSingleBean(OperationContextFilter.class);
            assertThat(context).hasSingleBean(OperationContextTaskDecorator.class);
            assertThat(context).hasBean("underUtilsOperationContextFilterRegistration");
            assertThat(context).hasSingleBean(RateLimitStore.class);
            assertThat(context).hasSingleBean(RepeatSubmitStore.class);
            assertThat(context).hasSingleBean(IdempotencyStore.class);
            assertThat(context).hasSingleBean(RateLimitAspect.class);
            assertThat(context).hasSingleBean(PreventRepeatAspect.class);
            assertThat(context).hasSingleBean(IdempotentAspect.class);
            assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class);
            assertThat(context.getBean(RateLimitStore.class)).isInstanceOf(LocalRateLimitStore.class);
            assertThat(context.getBean(RepeatSubmitStore.class)).isInstanceOf(LocalRepeatSubmitStore.class);
            assertThat(context.getBean(IdempotencyStore.class)).isInstanceOf(LocalIdempotencyStore.class);
        });
    }

    @Test
    void shouldBindLocalStoreLimitsAndCleanupInterval() {
        contextRunner
                .withPropertyValues(
                        "under.utils.web.rate-limit.local-max-entries=1",
                        "under.utils.web.rate-limit.local-cleanup-interval=10ms",
                        "under.utils.web.repeat-submit.local-max-entries=1",
                        "under.utils.web.repeat-submit.local-cleanup-interval=10ms",
                        "under.utils.idempotent.local-max-entries=1",
                        "under.utils.idempotent.local-cleanup-interval=10ms"
                )
                .run(context -> {
                    RateLimitStore rateLimitStore = context.getBean(RateLimitStore.class);
                    RepeatSubmitStore repeatSubmitStore = context.getBean(RepeatSubmitStore.class);
                    IdempotencyStore idempotencyStore = context.getBean(IdempotencyStore.class);

                    assertThat(rateLimitStore.tryAcquire("rate:1", 1, Duration.ofSeconds(1))).isTrue();
                    assertThat(rateLimitStore.tryAcquire("rate:2", 1, Duration.ofSeconds(1))).isFalse();
                    assertThat(repeatSubmitStore.acquire("repeat:1", Duration.ofSeconds(1))).isTrue();
                    assertThat(repeatSubmitStore.acquire("repeat:2", Duration.ofSeconds(1))).isFalse();
                    assertThat(idempotencyStore.begin("idem:1", Duration.ofSeconds(1), String.class).isAcquired())
                            .isTrue();
                    assertThatThrownBy(() -> idempotencyStore.begin("idem:2", Duration.ofSeconds(1), String.class))
                            .isInstanceOf(IdempotencyException.class);
                });
    }

    @Test
    void shouldBackOffWhenUserProviderExists() {
        CurrentUserProvider customProvider = () -> "custom-user";

        contextRunner
                .withBean(CurrentUserProvider.class, () -> customProvider)
                .run(context -> assertThat(context.getBean(CurrentUserProvider.class)).isSameAs(customProvider));
    }

    @Test
    void shouldAllowOperationContextFilterToBeDisabled() {
        contextRunner
                .withPropertyValues("under.utils.web.operation-context.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OperationContextFilter.class);
                    assertThat(context).doesNotHaveBean("underUtilsOperationContextFilterRegistration");
                });
    }

    @Test
    void shouldPassTrustedIdentityHeaderSettingToDefaultProvidersAndFilter() {
        contextRunner
                .withPropertyValues(
                        "under.utils.web.operation-context.trusted-identity-headers=true",
                        "under.utils.web.operation-context.trusted-proxy-headers=true"
                )
                .run(context -> {
                    assertThat(context.getBean(CurrentUserProvider.class))
                            .isInstanceOfSatisfying(DefaultCurrentUserProvider.class,
                                    provider -> assertThat(provider.isTrustedIdentityHeaders()).isTrue());
                    assertThat(context.getBean(CurrentTenantProvider.class))
                            .isInstanceOfSatisfying(DefaultCurrentTenantProvider.class,
                                    provider -> assertThat(provider.isTrustedIdentityHeaders()).isTrue());
                    assertThat(context.getBean(OperationContextFilter.class).isTrustedIdentityHeaders()).isTrue();
                    assertThat(context.getBean(OperationContextFilter.class).isTrustedProxyHeaders()).isTrue();
                });
    }

    @Test
    void shouldKeepProxyHeadersUntrustedByDefault() {
        contextRunner.run(context ->
                assertThat(context.getBean(OperationContextFilter.class).isTrustedProxyHeaders()).isFalse());
    }

    @Test
    void shouldCreateGlobalExceptionHandlerOnlyWhenEnabled() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));

        contextRunner
                .withPropertyValues("under.utils.web.exception-handling.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    void shouldNotCreateTaskDecoratorWhenOperationContextTaskDecoratorDisabled() {
        contextRunner
                .withPropertyValues("under.utils.web.operation-context.task-decorator-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TaskDecorator.class);
                    assertThat(context).doesNotHaveBean(OperationContextTaskDecorator.class);
                });
    }

    @Test
    void shouldBackOffOperationContextTaskDecoratorWhenUserTaskDecoratorExists() {
        TaskDecorator customDecorator = runnable -> runnable;

        contextRunner
                .withBean(TaskDecorator.class, () -> customDecorator)
                .run(context -> {
                    assertThat(context).hasSingleBean(TaskDecorator.class);
                    assertThat(context.getBean(TaskDecorator.class)).isSameAs(customDecorator);
                    assertThat(context).doesNotHaveBean(OperationContextTaskDecorator.class);
                });
    }

    @Test
    void shouldBackOffRateLimitStoreWhenUserStoreExists() {
        RateLimitStore customStore = (key, limit, window) -> true;

        contextRunner
                .withBean(RateLimitStore.class, () -> customStore)
                .run(context -> {
                    assertThat(context).hasSingleBean(RateLimitStore.class);
                    assertThat(context.getBean(RateLimitStore.class)).isSameAs(customStore);
                    assertThat(context).hasSingleBean(RateLimitAspect.class);
                });
    }

    @Test
    void shouldBackOffRepeatSubmitStoreWhenUserStoreExists() {
        RepeatSubmitStore customStore = new RepeatSubmitStore() {
            @Override
            public boolean acquire(String key, Duration ttl) {
                return true;
            }

            @Override
            public void release(String key) {
            }
        };

        contextRunner
                .withBean(RepeatSubmitStore.class, () -> customStore)
                .run(context -> {
                    assertThat(context).hasSingleBean(RepeatSubmitStore.class);
                    assertThat(context.getBean(RepeatSubmitStore.class)).isSameAs(customStore);
                    assertThat(context).hasSingleBean(PreventRepeatAspect.class);
                });
    }

    @Test
    void shouldBackOffIdempotencyStoreWhenUserStoreExists() {
        IdempotencyStore customStore = new IdempotencyStore() {
            @Override
            public IdempotencyExecution begin(String key, Duration processingTtl, java.lang.reflect.Type resultType) {
                return IdempotencyExecution.acquired();
            }

            @Override
            public boolean complete(String key,
                                    String executionToken,
                                    Object result,
                                    java.lang.reflect.Type resultType,
                                    Duration resultTtl) {
                return true;
            }
        };

        contextRunner
                .withBean(IdempotencyStore.class, () -> customStore)
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotencyStore.class);
                    assertThat(context.getBean(IdempotencyStore.class)).isSameAs(customStore);
                    assertThat(context).hasSingleBean(IdempotentAspect.class);
                });
    }

    @Test
    void shouldDisableIdempotentAspectWhenConfigured() {
        contextRunner
                .withPropertyValues("under.utils.idempotent.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(IdempotentAspect.class));
    }
}
