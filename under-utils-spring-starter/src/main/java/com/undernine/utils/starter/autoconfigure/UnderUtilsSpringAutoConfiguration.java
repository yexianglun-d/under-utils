package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.spring.aspect.PreventRepeatAspect;
import com.undernine.utils.spring.aspect.RateLimitAspect;
import com.undernine.utils.spring.context.CurrentTenantProvider;
import com.undernine.utils.spring.context.CurrentUserProvider;
import com.undernine.utils.spring.context.DefaultCurrentTenantProvider;
import com.undernine.utils.spring.context.DefaultCurrentUserProvider;
import com.undernine.utils.spring.context.DefaultTraceIdProvider;
import com.undernine.utils.spring.context.OperationContextCustomizer;
import com.undernine.utils.spring.context.OperationContextFilter;
import com.undernine.utils.spring.context.OperationContextTaskDecorator;
import com.undernine.utils.spring.context.TraceIdProvider;
import com.undernine.utils.spring.exception.GlobalExceptionHandler;
import com.undernine.utils.spring.aspect.IdempotentAspect;
import com.undernine.utils.spring.idempotent.DefaultIdempotentKeyResolver;
import com.undernine.utils.spring.idempotent.IdempotencyObserver;
import com.undernine.utils.spring.idempotent.IdempotencyStore;
import com.undernine.utils.spring.idempotent.IdempotencyResultCodec;
import com.undernine.utils.spring.idempotent.IdempotentKeyResolver;
import com.undernine.utils.spring.idempotent.JacksonIdempotencyResultCodec;
import com.undernine.utils.spring.idempotent.LocalIdempotencyStore;
import com.undernine.utils.spring.idempotent.MicrometerIdempotencyObserver;
import com.undernine.utils.spring.key.DefaultOperationKeyResolver;
import com.undernine.utils.spring.key.OperationKeyResolver;
import com.undernine.utils.spring.ratelimit.LocalRateLimitStore;
import com.undernine.utils.spring.ratelimit.RateLimitStore;
import com.undernine.utils.spring.repeat.LocalRepeatSubmitStore;
import com.undernine.utils.spring.repeat.RepeatSubmitStore;
import com.undernine.utils.starter.properties.UnderUtilsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.Collection;

/**
 * Under-Utils Spring 自动配置入口。
 *
 * @author Under-Utils Team
 * @version 1.0.2
 * @since 1.0.2
 */
@AutoConfiguration
@EnableConfigurationProperties(UnderUtilsProperties.class)
public class UnderUtilsSpringAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CurrentUserProvider currentUserProvider(UnderUtilsProperties properties) {
        return new DefaultCurrentUserProvider(
                properties.getWeb().getOperationContext().isTrustedIdentityHeaders());
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentTenantProvider currentTenantProvider(UnderUtilsProperties properties) {
        return new DefaultCurrentTenantProvider(
                properties.getWeb().getOperationContext().isTrustedIdentityHeaders());
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceIdProvider traceIdProvider() {
        return new DefaultTraceIdProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public OperationKeyResolver operationKeyResolver(CurrentUserProvider currentUserProvider,
                                                     CurrentTenantProvider currentTenantProvider) {
        return new DefaultOperationKeyResolver(currentUserProvider, currentTenantProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotentKeyResolver idempotentKeyResolver() {
        return new DefaultIdempotentKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyResultCodec idempotencyResultCodec() {
        return new JacksonIdempotencyResultCodec();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OperationContextFilter.class)
    @ConditionalOnProperty(prefix = "under.utils.web.operation-context", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OperationContextFilter operationContextFilter(Collection<OperationContextCustomizer> customizers,
                                                         UnderUtilsProperties properties) {
        return new OperationContextFilter(
                customizers,
                properties.getWeb().getOperationContext().isTrustedIdentityHeaders(),
                properties.getWeb().getOperationContext().isTrustedProxyHeaders());
    }

    @Bean
    @ConditionalOnMissingBean(name = "underUtilsOperationContextFilterRegistration")
    @ConditionalOnClass(FilterRegistrationBean.class)
    @ConditionalOnBean(OperationContextFilter.class)
    @ConditionalOnProperty(prefix = "under.utils.web.operation-context", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<OperationContextFilter> underUtilsOperationContextFilterRegistration(
            OperationContextFilter operationContextFilter,
            UnderUtilsProperties properties) {
        FilterRegistrationBean<OperationContextFilter> registration = new FilterRegistrationBean<>(operationContextFilter);
        registration.setName("underUtilsOperationContextFilter");
        registration.setOrder(properties.getWeb().getOperationContext().getOrder());
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    @ConditionalOnProperty(prefix = "under.utils.web.operation-context",
            name = {"enabled", "task-decorator-enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public OperationContextTaskDecorator operationContextTaskDecorator() {
        return new OperationContextTaskDecorator();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.web.exception-handling",
            name = "enabled",
            havingValue = "true")
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.web.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitAspect rateLimitAspect(RateLimitStore rateLimitStore, OperationKeyResolver operationKeyResolver) {
        RateLimitAspect aspect = new RateLimitAspect();
        aspect.setRateLimitStore(rateLimitStore);
        aspect.setOperationKeyResolver(operationKeyResolver);
        return aspect;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.web.repeat-submit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PreventRepeatAspect preventRepeatAspect(RepeatSubmitStore repeatSubmitStore,
                                                   OperationKeyResolver operationKeyResolver) {
        PreventRepeatAspect aspect = new PreventRepeatAspect();
        aspect.setRepeatSubmitStore(repeatSubmitStore);
        aspect.setOperationKeyResolver(operationKeyResolver);
        return aspect;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IdempotentAspect idempotentAspect(IdempotencyStore idempotencyStore,
                                             IdempotentKeyResolver idempotentKeyResolver,
                                             UnderUtilsProperties properties,
                                             ObjectProvider<IdempotencyObserver> idempotencyObserver) {
        IdempotentAspect aspect = new IdempotentAspect();
        aspect.setIdempotencyStore(idempotencyStore);
        aspect.setKeyResolver(idempotentKeyResolver);
        aspect.setIdempotencyObserver(idempotencyObserver.getIfAvailable(IdempotencyObserver::noop));
        aspect.setDefaultProcessingTtl(properties.getIdempotent().getProcessingTtl());
        aspect.setDefaultResultTtl(properties.getIdempotent().getResultTtl());
        return aspect;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
            "io.micrometer.core.instrument.MeterRegistry",
            "io.micrometer.observation.ObservationRegistry",
            "com.undernine.utils.spring.idempotent.MicrometerIdempotencyObserver"
    })
    static class IdempotencyObservationConfiguration {

        @Bean
        @ConditionalOnMissingBean(IdempotencyObserver.class)
        @ConditionalOnBean(MeterRegistry.class)
        @ConditionalOnProperty(prefix = "under.utils.idempotent.observation", name = "enabled",
                havingValue = "true", matchIfMissing = true)
        public IdempotencyObserver micrometerIdempotencyObserver(
                MeterRegistry meterRegistry,
                ObjectProvider<ObservationRegistry> observationRegistry) {
            return new MicrometerIdempotencyObserver(
                    meterRegistry,
                    observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP)
            );
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LocalStateConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "under.utils.web.rate-limit", name = "store", havingValue = "local", matchIfMissing = true)
        public RateLimitStore localRateLimitStore(UnderUtilsProperties properties) {
            UnderUtilsProperties.StoreCapability rateLimit = properties.getWeb().getRateLimit();
            return new LocalRateLimitStore(rateLimit.getLocalMaxEntries(), rateLimit.getLocalCleanupInterval());
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "under.utils.web.repeat-submit", name = "store", havingValue = "local", matchIfMissing = true)
        public RepeatSubmitStore localRepeatSubmitStore(UnderUtilsProperties properties) {
            UnderUtilsProperties.StoreCapability repeatSubmit = properties.getWeb().getRepeatSubmit();
            return new LocalRepeatSubmitStore(repeatSubmit.getLocalMaxEntries(),
                    repeatSubmit.getLocalCleanupInterval());
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "under.utils.idempotent", name = "store", havingValue = "local", matchIfMissing = true)
        public IdempotencyStore localIdempotencyStore(UnderUtilsProperties properties) {
            UnderUtilsProperties.Idempotent idempotent = properties.getIdempotent();
            return new LocalIdempotencyStore(
                    idempotent.getLocalMaxEntries(),
                    idempotent.getLocalCleanupInterval(),
                    idempotent.getKeyPrefix()
            );
        }
    }

}
