package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.redis.cache.CacheAsideTemplate;
import com.undernine.utils.redis.cache.CacheOperationEvent;
import com.undernine.utils.redis.cache.CacheOperationObserver;
import com.undernine.utils.redis.cache.CacheOptions;
import com.undernine.utils.redis.cache.CacheValueCodec;
import com.undernine.utils.redis.cache.LogicalExpireCacheOptions;
import com.undernine.utils.redis.cache.LogicalExpireCacheTemplate;
import com.undernine.utils.redis.cache.MicrometerCacheOperationObserver;
import com.undernine.utils.redis.idempotent.RedisIdempotencyStore;
import com.undernine.utils.redis.lock.DistributedLockTemplate;
import com.undernine.utils.redis.ratelimit.RedisRateLimitStore;
import com.undernine.utils.redis.repeat.RedisRepeatSubmitStore;
import com.undernine.utils.spring.aspect.IdempotentAspect;
import com.undernine.utils.spring.aspect.PreventRepeatAspect;
import com.undernine.utils.spring.aspect.RateLimitAspect;
import com.undernine.utils.spring.context.CurrentTenantProvider;
import com.undernine.utils.spring.context.CurrentUserProvider;
import com.undernine.utils.spring.context.OperationContextFilter;
import com.undernine.utils.spring.context.OperationContextTaskDecorator;
import com.undernine.utils.spring.context.TraceIdProvider;
import com.undernine.utils.spring.idempotent.IdempotencyStore;
import com.undernine.utils.spring.idempotent.LocalIdempotencyStore;
import com.undernine.utils.spring.key.OperationKeyResolver;
import com.undernine.utils.spring.ratelimit.LocalRateLimitStore;
import com.undernine.utils.spring.ratelimit.RateLimitStore;
import com.undernine.utils.spring.repeat.LocalRepeatSubmitStore;
import com.undernine.utils.spring.repeat.RepeatSubmitStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.redisson.api.RedissonClient;
import org.redisson.api.RBucket;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UnderUtilsRedisAutoConfiguration 测试。
 *
 * @author Under-Utils Team
 */
class UnderUtilsRedisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    UnderUtilsSpringAutoConfiguration.class,
                    UnderUtilsRedisAutoConfiguration.class));

    @Test
    void shouldAutoConfigureDefaultLocalComponents() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CurrentUserProvider.class);
            assertThat(context).hasSingleBean(CurrentTenantProvider.class);
            assertThat(context).hasSingleBean(TraceIdProvider.class);
            assertThat(context).hasSingleBean(OperationKeyResolver.class);
            assertThat(context).hasSingleBean(OperationContextFilter.class);
            assertThat(context).hasSingleBean(OperationContextTaskDecorator.class);
            assertThat(context).hasBean("underUtilsOperationContextFilterRegistration");
            assertThat(context).hasSingleBean(RateLimitStore.class);
            assertThat(context).hasSingleBean(RepeatSubmitStore.class);
            assertThat(context).hasSingleBean(IdempotencyStore.class);
            assertThat(context).hasSingleBean(RateLimitAspect.class);
            assertThat(context).hasSingleBean(PreventRepeatAspect.class);
            assertThat(context).hasSingleBean(IdempotentAspect.class);
            assertThat(context.getBean(RateLimitStore.class)).isInstanceOf(LocalRateLimitStore.class);
            assertThat(context.getBean(RepeatSubmitStore.class)).isInstanceOf(LocalRepeatSubmitStore.class);
            assertThat(context.getBean(IdempotencyStore.class)).isInstanceOf(LocalIdempotencyStore.class);
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
    void shouldUseRedisStoresWhenConfiguredAndRedissonExists() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "under.utils.web.rate-limit.store=redis",
                        "under.utils.web.repeat-submit.store=redis",
                        "under.utils.idempotent.store=redis"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RateLimitStore.class);
                    assertThat(context).hasSingleBean(RepeatSubmitStore.class);
                    assertThat(context).hasSingleBean(IdempotencyStore.class);
                    assertThat(context.getBean(RateLimitStore.class)).isInstanceOf(RedisRateLimitStore.class);
                    assertThat(context.getBean(RepeatSubmitStore.class)).isInstanceOf(RedisRepeatSubmitStore.class);
                    assertThat(context.getBean(IdempotencyStore.class)).isInstanceOf(RedisIdempotencyStore.class);
                    assertThat(context).hasSingleBean(DistributedLockTemplate.class);
                    assertThat(context).hasSingleBean(RateLimitAspect.class);
                    assertThat(context).hasSingleBean(PreventRepeatAspect.class);
                    assertThat(context).hasSingleBean(IdempotentAspect.class);
                });
    }

    @Test
    void shouldFailWhenRedisStoresAreRequestedWithoutRedissonClient() {
        contextRunner
                .withPropertyValues(
                        "under.utils.web.rate-limit.store=redis",
                        "under.utils.web.repeat-submit.store=redis",
                        "under.utils.idempotent.store=redis"
                )
                .run(context -> assertThat(context).hasFailed());
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
    void shouldBackOffDistributedLockTemplateWhenUserTemplateExists() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        DistributedLockTemplate customTemplate = new DistributedLockTemplate(redissonClient, "custom:lock:");

        contextRunner
                .withBean(RedissonClient.class, () -> redissonClient)
                .withBean(DistributedLockTemplate.class, () -> customTemplate)
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockTemplate.class);
                    assertThat(context.getBean(DistributedLockTemplate.class)).isSameAs(customTemplate);
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAutoConfigureRedisCacheTemplateWhenRedissonExists() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "under.utils.redis.cache.key-prefix=custom:cache:",
                        "under.utils.redis.cache.ttl=30s",
                        "under.utils.redis.cache.jitter=2s"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheValueCodec.class);
                    assertThat(context).hasSingleBean(CacheOptions.class);
                    assertThat(context).hasSingleBean(CacheAsideTemplate.class);
                    assertThat(context).doesNotHaveBean(LogicalExpireCacheTemplate.class);
                    CacheOptions options = context.getBean(CacheOptions.class);
                    assertThat(options.keyPrefix()).isEqualTo("custom:cache:");
                    assertThat(options.ttl()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(options.jitter()).isEqualTo(Duration.ofSeconds(2));

                    FilterRegistrationBean<OperationContextFilter> registration =
                            context.getBean("underUtilsOperationContextFilterRegistration", FilterRegistrationBean.class);
                    assertThat(registration.getOrder()).isEqualTo(Integer.MIN_VALUE + 100);
                });
    }

    @Test
    void shouldBackOffCacheValueCodecWhenUserCodecExists() {
        CacheValueCodec customCodec = new CacheValueCodec() {
            @Override
            public String encode(Object value) {
                return String.valueOf(value);
            }

            @Override
            public <T> T decode(String payload, Class<T> valueType) {
                return null;
            }
        };

        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(CacheValueCodec.class, () -> customCodec)
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheValueCodec.class);
                    assertThat(context.getBean(CacheValueCodec.class)).isSameAs(customCodec);
                    assertThat(context).hasSingleBean(CacheAsideTemplate.class);
                });
    }

    @Test
    void shouldBackOffCacheOptionsWhenUserOptionsExist() {
        CacheOptions customOptions = CacheOptions.builder()
                .ttl(Duration.ofMinutes(10))
                .keyPrefix("custom:options:")
                .build();

        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(CacheOptions.class, () -> customOptions)
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheOptions.class);
                    assertThat(context.getBean(CacheOptions.class)).isSameAs(customOptions);
                    assertThat(context).hasSingleBean(CacheAsideTemplate.class);
                });
    }

    @Test
    void shouldBackOffCacheOptionsAndTemplateWhenUserTemplateExists() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        CacheAsideTemplate customTemplate = new CacheAsideTemplate(redissonClient);

        contextRunner
                .withBean(RedissonClient.class, () -> redissonClient)
                .withBean(CacheAsideTemplate.class, () -> customTemplate)
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheAsideTemplate.class);
                    assertThat(context.getBean(CacheAsideTemplate.class)).isSameAs(customTemplate);
                    assertThat(context).doesNotHaveBean(CacheOptions.class);
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldInjectCacheOperationObserverIntoCacheAsideTemplate() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = mock(RBucket.class);
        AtomicInteger writeEvents = new AtomicInteger();
        CacheOperationObserver observer = new CacheOperationObserver() {
            @Override
            public void onWrite(CacheOperationEvent event) {
                writeEvents.incrementAndGet();
            }
        };

        when(redissonClient.<String>getBucket(anyString())).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);
        doAnswer(invocation -> null).when(bucket).set(anyString(), any(Duration.class));

        contextRunner
                .withBean(RedissonClient.class, () -> redissonClient)
                .withBean(CacheOperationObserver.class, () -> observer)
                .withPropertyValues("under.utils.redis.cache.rebuild-lock-enabled=false")
                .run(context -> {
                    CacheAsideTemplate template = context.getBean(CacheAsideTemplate.class);
                    String value = template.getOrLoad("observer", String.class, key -> "fresh");

                    assertThat(value).isEqualTo("fresh");
                    assertThat(writeEvents).hasValue(1);
                });
    }

    @Test
    void shouldAutoConfigureMicrometerCacheObserverWhenMeterRegistryExists() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheOperationObserver.class);
                    assertThat(context.getBean(CacheOperationObserver.class))
                            .isInstanceOf(MicrometerCacheOperationObserver.class);
                });
    }

    @Test
    void shouldBackOffMicrometerCacheObserverWhenUserObserverExists() {
        CacheOperationObserver customObserver = CacheOperationObserver.noop();

        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(CacheOperationObserver.class, () -> customObserver)
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheOperationObserver.class);
                    assertThat(context.getBean(CacheOperationObserver.class)).isSameAs(customObserver);
                });
    }

    @Test
    void shouldAllowMicrometerCacheObserverToBeDisabled() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("under.utils.redis.observation.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CacheOperationObserver.class));
    }

    @Test
    void shouldNotCreateMicrometerCacheObserverWhenCacheFeaturesDisabled() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "under.utils.redis.cache.enabled=false",
                        "under.utils.redis.logical-cache.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(CacheOperationObserver.class));
    }

    @Test
    void shouldBackOffLogicalCacheOptionsWhenUserOptionsExist() {
        LogicalExpireCacheOptions customOptions = LogicalExpireCacheOptions.builder()
                .logicalTtl(Duration.ofSeconds(20))
                .physicalTtl(Duration.ofMinutes(2))
                .refreshExecutor(Runnable::run)
                .build();

        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(LogicalExpireCacheOptions.class, () -> customOptions)
                .withPropertyValues("under.utils.redis.logical-cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(LogicalExpireCacheOptions.class);
                    assertThat(context.getBean(LogicalExpireCacheOptions.class)).isSameAs(customOptions);
                    assertThat(context).hasSingleBean(LogicalExpireCacheTemplate.class);
                });
    }

    @Test
    void shouldBackOffLogicalCacheOptionsAndTemplateWhenUserTemplateExists() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        LogicalExpireCacheTemplate customTemplate = new LogicalExpireCacheTemplate(redissonClient);

        contextRunner
                .withBean(RedissonClient.class, () -> redissonClient)
                .withBean(LogicalExpireCacheTemplate.class, () -> customTemplate)
                .withPropertyValues("under.utils.redis.logical-cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(LogicalExpireCacheTemplate.class);
                    assertThat(context.getBean(LogicalExpireCacheTemplate.class)).isSameAs(customTemplate);
                    assertThat(context).doesNotHaveBean(LogicalExpireCacheOptions.class);
                });
    }

    @Test
    void shouldNotCreateCacheCodecWhenAllRedisCacheFeaturesDisabled() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "under.utils.redis.cache.enabled=false",
                        "under.utils.redis.logical-cache.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CacheValueCodec.class);
                    assertThat(context).doesNotHaveBean(CacheOptions.class);
                    assertThat(context).doesNotHaveBean(CacheAsideTemplate.class);
                    assertThat(context).doesNotHaveBean(LogicalExpireCacheTemplate.class);
                });
    }

    @Test
    void shouldAutoConfigureLogicalCacheTemplateWhenCacheAsideDisabled() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "under.utils.redis.cache.enabled=false",
                        "under.utils.redis.logical-cache.enabled=true",
                        "under.utils.redis.logical-cache.key-prefix=custom:logical:",
                        "under.utils.redis.logical-cache.logical-ttl=20s",
                        "under.utils.redis.logical-cache.physical-ttl=2m",
                        "under.utils.redis.logical-cache.core-pool-size=1",
                        "under.utils.redis.logical-cache.max-pool-size=2",
                        "under.utils.redis.logical-cache.queue-capacity=16"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CacheOptions.class);
                    assertThat(context).doesNotHaveBean(CacheAsideTemplate.class);
                    assertThat(context).hasSingleBean(CacheValueCodec.class);
                    assertThat(context).hasSingleBean(LogicalExpireCacheOptions.class);
                    assertThat(context).hasSingleBean(LogicalExpireCacheTemplate.class);
                    assertThat(context).hasBean("underUtilsLogicalCacheRefreshExecutor");

                    LogicalExpireCacheOptions options = context.getBean(LogicalExpireCacheOptions.class);
                    assertThat(options.keyPrefix()).isEqualTo("custom:logical:");
                    assertThat(options.logicalTtl()).isEqualTo(Duration.ofSeconds(20));
                    assertThat(options.physicalTtl()).isEqualTo(Duration.ofMinutes(2));

                    ThreadPoolTaskExecutor executor = context.getBean("underUtilsLogicalCacheRefreshExecutor",
                            ThreadPoolTaskExecutor.class);
                    assertThat(executor.getCorePoolSize()).isEqualTo(1);
                    assertThat(executor.getMaxPoolSize()).isEqualTo(2);
                });
    }

    @Test
    void shouldRejectLogicalCachePhysicalTtlNotLongerThanLogicalTtl() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "under.utils.redis.logical-cache.enabled=true",
                        "under.utils.redis.logical-cache.logical-ttl=30s",
                        "under.utils.redis.logical-cache.physical-ttl=30s"
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
