package com.undernine.utils.ai;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleAiClientTest {

    private static final String API_KEY = "sk-test-secret";

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldCallOpenAiCompatibleChatCompletions() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": "chatcmpl-001",
                          "model": "demo-model",
                          "system_fingerprint": "fp-demo",
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "你好，Under-Utils"
                              },
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": {
                            "prompt_tokens": 7,
                            "completion_tokens": 5,
                            "total_tokens": 12
                          }
                        }
                        """));
        AiClient client = client();

        ChatResponse response = client.chat(ChatRequest.builder()
                .system("你是一个简洁的助手")
                .user("打个招呼")
                .temperature(0.2D)
                .maxTokens(64)
                .requestId("req-001")
                .build());

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/chat/completions");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer " + API_KEY);
        assertThat(request.getHeader("X-Request-Id")).isEqualTo("req-001");
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"model\":\"demo-model\"");
        assertThat(requestBody).contains("\"role\":\"system\"");
        assertThat(requestBody).contains("\"role\":\"user\"");
        assertThat(requestBody).contains("\"temperature\":0.2");
        assertThat(requestBody).contains("\"max_tokens\":64");
        assertThat(response.text()).isEqualTo("你好，Under-Utils");
        assertThat(response.getModel()).isEqualTo("demo-model");
        assertThat(response.getRequestId()).isEqualTo("chatcmpl-001");
        assertThat(response.getResponseId()).isEqualTo("chatcmpl-001");
        assertThat(response.getModelFingerprint()).isEqualTo("fp-demo");
        assertThat(response.getDuration()).isNotNull();
        assertThat(response.getMetadata().getProvider()).isEqualTo(AiProviderNames.OPENAI_COMPATIBLE);
        assertThat(response.getMetadata().getRequestId()).isEqualTo("req-001");
        assertThat(response.getFinishReason()).isEqualTo("stop");
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(12);
    }

    @Test
    void shouldStreamOpenAiCompatibleChatCompletions() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        data: {"id":"chatcmpl-stream","model":"demo-model","system_fingerprint":"fp-stream","choices":[{"delta":{"role":"assistant","content":"你"},"finish_reason":null}]}

                        data: {"id":"chatcmpl-stream","model":"demo-model","choices":[{"delta":{"content":"好"},"finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}

                        data: [DONE]

                        """));
        StreamingAiClient client = (StreamingAiClient) client();

        List<ChatStreamEvent> events;
        try (ChatStream stream = client.streamChat(ChatRequest.builder()
                .user("打个招呼")
                .requestId("req-stream")
                .build())) {
            events = stream.stream().toList();
        }

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Accept")).isEqualTo("text/event-stream");
        assertThat(request.getHeader("X-Request-Id")).isEqualTo("req-stream");
        assertThat(request.getBody().readUtf8()).contains("\"stream\":true");
        assertThat(events).hasSize(3);
        assertThat(events.get(0).text()).isEqualTo("你");
        assertThat(events.get(0).getRole()).isEqualTo("assistant");
        assertThat(events.get(0).getResponseId()).isEqualTo("chatcmpl-stream");
        assertThat(events.get(0).getModelFingerprint()).isEqualTo("fp-stream");
        assertThat(events.get(0).getMetadata().getRequestId()).isEqualTo("req-stream");
        assertThat(events.get(1).text()).isEqualTo("好");
        assertThat(events.get(1).getFinishReason()).isEqualTo("stop");
        assertThat(events.get(1).getUsage().getTotalTokens()).isEqualTo(5);
        assertThat(events.get(2).isDone()).isTrue();
    }

    @Test
    void shouldMapStreamStatusFailure() {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "error": {
                            "message": "too many requests",
                            "code": "rate_limit"
                          }
                        }
                        """));
        StreamingAiClient client = (StreamingAiClient) client();

        assertThatThrownBy(() -> client.streamChat(ChatRequest.user("hello")))
                .isInstanceOfSatisfying(AiException.class, ex -> {
                    assertThat(ex.getErrorType()).isEqualTo(AiErrorType.RATE_LIMIT);
                    assertThat(ex.getStatusCode()).isEqualTo(429);
                    assertThat(ex.isRetryable()).isTrue();
                    assertThat(ex.getMessage()).doesNotContain("hello");
                });
    }

    @Test
    void shouldMapStreamConnectionInterrupted() {
        server.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        StreamingAiClient client = (StreamingAiClient) client();

        assertThatThrownBy(() -> client.streamChat(ChatRequest.user("hello")))
                .isInstanceOfSatisfying(AiException.class, ex ->
                        assertThat(ex.getErrorType()).isEqualTo(AiErrorType.NETWORK));
    }

    @Test
    void shouldAllowStreamCancellationByClose() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        data: {"id":"chatcmpl-cancel","model":"demo-model","choices":[{"delta":{"content":"first"},"finish_reason":null}]}

                        """)
                .setSocketPolicy(SocketPolicy.KEEP_OPEN));
        StreamingAiClient client = (StreamingAiClient) client();

        try (ChatStream stream = client.streamChat(ChatRequest.user("hello"))) {
            Iterator<ChatStreamEvent> iterator = stream.iterator();
            assertThat(iterator.hasNext()).isTrue();
            assertThat(iterator.next().text()).isEqualTo("first");
            stream.close();
            assertThat(iterator.hasNext()).isFalse();
        }
    }

    @Test
    void shouldSupportSimpleUserMessageShortcut() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "id": "chatcmpl-002",
                          "model": "demo-model",
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "done"
                              },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                        """));

        ChatResponse response = client().chat("ping");

        assertThat(response.text()).isEqualTo("done");
    }

    @Test
    void shouldAllowRequestModelOverrideAndExtraBody() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "id": "chatcmpl-003",
                          "model": "other-model",
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "ok"
                              }
                            }
                          ]
                        }
                        """));

        client().chat(ChatRequest.builder()
                .user("ping")
                .model("other-model")
                .extraBody("top_p", 0.9D)
                .build());

        RecordedRequest request = server.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"model\":\"other-model\"");
        assertThat(requestBody).contains("\"top_p\":0.9");
    }

    @Test
    void shouldSendNativeMessagesAndToolOptions() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "id": "chatcmpl-native",
                          "model": "vision-model",
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "native-ok"
                              },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                        """));

        client().chat(ChatRequest.builder()
                .nativeMessage(Map.of(
                        "role", "user",
                        "content", List.of(Map.of("type", "text", "text", "describe this image"))
                ))
                .tool(Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "lookup_order",
                                "parameters", Map.of("type", "object")
                        )
                ))
                .toolChoice(Map.of(
                        "type", "function",
                        "function", Map.of("name", "lookup_order")
                ))
                .responseFormat(Map.of("type", "json_object"))
                .build());

        RecordedRequest request = server.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"content\":[{");
        assertThat(requestBody).contains("\"type\":\"text\"");
        assertThat(requestBody).contains("\"text\":\"describe this image\"");
        assertThat(requestBody).contains("\"tools\":[{");
        assertThat(requestBody).contains("\"tool_choice\":{");
        assertThat(requestBody).contains("\"name\":\"lookup_order\"");
        assertThat(requestBody).contains("\"response_format\":{\"type\":\"json_object\"}");
    }

    @Test
    void shouldExposeRawAssistantMessageForToolCalls() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "id": "chatcmpl-tool",
                          "model": "demo-model",
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": null,
                                "tool_calls": [
                                  {
                                    "id": "call_001",
                                    "type": "function",
                                    "function": {
                                      "name": "lookup_order",
                                      "arguments": "{\\"orderNo\\":\\"A001\\"}"
                                    }
                                  }
                                ]
                              },
                              "finish_reason": "tool_calls"
                            }
                          ]
                        }
                        """));

        ChatResponse response = client().chat(ChatRequest.builder()
                .user("查订单")
                .tool(Map.of(
                        "type", "function",
                        "function", Map.of("name", "lookup_order", "parameters", Map.of("type", "object"))
                ))
                .build());

        assertThat(response.text()).isEmpty();
        assertThat(response.getFinishReason()).isEqualTo("tool_calls");
        assertThat(response.getRawMessage()).containsKeys("role", "tool_calls");
        assertThat(response.getRawMessage().get("role")).isEqualTo("assistant");
    }

    @Test
    void shouldMapAuthenticationFailureWithoutLeakingApiKey() {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "error": {
                            "message": "invalid api key",
                            "type": "authentication_error",
                            "code": "invalid_api_key"
                          }
                        }
                        """));

        assertThatThrownBy(() -> client().chat(ChatRequest.user("hello")))
                .isInstanceOfSatisfying(AiException.class, ex -> {
                    assertThat(ex.getErrorType()).isEqualTo(AiErrorType.AUTHENTICATION);
                    assertThat(ex.getStatusCode()).isEqualTo(401);
                    assertThat(ex.getErrorCode()).isEqualTo("invalid_api_key");
                    assertThat(ex.isRetryable()).isFalse();
                    assertThat(ex.getMessage()).doesNotContain(API_KEY);
                    assertThat(ex.getMessage()).doesNotContain("Bearer");
                    assertThat(ex.getMessage()).doesNotContain("hello");
                });
    }

    @Test
    void shouldMapRateLimitAsRetryable() {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setBody("""
                        {
                          "error": {
                            "message": "too many requests",
                            "code": "rate_limit"
                          }
                        }
                        """));

        assertThatThrownBy(() -> client().chat(ChatRequest.user("hello")))
                .isInstanceOfSatisfying(AiException.class, ex -> {
                    assertThat(ex.getErrorType()).isEqualTo(AiErrorType.RATE_LIMIT);
                    assertThat(ex.getStatusCode()).isEqualTo(429);
                    assertThat(ex.isRetryable()).isTrue();
                });
    }

    @Test
    void shouldMapServerErrorAsRetryable() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{}"));

        assertThatThrownBy(() -> client().chat(ChatRequest.user("hello")))
                .isInstanceOfSatisfying(AiException.class, ex -> {
                    assertThat(ex.getErrorType()).isEqualTo(AiErrorType.SERVER_ERROR);
                    assertThat(ex.getStatusCode()).isEqualTo(500);
                    assertThat(ex.isRetryable()).isTrue();
                });
    }

    @Test
    void shouldMapInvalidSuccessBodyToParseFailure() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"choices\":[]}"));

        assertThatThrownBy(() -> client().chat(ChatRequest.user("hello")))
                .isInstanceOfSatisfying(AiException.class, ex -> {
                    assertThat(ex.getErrorType()).isEqualTo(AiErrorType.RESPONSE_PARSE);
                    assertThat(ex.getStatusCode()).isEqualTo(200);
                    assertThat(ex.isRetryable()).isFalse();
                    assertThat(ex.getMessage()).doesNotContain("hello");
                });
    }

    @Test
    void shouldMapTimeoutFailure() {
        server.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE));
        AiClient client = AiClient.builder()
                .baseUrl(server.url("/v1").toString())
                .apiKey(API_KEY)
                .model("demo-model")
                .timeout(Duration.ofMillis(50))
                .build();

        assertThatThrownBy(() -> client.chat(ChatRequest.user("hello")))
                .isInstanceOfSatisfying(AiException.class, ex -> {
                    assertThat(ex.getErrorType()).isEqualTo(AiErrorType.TIMEOUT);
                    assertThat(ex.getStatusCode()).isZero();
                    assertThat(ex.isRetryable()).isTrue();
                });
    }

    @Test
    void shouldHideSensitiveValuesInToString() {
        AiClientOptions options = AiClientOptions.builder()
                .baseUrl("https://api.example.com/v1")
                .apiKey(API_KEY)
                .model("demo-model")
                .header("Authorization", "Bearer other-secret")
                .build();
        ChatRequest request = ChatRequest.user("secret prompt");
        ChatResponse response = new ChatResponse("secret output", "demo-model", "stop", "req-001", null);

        assertThat(options.toString()).doesNotContain(API_KEY);
        assertThat(options.toString()).doesNotContain("other-secret");
        assertThat(request.toString()).doesNotContain("secret prompt");
        assertThat(response.toString()).doesNotContain("secret output");
    }

    @Test
    void shouldBuildClientWithCustomProvider() {
        AiClientProvider provider = new AiClientProvider() {
            @Override
            public String provider() {
                return "custom";
            }

            @Override
            public AiClient create(AiClientOptions options) {
                return request -> new ChatResponse("provider-ok", options.getModel(), "stop", "custom-001", null);
            }
        };

        AiClient client = AiClient.builder()
                .baseUrl("https://api.example.com/v1")
                .model("custom-model")
                .provider(provider)
                .build();

        assertThat(client.chat("hello").text()).isEqualTo("provider-ok");
    }

    @Test
    void shouldRejectEmptyMessages() {
        assertThatThrownBy(() -> ChatRequest.builder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages must not be empty");
    }

    @Test
    void shouldRejectMixedTypedAndNativeMessages() {
        assertThatThrownBy(() -> ChatRequest.builder()
                .user("hello")
                .nativeMessage(Map.of("role", "user", "content", "native"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typed messages and native messages must not be mixed");
    }

    @Test
    void shouldDisableStreamingReadTimeoutByDefault() throws Exception {
        OpenAiCompatibleAiClient client = (OpenAiCompatibleAiClient) client();

        assertThat(streamingHttpClient(client).readTimeoutMillis()).isZero();
    }

    @Test
    void shouldApplyCustomStreamingReadTimeout() throws Exception {
        OpenAiCompatibleAiClient client = new OpenAiCompatibleAiClient(AiClientOptions.builder()
                .baseUrl(server.url("/v1").toString())
                .apiKey(API_KEY)
                .model("demo-model")
                .timeout(Duration.ofSeconds(2))
                .streamReadTimeout(Duration.ofSeconds(10))
                .build());

        assertThat(streamingHttpClient(client).readTimeoutMillis()).isEqualTo(10_000);
    }

    private AiClient client() {
        return AiClient.builder()
                .baseUrl(server.url("/v1").toString())
                .apiKey(API_KEY)
                .model("demo-model")
                .timeout(Duration.ofSeconds(2))
                .retryInterval(Duration.ZERO)
                .build();
    }

    private OkHttpClient streamingHttpClient(OpenAiCompatibleAiClient client) throws Exception {
        Field field = OpenAiCompatibleAiClient.class.getDeclaredField("streamingHttpClient");
        field.setAccessible(true);
        return (OkHttpClient) field.get(client);
    }
}
