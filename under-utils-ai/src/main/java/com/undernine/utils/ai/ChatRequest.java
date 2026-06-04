package com.undernine.utils.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 文本对话请求。
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.2
 */
public final class ChatRequest {

    private final List<ChatMessage> messages;
    private final List<Map<String, Object>> nativeMessages;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private final String requestId;
    private final Map<String, Object> extraBody;

    private ChatRequest(Builder builder) {
        if (builder.messages.isEmpty() && builder.nativeMessages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        if (!builder.messages.isEmpty() && !builder.nativeMessages.isEmpty()) {
            throw new IllegalArgumentException("typed messages and native messages must not be mixed");
        }
        this.messages = Collections.unmodifiableList(new ArrayList<>(builder.messages));
        this.nativeMessages = copyNativeMessages(builder.nativeMessages);
        this.model = trimToNull(builder.model);
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.requestId = trimToNull(builder.requestId);
        this.extraBody = Collections.unmodifiableMap(new LinkedHashMap<>(builder.extraBody));
    }

    /**
     * 创建只有一条 user 消息的请求。
     *
     * @param content 用户消息
     * @return 对话请求
     */
    public static ChatRequest user(String content) {
        return builder().user(content).build();
    }

    /**
     * 创建请求构建器。
     *
     * @return 请求构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 基于当前请求创建构建器。
     *
     * @return 请求构建器
     */
    public Builder toBuilder() {
        return builder()
                .messages(messages)
                .nativeMessages(nativeMessages)
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .requestId(requestId)
                .extraBody(extraBody);
    }

    /**
     * 对话消息。
     *
     * @return 对话消息
     */
    public List<ChatMessage> getMessages() {
        return messages;
    }

    /**
     * OpenAI-compatible 原生消息结构。用于多模态 content、tool 调用结果等文本 helper 无法表达的场景。
     *
     * @return 原生消息结构
     */
    public List<Map<String, Object>> getNativeMessages() {
        return nativeMessages;
    }

    /**
     * 本次请求覆盖的模型名称。
     *
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 采样温度。
     *
     * @return 采样温度
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     * 最大输出 token 数。
     *
     * @return 最大输出 token 数
     */
    public Integer getMaxTokens() {
        return maxTokens;
    }

    /**
     * 请求 ID。
     *
     * @return 请求 ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 透传给 OpenAI-compatible API 的额外请求字段。
     *
     * @return 额外请求字段
     */
    public Map<String, Object> getExtraBody() {
        return extraBody;
    }

    @Override
    public String toString() {
        return "ChatRequest{"
                + "messageCount=" + (messages.isEmpty() ? nativeMessages.size() : messages.size())
                + ", model='" + model + '\''
                + ", temperature=" + temperature
                + ", maxTokens=" + maxTokens
                + ", requestId='" + requestId + '\''
                + ", extraBodyKeys=" + extraBody.keySet()
                + '}';
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static List<Map<String, Object>> copyNativeMessages(List<Map<String, Object>> messages) {
        List<Map<String, Object>> copiedMessages = new ArrayList<>(messages.size());
        for (Map<String, Object> message : messages) {
            copiedMessages.add(copyObjectMap(message, "native message"));
        }
        return Collections.unmodifiableList(copiedMessages);
    }

    private static List<Map<String, Object>> copyObjectMaps(List<Map<String, Object>> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        List<Map<String, Object>> copiedValues = new ArrayList<>(values.size());
        for (Map<String, Object> value : values) {
            copiedValues.add(copyObjectMap(value, name + " item"));
        }
        return copiedValues;
    }

    private static Map<String, Object> copyObjectMap(Map<String, Object> value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }

    /**
     * 对话请求构建器。
     */
    public static final class Builder {

        private final List<ChatMessage> messages = new ArrayList<>();
        private final List<Map<String, Object>> nativeMessages = new ArrayList<>();
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private String requestId;
        private final Map<String, Object> extraBody = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * 添加消息。
         *
         * @param message 消息
         * @return 当前构建器
         */
        public Builder message(ChatMessage message) {
            this.messages.add(Objects.requireNonNull(message, "message must not be null"));
            return this;
        }

        /**
         * 批量添加消息。
         *
         * @param messages 消息列表
         * @return 当前构建器
         */
        public Builder messages(List<ChatMessage> messages) {
            if (messages != null) {
                messages.forEach(this::message);
            }
            return this;
        }

        /**
         * 添加 OpenAI-compatible 原生消息结构。用于 content 数组、多模态消息、tool 调用结果等。
         *
         * @param message 原生消息结构
         * @return 当前构建器
         */
        public Builder nativeMessage(Map<String, Object> message) {
            this.nativeMessages.add(copyObjectMap(message, "native message"));
            return this;
        }

        /**
         * 批量添加 OpenAI-compatible 原生消息结构。
         *
         * @param messages 原生消息列表
         * @return 当前构建器
         */
        public Builder nativeMessages(List<Map<String, Object>> messages) {
            if (messages != null) {
                this.nativeMessages.addAll(copyObjectMaps(messages, "nativeMessages"));
            }
            return this;
        }

        /**
         * 添加 system 消息。
         *
         * @param content 消息内容
         * @return 当前构建器
         */
        public Builder system(String content) {
            return message(ChatMessage.system(content));
        }

        /**
         * 添加 user 消息。
         *
         * @param content 消息内容
         * @return 当前构建器
         */
        public Builder user(String content) {
            return message(ChatMessage.user(content));
        }

        /**
         * 添加 assistant 消息。
         *
         * @param content 消息内容
         * @return 当前构建器
         */
        public Builder assistant(String content) {
            return message(ChatMessage.assistant(content));
        }

        /**
         * 覆盖默认模型。
         *
         * @param model 模型名称
         * @return 当前构建器
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * 设置采样温度。
         *
         * @param temperature 采样温度
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
         * 设置最大输出 token 数。
         *
         * @param maxTokens 最大输出 token 数
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
         * 设置请求 ID。
         *
         * @param requestId 请求 ID
         * @return 当前构建器
         */
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * 添加额外请求字段。
         *
         * @param name 字段名
         * @param value 字段值
         * @return 当前构建器
         */
        public Builder extraBody(String name, Object value) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("extra body name must not be blank");
            }
            this.extraBody.put(name.trim(), value);
            return this;
        }

        /**
         * 批量添加额外请求字段。
         *
         * @param extraBody 额外请求字段
         * @return 当前构建器
         */
        public Builder extraBody(Map<String, Object> extraBody) {
            if (extraBody != null) {
                extraBody.forEach(this::extraBody);
            }
            return this;
        }

        /**
         * 设置 OpenAI-compatible tools 参数。
         *
         * @param tools tools 列表
         * @return 当前构建器
         */
        public Builder tools(List<Map<String, Object>> tools) {
            return extraBody("tools", Collections.unmodifiableList(copyObjectMaps(tools, "tools")));
        }

        /**
         * 添加一个 OpenAI-compatible tool 参数。
         *
         * @param tool tool 定义
         * @return 当前构建器
         */
        public Builder tool(Map<String, Object> tool) {
            List<Map<String, Object>> tools = new ArrayList<>();
            Object existingTools = this.extraBody.get("tools");
            if (existingTools instanceof List<?> existingList) {
                for (Object existingTool : existingList) {
                    if (!(existingTool instanceof Map<?, ?> existingToolMap)) {
                        throw new IllegalArgumentException("tools must contain map items");
                    }
                    tools.add(copyUnknownObjectMap(existingToolMap, "tools item"));
                }
            }
            tools.add(copyObjectMap(tool, "tool"));
            this.extraBody.put("tools", Collections.unmodifiableList(tools));
            return this;
        }

        /**
         * 设置 OpenAI-compatible tool_choice 参数。
         *
         * @param toolChoice tool_choice 值
         * @return 当前构建器
         */
        public Builder toolChoice(Object toolChoice) {
            return extraBody("tool_choice", toolChoice);
        }

        /**
         * 设置 OpenAI-compatible response_format 参数。
         *
         * @param responseFormat response_format 值
         * @return 当前构建器
         */
        public Builder responseFormat(Object responseFormat) {
            return extraBody("response_format", responseFormat);
        }

        /**
         * 构建请求。
         *
         * @return 对话请求
         */
        public ChatRequest build() {
            return new ChatRequest(this);
        }

        private Map<String, Object> copyUnknownObjectMap(Map<?, ?> value, String name) {
            Map<String, Object> copied = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : value.entrySet()) {
                if (!(entry.getKey() instanceof String key) || key.trim().isEmpty()) {
                    throw new IllegalArgumentException(name + " key must be a non-blank string");
                }
                copied.put(key, entry.getValue());
            }
            return Collections.unmodifiableMap(copied);
        }
    }
}
