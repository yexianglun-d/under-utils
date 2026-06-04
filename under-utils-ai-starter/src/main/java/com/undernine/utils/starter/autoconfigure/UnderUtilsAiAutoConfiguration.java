package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.ai.AiClient;
import com.undernine.utils.ai.AiClientOptions;
import com.undernine.utils.ai.AiClientProvider;
import com.undernine.utils.ai.AiClientRegistry;
import com.undernine.utils.ai.AiProviderNames;
import com.undernine.utils.ai.DefaultAiClientRegistry;
import com.undernine.utils.ai.OpenAiCompatibleAiClient;
import com.undernine.utils.starter.properties.UnderUtilsAiProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模块自动装配。
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.2
 */
@AutoConfiguration
@ConditionalOnClass(AiClient.class)
@EnableConfigurationProperties(UnderUtilsAiProperties.class)
public class UnderUtilsAiAutoConfiguration {

    /**
     * 创建 AI client 注册表。
     *
     * @param properties AI 配置
     * @param aiClientProviders 自定义 AI client provider
     * @return AI client 注册表
     */
    @Bean
    @ConditionalOnMissingBean({AiClient.class, AiClientRegistry.class})
    @ConditionalOnProperty(prefix = "under.utils.ai", name = "enabled", havingValue = "true")
    public AiClientRegistry aiClientRegistry(UnderUtilsAiProperties properties,
                                             ObjectProvider<AiClientProvider> aiClientProviders) {
        List<AiClientProvider> providers = aiClientProviders.stream().toList();
        Map<String, AiClient> clients = new LinkedHashMap<>();
        if (properties.getClients().isEmpty()) {
            clients.put("default", createClient(properties.getProvider(),
                    buildOptions(properties, null), providers));
        } else {
            properties.getClients().forEach((name, clientProperties) -> clients.put(
                    AiClientRegistry.normalizeName(name),
                    createClient(resolveProvider(properties, clientProperties),
                            buildOptions(properties, clientProperties),
                            providers)
            ));
        }
        return new DefaultAiClientRegistry(clients, resolveDefaultName(properties, clients));
    }

    /**
     * 创建默认 AI client。
     *
     * @param aiClientRegistry AI client 注册表
     * @return 默认 AI client
     */
    @Bean
    @ConditionalOnMissingBean(AiClient.class)
    @ConditionalOnProperty(prefix = "under.utils.ai", name = "enabled", havingValue = "true")
    public AiClient aiClient(AiClientRegistry aiClientRegistry) {
        return aiClientRegistry.getDefaultClient();
    }

    /**
     * 创建默认 AI client。
     *
     * @param properties AI 配置
     * @param aiClientProviders 自定义 AI client provider
     * @return AI client
     * @deprecated 1.0.3 起自动装配通过 {@link #aiClientRegistry(UnderUtilsAiProperties, ObjectProvider)}
     * 和 {@link #aiClient(AiClientRegistry)} 暴露默认客户端；保留该方法用于兼容 1.0.2 已发布 public API。
     */
    @Deprecated(since = "1.0.3", forRemoval = false)
    public AiClient aiClient(UnderUtilsAiProperties properties,
                             ObjectProvider<AiClientProvider> aiClientProviders) {
        return createClient(resolveProvider(properties, null),
                buildOptions(properties, null),
                aiClientProviders.stream().toList());
    }

    private AiClient createClient(String providerName, AiClientOptions options, List<AiClientProvider> providers) {
        String provider = AiClientProvider.normalize(providerName);
        AiClientProvider customProvider = providers.stream()
                .filter(candidate -> candidate.supports(provider))
                .findFirst()
                .orElse(null);
        if (customProvider != null) {
            return customProvider.create(options);
        }
        if (AiProviderNames.OPENAI_COMPATIBLE.equals(provider)) {
            return new OpenAiCompatibleAiClient(options);
        }
        throw new IllegalArgumentException("unsupported AI provider: " + providerName);
    }

    private AiClientOptions buildOptions(UnderUtilsAiProperties properties,
                                         UnderUtilsAiProperties.Client clientProperties) {
        return AiClientOptions.builder()
                .baseUrl(resolve(properties.getBaseUrl(), clientProperties == null ? null : clientProperties.getBaseUrl()))
                .apiKey(resolve(properties.getApiKey(), clientProperties == null ? null : clientProperties.getApiKey()))
                .model(resolve(properties.getModel(), clientProperties == null ? null : clientProperties.getModel()))
                .chatCompletionsPath(resolve(properties.getChatCompletionsPath(),
                        clientProperties == null ? null : clientProperties.getChatCompletionsPath()))
                .timeout(clientProperties == null || clientProperties.getTimeout() == null
                        ? properties.getTimeout() : clientProperties.getTimeout())
                .streamReadTimeout(clientProperties == null || clientProperties.getStreamReadTimeout() == null
                        ? properties.getStreamReadTimeout() : clientProperties.getStreamReadTimeout())
                .maxRetries(clientProperties == null || clientProperties.getMaxRetries() == null
                        ? properties.getMaxRetries() : clientProperties.getMaxRetries())
                .retryInterval(clientProperties == null || clientProperties.getRetryInterval() == null
                        ? properties.getRetryInterval() : clientProperties.getRetryInterval())
                .temperature(clientProperties == null || clientProperties.getTemperature() == null
                        ? properties.getTemperature() : clientProperties.getTemperature())
                .maxTokens(clientProperties == null || clientProperties.getMaxTokens() == null
                        ? properties.getMaxTokens() : clientProperties.getMaxTokens())
                .headers(resolveHeaders(properties, clientProperties))
                .build();
    }

    private String resolveProvider(UnderUtilsAiProperties properties, UnderUtilsAiProperties.Client clientProperties) {
        return resolve(properties.getProvider(), clientProperties == null ? null : clientProperties.getProvider());
    }

    private Map<String, String> resolveHeaders(UnderUtilsAiProperties properties,
                                               UnderUtilsAiProperties.Client clientProperties) {
        Map<String, String> headers = new LinkedHashMap<>(properties.getHeaders());
        if (clientProperties != null) {
            headers.putAll(clientProperties.getHeaders());
        }
        return headers;
    }

    private String resolveDefaultName(UnderUtilsAiProperties properties, Map<String, AiClient> clients) {
        String configuredName = trimToNull(properties.getDefaultClient());
        if (configuredName != null) {
            return AiClientRegistry.normalizeName(configuredName);
        }
        if (clients.containsKey("default")) {
            return "default";
        }
        return clients.keySet().iterator().next();
    }

    private String resolve(String fallback, String value) {
        return value == null ? fallback : value;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
