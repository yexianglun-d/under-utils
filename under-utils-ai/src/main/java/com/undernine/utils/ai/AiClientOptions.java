package com.undernine.utils.ai;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * AI 客户端配置。
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.2
 */
public final class AiClientOptions {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ZERO;
    private static final Duration DEFAULT_STREAM_READ_TIMEOUT = Duration.ZERO;

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String chatCompletionsPath;
    private final Duration timeout;
    private final Duration streamReadTimeout;
    private final int maxRetries;
    private final Duration retryInterval;
    private final Double temperature;
    private final Integer maxTokens;
    private final Map<String, String> headers;

    private AiClientOptions(Builder builder) {
        this.baseUrl = normalizeBaseUrl(requireText(builder.baseUrl, "baseUrl"));
        this.apiKey = trimToNull(builder.apiKey);
        this.model = requireText(builder.model, "model");
        this.chatCompletionsPath = normalizePath(builder.chatCompletionsPath);
        this.timeout = requireNonNegative(builder.timeout, "timeout");
        this.streamReadTimeout = requireNonNegative(builder.streamReadTimeout, "streamReadTimeout");
        this.maxRetries = builder.maxRetries;
        this.retryInterval = requireNonNegative(builder.retryInterval, "retryInterval");
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
    }

    /**
     * 创建配置构建器。
     *
     * @return 配置构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 基于当前配置创建构建器。
     *
     * @return 配置构建器
     */
    public Builder toBuilder() {
        return builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .chatCompletionsPath(chatCompletionsPath)
                .timeout(timeout)
                .streamReadTimeout(streamReadTimeout)
                .maxRetries(maxRetries)
                .retryInterval(retryInterval)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .headers(headers);
    }

    /**
     * 模型服务基础地址。
     *
     * @return 基础地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * API key。可能为空，为空时不会发送 Authorization header。
     *
     * @return API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 默认模型名称。
     *
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * Chat Completions 路径。
     *
     * @return Chat Completions 路径
     */
    public String getChatCompletionsPath() {
        return chatCompletionsPath;
    }

    /**
     * HTTP 超时时间。
     *
     * @return HTTP 超时时间
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * 流式响应读取超时时间。为 0 时表示不限制 SSE 分片读取等待时间。
     *
     * @return 流式读取超时时间
     */
    public Duration getStreamReadTimeout() {
        return streamReadTimeout;
    }

    /**
     * 最大重试次数。
     *
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 重试间隔。
     *
     * @return 重试间隔
     */
    public Duration getRetryInterval() {
        return retryInterval;
    }

    /**
     * 默认采样温度。
     *
     * @return 默认采样温度
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     * 默认最大输出 token 数。
     *
     * @return 默认最大输出 token 数
     */
    public Integer getMaxTokens() {
        return maxTokens;
    }

    /**
     * 默认请求头。
     *
     * @return 默认请求头
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "AiClientOptions{"
                + "baseUrl='" + baseUrl + '\''
                + ", apiKey=" + (apiKey == null ? "[empty]" : "[configured]")
                + ", model='" + model + '\''
                + ", chatCompletionsPath='" + chatCompletionsPath + '\''
                + ", timeout=" + timeout
                + ", streamReadTimeout=" + streamReadTimeout
                + ", maxRetries=" + maxRetries
                + ", retryInterval=" + retryInterval
                + ", temperature=" + temperature
                + ", maxTokens=" + maxTokens
                + ", headers=" + sanitizedHeaderNames()
                + '}';
    }

    String chatCompletionsUrl() {
        return baseUrl + chatCompletionsPath;
    }

    private String sanitizedHeaderNames() {
        return headers.keySet().toString();
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizePath(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "/chat/completions";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String requireText(String value, String fieldName) {
        String text = trimToNull(value);
        if (text == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return text;
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static Duration requireNonNegative(Duration duration, String fieldName) {
        Duration value = Objects.requireNonNull(duration, fieldName + " must not be null");
        if (value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }

    /**
     * AI 客户端配置构建器。
     */
    public static final class Builder {

        private String baseUrl;
        private String apiKey;
        private String model;
        private String chatCompletionsPath = "/chat/completions";
        private Duration timeout = DEFAULT_TIMEOUT;
        private Duration streamReadTimeout = DEFAULT_STREAM_READ_TIMEOUT;
        private int maxRetries;
        private Duration retryInterval = DEFAULT_RETRY_INTERVAL;
        private Double temperature;
        private Integer maxTokens;
        private final Map<String, String> headers = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * 设置模型服务基础地址。
         *
         * @param baseUrl 模型服务基础地址
         * @return 当前构建器
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * 设置 API key。为空时不会发送 Authorization header。
         *
         * @param apiKey API key
         * @return 当前构建器
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * 设置默认模型。
         *
         * @param model 模型名称
         * @return 当前构建器
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * 设置 Chat Completions 路径。
         *
         * @param chatCompletionsPath Chat Completions 路径
         * @return 当前构建器
         */
        public Builder chatCompletionsPath(String chatCompletionsPath) {
            this.chatCompletionsPath = chatCompletionsPath;
            return this;
        }

        /**
         * 设置 HTTP 超时时间。
         *
         * @param timeout HTTP 超时时间
         * @return 当前构建器
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * 设置流式响应读取超时时间。为 0 时表示不限制 SSE 分片读取等待时间。
         *
         * @param streamReadTimeout 流式读取超时时间
         * @return 当前构建器
         */
        public Builder streamReadTimeout(Duration streamReadTimeout) {
            this.streamReadTimeout = streamReadTimeout;
            return this;
        }

        /**
         * 设置最大重试次数。
         *
         * @param maxRetries 最大重试次数
         * @return 当前构建器
         */
        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries must not be negative");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * 设置重试间隔。
         *
         * @param retryInterval 重试间隔
         * @return 当前构建器
         */
        public Builder retryInterval(Duration retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        /**
         * 设置默认采样温度。
         *
         * @param temperature 默认采样温度
         * @return 当前构建器
         */
        public Builder temperature(Double temperature) {
            if (temperature != null && temperature < 0D) {
                throw new IllegalArgumentException("temperature must not be negative");
            }
            this.temperature = temperature;
            return this;
        }

        /**
         * 设置默认最大输出 token 数。
         *
         * @param maxTokens 默认最大输出 token 数
         * @return 当前构建器
         */
        public Builder maxTokens(Integer maxTokens) {
            if (maxTokens != null && maxTokens <= 0) {
                throw new IllegalArgumentException("maxTokens must be greater than 0");
            }
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * 添加默认请求头。Authorization header 由 apiKey 统一管理，不建议通过这里设置。
         *
         * @param name 请求头名称
         * @param value 请求头值
         * @return 当前构建器
         */
        public Builder header(String name, String value) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("header name must not be blank");
            }
            this.headers.put(name.trim(), value);
            return this;
        }

        /**
         * 批量添加默认请求头。
         *
         * @param headers 默认请求头
         * @return 当前构建器
         */
        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                headers.forEach(this::header);
            }
            return this;
        }

        /**
         * 构建配置。
         *
         * @return AI 客户端配置
         */
        public AiClientOptions build() {
            return new AiClientOptions(this);
        }
    }
}
