package com.undernine.utils.ai;

import java.time.Duration;
import java.util.Objects;

/**
 * AI 模型调用入口。
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.2
 */
public interface AiClient {

    /**
     * 创建 OpenAI-compatible AI client 构建器。
     *
     * @return AI client 构建器
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * 执行文本对话。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 执行单条 user 消息文本对话。
     *
     * @param userMessage user 消息
     * @return 对话响应
     */
    default ChatResponse chat(String userMessage) {
        return chat(ChatRequest.user(userMessage));
    }

    /**
     * AI client 构建器。
     */
    final class Builder {

        private final AiClientOptions.Builder optionsBuilder = AiClientOptions.builder();
        private AiClientProvider provider = new OpenAiCompatibleAiClientProvider();

        private Builder() {
        }

        /**
         * 设置模型服务基础地址。
         *
         * @param baseUrl 模型服务基础地址
         * @return 当前构建器
         */
        public Builder baseUrl(String baseUrl) {
            optionsBuilder.baseUrl(baseUrl);
            return this;
        }

        /**
         * 设置 API key。
         *
         * @param apiKey API key
         * @return 当前构建器
         */
        public Builder apiKey(String apiKey) {
            optionsBuilder.apiKey(apiKey);
            return this;
        }

        /**
         * 设置默认模型。
         *
         * @param model 模型名称
         * @return 当前构建器
         */
        public Builder model(String model) {
            optionsBuilder.model(model);
            return this;
        }

        /**
         * 设置 HTTP 超时时间。
         *
         * @param timeout HTTP 超时时间
         * @return 当前构建器
         */
        public Builder timeout(Duration timeout) {
            optionsBuilder.timeout(timeout);
            return this;
        }

        /**
         * 设置流式响应读取超时时间。为 0 时表示不限制 SSE 分片读取等待时间。
         *
         * @param streamReadTimeout 流式读取超时时间
         * @return 当前构建器
         */
        public Builder streamReadTimeout(Duration streamReadTimeout) {
            optionsBuilder.streamReadTimeout(streamReadTimeout);
            return this;
        }

        /**
         * 设置最大重试次数。
         *
         * @param maxRetries 最大重试次数
         * @return 当前构建器
         */
        public Builder maxRetries(int maxRetries) {
            optionsBuilder.maxRetries(maxRetries);
            return this;
        }

        /**
         * 设置重试间隔。
         *
         * @param retryInterval 重试间隔
         * @return 当前构建器
         */
        public Builder retryInterval(Duration retryInterval) {
            optionsBuilder.retryInterval(retryInterval);
            return this;
        }

        /**
         * 设置默认采样温度。
         *
         * @param temperature 默认采样温度
         * @return 当前构建器
         */
        public Builder temperature(Double temperature) {
            optionsBuilder.temperature(temperature);
            return this;
        }

        /**
         * 设置默认最大输出 token 数。
         *
         * @param maxTokens 默认最大输出 token 数
         * @return 当前构建器
         */
        public Builder maxTokens(Integer maxTokens) {
            optionsBuilder.maxTokens(maxTokens);
            return this;
        }

        /**
         * 添加默认请求头。
         *
         * @param name 请求头名称
         * @param value 请求头值
         * @return 当前构建器
         */
        public Builder header(String name, String value) {
            optionsBuilder.header(name, value);
            return this;
        }

        /**
         * 设置 AI client provider。
         *
         * @param provider AI client provider
         * @return 当前构建器
         */
        public Builder provider(AiClientProvider provider) {
            this.provider = Objects.requireNonNull(provider, "provider must not be null");
            return this;
        }

        /**
         * 构建 AI client。
         *
         * @return AI client
         */
        public AiClient build() {
            return provider.create(optionsBuilder.build());
        }
    }
}
