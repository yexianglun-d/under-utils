package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.ai.AiClient;
import com.undernine.utils.ai.AiClientOptions;
import com.undernine.utils.ai.AiClientProvider;
import com.undernine.utils.ai.AiClientRegistry;
import com.undernine.utils.ai.ChatRequest;
import com.undernine.utils.ai.ChatResponse;
import com.undernine.utils.starter.properties.UnderUtilsAiProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class UnderUtilsAiAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UnderUtilsAiAutoConfiguration.class));

    @Test
    void shouldNotCreateAiClientByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(AiClient.class));
    }

    @Test
    void shouldCreateOpenAiCompatibleClientWhenEnabled() throws IOException, InterruptedException {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {
                              "id": "chatcmpl-starter",
                              "model": "starter-model",
                              "choices": [
                                {
                                  "message": {
                                    "role": "assistant",
                                    "content": "starter-ok"
                                  }
                                }
                              ]
                            }
                            """));

            contextRunner
                    .withPropertyValues(
                            "under.utils.ai.enabled=true",
                            "under.utils.ai.base-url=" + server.url("/v1"),
                            "under.utils.ai.api-key=starter-secret",
                            "under.utils.ai.model=starter-model",
                            "under.utils.ai.timeout=2s",
                            "under.utils.ai.max-retries=0",
                            "under.utils.ai.headers.X-App-Id=demo"
                    )
                    .run(context -> {
                        assertThat(context).hasSingleBean(AiClient.class);
                        assertThat(context).hasSingleBean(AiClientRegistry.class);
                        assertThat(context.getBean(AiClientRegistry.class).getDefaultName()).isEqualTo("default");
                        String text = context.getBean(AiClient.class)
                                .chat(ChatRequest.user("hello"))
                                .text();
                        assertThat(text).isEqualTo("starter-ok");
                    });

            RecordedRequest request = server.takeRequest();
            assertThat(request.getPath()).isEqualTo("/v1/chat/completions");
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer starter-secret");
            assertThat(request.getHeader("X-App-Id")).isEqualTo("demo");
        }
    }

    @Test
    void shouldCreateNamedAiClientRegistry() throws IOException, InterruptedException {
        try (MockWebServer deepseekServer = new MockWebServer();
             MockWebServer qwenServer = new MockWebServer()) {
            deepseekServer.start();
            qwenServer.start();
            deepseekServer.enqueue(responseBody("deepseek-ok", "deepseek-chat"));
            qwenServer.enqueue(responseBody("qwen-ok", "qwen-plus"));

            contextRunner
                    .withPropertyValues(
                            "under.utils.ai.enabled=true",
                            "under.utils.ai.default-client=qwen",
                            "under.utils.ai.timeout=2s",
                            "under.utils.ai.headers.X-Root=root",
                            "under.utils.ai.clients.deepseek.provider=openai-compatible",
                            "under.utils.ai.clients.deepseek.base-url=" + deepseekServer.url("/v1"),
                            "under.utils.ai.clients.deepseek.api-key=deepseek-secret",
                            "under.utils.ai.clients.deepseek.model=deepseek-chat",
                            "under.utils.ai.clients.deepseek.headers.X-Client=deepseek",
                            "under.utils.ai.clients.qwen.base-url=" + qwenServer.url("/compatible-mode/v1"),
                            "under.utils.ai.clients.qwen.api-key=qwen-secret",
                            "under.utils.ai.clients.qwen.model=qwen-plus",
                            "under.utils.ai.clients.qwen.headers.X-Client=qwen"
                    )
                    .run(context -> {
                        assertThat(context).hasSingleBean(AiClientRegistry.class);
                        assertThat(context).hasSingleBean(AiClient.class);
                        AiClientRegistry registry = context.getBean(AiClientRegistry.class);
                        assertThat(registry.getDefaultName()).isEqualTo("qwen");
                        assertThat(registry.names()).containsExactly("deepseek", "qwen");
                        assertThat(context.getBean(AiClient.class)).isSameAs(registry.getDefaultClient());
                        assertThat(registry.getDefaultClient().chat(ChatRequest.user("hello")).text())
                                .isEqualTo("qwen-ok");
                        assertThat(registry.get("deepseek").chat(ChatRequest.user("hello")).text())
                                .isEqualTo("deepseek-ok");
                    });

            RecordedRequest qwenRequest = qwenServer.takeRequest();
            assertThat(qwenRequest.getPath()).isEqualTo("/compatible-mode/v1/chat/completions");
            assertThat(qwenRequest.getHeader("Authorization")).isEqualTo("Bearer qwen-secret");
            assertThat(qwenRequest.getHeader("X-Root")).isEqualTo("root");
            assertThat(qwenRequest.getHeader("X-Client")).isEqualTo("qwen");

            RecordedRequest deepseekRequest = deepseekServer.takeRequest();
            assertThat(deepseekRequest.getPath()).isEqualTo("/v1/chat/completions");
            assertThat(deepseekRequest.getHeader("Authorization")).isEqualTo("Bearer deepseek-secret");
            assertThat(deepseekRequest.getHeader("X-Root")).isEqualTo("root");
            assertThat(deepseekRequest.getHeader("X-Client")).isEqualTo("deepseek");
        }
    }

    @Test
    void shouldUseFirstNamedClientAsDefaultWhenDefaultClientNotSet() {
        contextRunner
                .withPropertyValues(
                        "under.utils.ai.enabled=true",
                        "under.utils.ai.clients.deepseek.base-url=https://api.example.com/v1",
                        "under.utils.ai.clients.deepseek.model=deepseek-chat"
                )
                .run(context -> assertThat(context.getBean(AiClientRegistry.class).getDefaultName())
                        .isEqualTo("deepseek"));
    }

    @Test
    void shouldKeepLegacyAiClientFactoryMethod() {
        UnderUtilsAiProperties properties = new UnderUtilsAiProperties();
        properties.setBaseUrl("https://api.example.com/v1");
        properties.setModel("demo-model");
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        AiClient client = new UnderUtilsAiAutoConfiguration().aiClient(properties,
                beanFactory.getBeanProvider(AiClientProvider.class));

        assertThat(client).isInstanceOf(AiClient.class);
    }

    @Test
    void shouldBackOffWhenUserProvidesAiClient() {
        AiClient customClient = request -> null;

        contextRunner
                .withBean(AiClient.class, () -> customClient)
                .withPropertyValues(
                        "under.utils.ai.enabled=true",
                        "under.utils.ai.base-url=https://api.example.com/v1",
                        "under.utils.ai.api-key=secret",
                        "under.utils.ai.model=demo-model"
                )
                .run(context -> {
                    assertThat(context.getBean(AiClient.class)).isSameAs(customClient);
                    assertThat(context).doesNotHaveBean(AiClientRegistry.class);
                });
    }

    @Test
    void shouldUseCustomProviderWhenProviderMatches() {
        AiClientProvider customProvider = new AiClientProvider() {
            @Override
            public String provider() {
                return "native-vendor";
            }

            @Override
            public AiClient create(AiClientOptions options) {
                return request -> new ChatResponse("custom-provider", options.getModel(), "stop", "custom-001", null);
            }
        };

        contextRunner
                .withBean(AiClientProvider.class, () -> customProvider)
                .withPropertyValues(
                        "under.utils.ai.enabled=true",
                        "under.utils.ai.provider=native-vendor",
                        "under.utils.ai.base-url=https://api.example.com/v1",
                        "under.utils.ai.api-key=secret",
                        "under.utils.ai.model=native-model"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AiClient.class);
                    assertThat(context).hasSingleBean(AiClientRegistry.class);
                    assertThat(context.getBean(AiClient.class).chat(ChatRequest.user("hello")).text())
                            .isEqualTo("custom-provider");
                });
    }

    @Test
    void shouldBindStreamReadTimeout() {
        AiClientProvider customProvider = new AiClientProvider() {
            @Override
            public String provider() {
                return "native-vendor";
            }

            @Override
            public AiClient create(AiClientOptions options) {
                return request -> new ChatResponse(options.getStreamReadTimeout().toString(),
                        options.getModel(), "stop", "custom-002", null);
            }
        };

        contextRunner
                .withBean(AiClientProvider.class, () -> customProvider)
                .withPropertyValues(
                        "under.utils.ai.enabled=true",
                        "under.utils.ai.provider=native-vendor",
                        "under.utils.ai.base-url=https://api.example.com/v1",
                        "under.utils.ai.model=native-model",
                        "under.utils.ai.stream-read-timeout=12s"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AiClient.class);
                    assertThat(context.getBean(AiClient.class).chat(ChatRequest.user("hello")).text())
                            .isEqualTo(Duration.ofSeconds(12).toString());
                });
    }

    @Test
    void shouldFailFastForUnsupportedProvider() {
        contextRunner
                .withPropertyValues(
                        "under.utils.ai.enabled=true",
                        "under.utils.ai.provider=native-vendor",
                        "under.utils.ai.base-url=https://api.example.com/v1",
                        "under.utils.ai.api-key=secret",
                        "under.utils.ai.model=demo-model"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    private MockResponse responseBody(String text, String model) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": "chatcmpl-test",
                          "model": "%s",
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "%s"
                              }
                            }
                          ]
                        }
                        """.formatted(model, text));
    }
}
