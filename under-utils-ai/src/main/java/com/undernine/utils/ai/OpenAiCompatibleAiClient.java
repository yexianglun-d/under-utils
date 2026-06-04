package com.undernine.utils.ai;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.undernine.utils.core.json.JsonException;
import com.undernine.utils.http.config.HttpConfig;
import com.undernine.utils.http.exception.HttpException;
import com.undernine.utils.http.exception.HttpNetworkException;
import com.undernine.utils.http.exception.HttpTimeoutException;
import com.undernine.utils.http.request.HttpRequest;
import com.undernine.utils.http.response.HttpResponse;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI-compatible Chat Completions 客户端。
 * <p>
 * 当前实现覆盖同步文本对话和 SSE 流式文本对话，并支持 OpenAI-compatible
 * 原生消息、tools、response_format 等请求参数透传；厂商私有协议仍应通过自定义 provider 扩展。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.2
 */
public class OpenAiCompatibleAiClient implements AiClient, StreamingAiClient {

    private static final ObjectMapper JSON_MAPPER = createJsonMapper();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final AiClientOptions options;
    private final HttpConfig httpConfig;
    private final OkHttpClient streamingHttpClient;

    /**
     * 创建 OpenAI-compatible AI client。
     *
     * @param options 客户端配置
     */
    public OpenAiCompatibleAiClient(AiClientOptions options) {
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.httpConfig = buildHttpConfig(options);
        this.streamingHttpClient = buildStreamingHttpClient(options);
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        ChatRequest chatRequest = Objects.requireNonNull(request, "request must not be null");
        long startedAt = System.nanoTime();
        HttpResponse response = execute(chatRequest);
        if (response.isFail()) {
            throw toStatusException(response);
        }
        return parseSuccess(response, chatRequest.getRequestId(), durationSince(startedAt));
    }

    @Override
    public ChatStream streamChat(ChatRequest request) {
        ChatRequest chatRequest = Objects.requireNonNull(request, "request must not be null");
        long startedAt = System.nanoTime();
        Request httpRequest = buildStreamingRequest(chatRequest);
        Call call = streamingHttpClient.newCall(httpRequest);
        Response response;
        try {
            response = call.execute();
        } catch (SocketTimeoutException e) {
            throw new AiException(AiErrorType.TIMEOUT, "AI stream request timed out", 0, null, true, e);
        } catch (IOException e) {
            throw new AiException(AiErrorType.NETWORK, "AI stream request failed because of network error",
                    0, null, true, e);
        }
        if (!response.isSuccessful()) {
            String body = readResponseBody(response);
            response.close();
            throw toStatusException(response.code(), body);
        }
        ResponseBody body = response.body();
        if (body == null) {
            response.close();
            throw new AiException(AiErrorType.RESPONSE_PARSE, "AI stream response body is missing",
                    response.code(), null, false);
        }
        OpenAiSseIterator iterator = new OpenAiSseIterator(
                new BufferedReader(new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8)),
                response,
                call,
                chatRequest.getRequestId(),
                startedAt
        );
        return new ChatStream(iterator, iterator);
    }

    /**
     * 客户端配置。
     *
     * @return 客户端配置
     */
    public AiClientOptions getOptions() {
        return options;
    }

    private HttpResponse execute(ChatRequest request) {
        try {
            HttpRequest.Builder builder = HttpRequest.post(options.chatCompletionsUrl())
                    .config(httpConfig)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(toRequestBody(request));
            if (options.getApiKey() != null) {
                builder.header("Authorization", "Bearer " + options.getApiKey());
            }
            if (request.getRequestId() != null) {
                builder.header("X-Request-Id", request.getRequestId());
            }
            options.getHeaders().forEach(builder::header);
            return builder.execute();
        } catch (HttpTimeoutException e) {
            throw new AiException(AiErrorType.TIMEOUT, "AI request timed out", 0, null, true, e);
        } catch (HttpNetworkException e) {
            throw new AiException(AiErrorType.NETWORK, "AI request failed because of network error", 0, null, true, e);
        } catch (HttpException e) {
            throw new AiException(AiErrorType.UNKNOWN, "AI request failed", 0, null, false, e);
        }
    }

    private Map<String, Object> toRequestBody(ChatRequest request) {
        Map<String, Object> body = new LinkedHashMap<>(request.getExtraBody());
        body.put("model", request.getModel() == null ? options.getModel() : request.getModel());
        body.put("messages", toMessages(request));
        Double temperature = request.getTemperature() == null ? options.getTemperature() : request.getTemperature();
        Integer maxTokens = request.getMaxTokens() == null ? options.getMaxTokens() : request.getMaxTokens();
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }
        return body;
    }

    private Map<String, Object> toStreamingRequestBody(ChatRequest request) {
        Map<String, Object> body = new LinkedHashMap<>(toRequestBody(request));
        body.put("stream", true);
        return body;
    }

    private List<Map<String, Object>> toMessages(ChatRequest request) {
        if (!request.getNativeMessages().isEmpty()) {
            return request.getNativeMessages();
        }
        List<ChatMessage> messages = request.getMessages();
        List<Map<String, Object>> result = new ArrayList<>(messages.size());
        for (ChatMessage message : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", message.getRole().wireName());
            item.put("content", message.getContent());
            result.add(item);
        }
        return result;
    }

    private ChatResponse parseSuccess(HttpResponse response, String requestId, Duration duration) {
        ChatCompletionResponse result;
        try {
            result = response.asObject(ChatCompletionResponse.class);
        } catch (JsonException e) {
            throw new AiException(AiErrorType.RESPONSE_PARSE, "Failed to parse AI response", response.getStatusCode(),
                    null, false, e);
        }
        if (result == null || result.choices == null || result.choices.isEmpty()
                || result.choices.get(0).message == null) {
            throw new AiException(AiErrorType.RESPONSE_PARSE, "AI response does not contain assistant message",
                    response.getStatusCode(), null, false);
        }
        ChatChoice choice = result.choices.get(0);
        String text = choice.message.content;
        if (text == null && choice.message.hasToolCallPayload()) {
            text = "";
        }
        if (text == null) {
            throw new AiException(AiErrorType.RESPONSE_PARSE, "AI response assistant message content is missing",
                    response.getStatusCode(), null, false);
        }
        TokenUsage usage = null;
        if (result.usage != null) {
            usage = new TokenUsage(result.usage.promptTokens, result.usage.completionTokens, result.usage.totalTokens);
        }
        String responseId = result.id == null ? requestId : result.id;
        AiResponseMetadata metadata = AiResponseMetadata.builder()
                .provider(AiProviderNames.OPENAI_COMPATIBLE)
                .requestId(requestId)
                .responseId(result.id)
                .modelFingerprint(result.systemFingerprint)
                .duration(duration)
                .build();
        return new ChatResponse(text, result.model, choice.finishReason, responseId, usage, metadata,
                choice.message.rawMessage());
    }

    private AiException toStatusException(HttpResponse response) {
        AiStatusError error = parseStatusError(response);
        AiErrorType type = classify(response.getStatusCode());
        boolean retryable = response.getStatusCode() == 429 || response.getStatusCode() >= 500;
        String code = error == null ? null : error.code;
        String message = "AI request failed with HTTP status " + response.getStatusCode();
        if (code != null && !code.trim().isEmpty()) {
            message += ", errorCode=" + code;
        }
        return new AiException(type, message, response.getStatusCode(), code, retryable);
    }

    private AiStatusError parseStatusError(HttpResponse response) {
        return parseStatusError(response.asString());
    }

    private AiStatusError parseStatusError(String bodyString) {
        try {
            if (bodyString == null || bodyString.trim().isEmpty()) {
                return null;
            }
            ErrorResponse body = JSON_MAPPER.readValue(bodyString, ErrorResponse.class);
            return body == null ? null : body.error;
        } catch (RuntimeException ignored) {
            return null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private AiErrorType classify(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return AiErrorType.AUTHENTICATION;
        }
        if (statusCode == 429) {
            return AiErrorType.RATE_LIMIT;
        }
        if (statusCode >= 500) {
            return AiErrorType.SERVER_ERROR;
        }
        if (statusCode >= 400) {
            return AiErrorType.CLIENT_ERROR;
        }
        return AiErrorType.UNKNOWN;
    }

    private HttpConfig buildHttpConfig(AiClientOptions options) {
        Duration timeout = options.getTimeout();
        return HttpConfig.builder()
                .connectTimeoutDuration(timeout)
                .readTimeoutDuration(timeout)
                .writeTimeoutDuration(timeout)
                .maxRetries(options.getMaxRetries())
                .retryIntervalDuration(options.getRetryInterval())
                .build();
    }

    private OkHttpClient buildStreamingHttpClient(AiClientOptions options) {
        Duration timeout = options.getTimeout();
        Duration streamReadTimeout = options.getStreamReadTimeout();
        return new OkHttpClient.Builder()
                .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(streamReadTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(options.getMaxRetries() > 0)
                .connectionPool(new ConnectionPool())
                .build();
    }

    private Request buildStreamingRequest(ChatRequest request) {
        Request.Builder builder = new Request.Builder()
                .url(options.chatCompletionsUrl())
                .post(RequestBody.create(toJson(toStreamingRequestBody(request)), JSON_MEDIA_TYPE))
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json");
        if (options.getApiKey() != null) {
            builder.header("Authorization", "Bearer " + options.getApiKey());
        }
        if (request.getRequestId() != null) {
            builder.header("X-Request-Id", request.getRequestId());
        }
        options.getHeaders().forEach(builder::header);
        return builder.build();
    }

    private AiException toStatusException(int statusCode, String bodyString) {
        AiStatusError error = parseStatusError(bodyString);
        AiErrorType type = classify(statusCode);
        boolean retryable = statusCode == 429 || statusCode >= 500;
        String code = error == null ? null : error.code;
        String message = "AI request failed with HTTP status " + statusCode;
        if (code != null && !code.trim().isEmpty()) {
            message += ", errorCode=" + code;
        }
        return new AiException(type, message, statusCode, code, retryable);
    }

    private String readResponseBody(Response response) {
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return null;
        }
        try {
            return responseBody.string();
        } catch (IOException e) {
            return null;
        }
    }

    private ChatStreamEvent parseStreamEvent(String data, String fallbackRequestId, Duration duration) {
        ChatCompletionChunk chunk;
        try {
            chunk = JSON_MAPPER.readValue(data, ChatCompletionChunk.class);
        } catch (IOException e) {
            throw new AiException(AiErrorType.RESPONSE_PARSE, "Failed to parse AI stream response",
                    200, null, false, e);
        }
        if (chunk == null) {
            throw new AiException(AiErrorType.RESPONSE_PARSE, "AI stream response chunk is empty",
                    200, null, false);
        }
        TokenUsage usage = null;
        if (chunk.usage != null) {
            usage = new TokenUsage(chunk.usage.promptTokens, chunk.usage.completionTokens, chunk.usage.totalTokens);
        }
        ChatChunkChoice choice = chunk.choices == null || chunk.choices.isEmpty() ? null : chunk.choices.get(0);
        ChatCompletionDelta delta = choice == null ? null : choice.delta;
        String text = delta == null ? null : delta.content;
        String role = delta == null ? null : delta.role;
        String finishReason = choice == null ? null : choice.finishReason;
        AiResponseMetadata metadata = AiResponseMetadata.builder()
                .provider(AiProviderNames.OPENAI_COMPATIBLE)
                .requestId(fallbackRequestId)
                .responseId(chunk.id)
                .modelFingerprint(chunk.systemFingerprint)
                .duration(duration)
                .build();
        return new ChatStreamEvent(text, role, chunk.model, finishReason, usage, metadata, false);
    }

    private ChatStreamEvent doneEvent(String requestId, Duration duration) {
        AiResponseMetadata metadata = AiResponseMetadata.builder()
                .provider(AiProviderNames.OPENAI_COMPATIBLE)
                .requestId(requestId)
                .duration(duration)
                .build();
        return new ChatStreamEvent(null, null, null, null, null, metadata, true);
    }

    private Duration durationSince(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }

    private String toJson(Object body) {
        try {
            return JSON_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new AiException(AiErrorType.UNKNOWN, "Failed to serialize AI request body", 0, null, false, e);
        }
    }

    private static ObjectMapper createJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    private final class OpenAiSseIterator implements Iterator<ChatStreamEvent>, AutoCloseable {

        private final BufferedReader reader;
        private final Response response;
        private final Call call;
        private final String requestId;
        private final long startedAt;
        private ChatStreamEvent next;
        private boolean closed;

        private OpenAiSseIterator(BufferedReader reader, Response response, Call call,
                                  String requestId, long startedAt) {
            this.reader = reader;
            this.response = response;
            this.call = call;
            this.requestId = requestId;
            this.startedAt = startedAt;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            next = readNext();
            return next != null;
        }

        @Override
        public ChatStreamEvent next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            ChatStreamEvent current = next;
            next = null;
            return current;
        }

        private ChatStreamEvent readNext() {
            if (closed) {
                return null;
            }
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith(":")) {
                        continue;
                    }
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring("data:".length()).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        ChatStreamEvent done = doneEvent(requestId, durationSince(startedAt));
                        close();
                        return done;
                    }
                    return parseStreamEvent(data, requestId, durationSince(startedAt));
                }
                close();
                return null;
            } catch (SocketTimeoutException e) {
                close();
                throw new AiException(AiErrorType.TIMEOUT, "AI stream request timed out", 0, null, true, e);
            } catch (IOException e) {
                close();
                throw new AiException(AiErrorType.NETWORK, "AI stream request failed because of network error",
                        0, null, true, e);
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                reader.close();
            } catch (IOException ignored) {
            }
            response.close();
            call.cancel();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class ErrorResponse {
        private AiStatusError error;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class AiStatusError {
        private String message;
        private String type;
        private String code;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class ChatCompletionResponse {
        private String id;
        private String model;
        @JsonProperty("system_fingerprint")
        private String systemFingerprint;
        private List<ChatChoice> choices;
        private Usage usage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class ChatChoice {
        private ChatCompletionMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class ChatCompletionMessage {
        private String role;
        private String content;
        private final Map<String, Object> extraFields = new LinkedHashMap<>();

        @JsonAnySetter
        private void setExtraField(String name, Object value) {
            extraFields.put(name, value);
        }

        private boolean hasToolCallPayload() {
            return extraFields.containsKey("tool_calls") || extraFields.containsKey("function_call");
        }

        private Map<String, Object> rawMessage() {
            Map<String, Object> rawMessage = new LinkedHashMap<>(extraFields);
            if (role != null) {
                rawMessage.put("role", role);
            }
            if (content != null) {
                rawMessage.put("content", content);
            }
            return rawMessage;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class ChatCompletionChunk {
        private String id;
        private String model;
        @JsonProperty("system_fingerprint")
        private String systemFingerprint;
        private List<ChatChunkChoice> choices;
        private Usage usage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class ChatChunkChoice {
        private ChatCompletionDelta delta;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class ChatCompletionDelta {
        private String role;
        private String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
