# Under-Utils AI Starter

Spring Boot 自动装配入口，用于按配置创建默认 `AiClient`。

本 starter 只依赖 `under-utils-ai` 和 Spring Boot autoconfigure，不会被 `under-utils-starter` 聚合引入。需要 AI 能力的项目应显式引入该坐标。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-ai-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

## 配置

默认不会创建 `AiClient`，需要显式启用：

```yaml
under:
  utils:
    ai:
      enabled: true
      provider: openai-compatible
      base-url: https://api.example.com/v1
      api-key: ${AI_API_KEY}
      model: your-model-name
      timeout: 30s
      stream-read-timeout: 0ms
      max-retries: 0
      retry-interval: 0ms
```

使用：

```java
@Service
public class SummaryService {

    private final AiClient aiClient;

    public SummaryService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public String summarize(String content) {
        return aiClient.chat(ChatRequest.user("请总结：" + content)).text();
    }
}
```

多模型配置：

该能力位于当前 `1.0.3-SNAPSHOT` 开发周期，正式发布后再使用对应稳定版本坐标。

```yaml
under:
  utils:
    ai:
      enabled: true
      default-client: deepseek
      timeout: 30s
      clients:
        deepseek:
          provider: openai-compatible
          base-url: https://api.deepseek.example/v1
          api-key: ${DEEPSEEK_API_KEY}
          model: deepseek-chat
        qwen:
          provider: openai-compatible
          base-url: https://dashscope-compatible.example/v1
          api-key: ${QWEN_API_KEY}
          model: qwen-plus
```

使用命名客户端：

```java
@Service
public class ModelRouter {

    private final AiClientRegistry aiClients;

    public ModelRouter(AiClientRegistry aiClients) {
        this.aiClients = aiClients;
    }

    public String askQwen(String prompt) {
        return aiClients.get("qwen").chat(ChatRequest.user(prompt)).text();
    }
}
```

顶层 `provider`、`base-url`、`api-key`、`model`、`timeout`、`stream-read-timeout`、`max-retries`、`retry-interval`、`temperature`、`max-tokens` 和 `headers` 可作为命名客户端默认值；单个客户端配置会覆盖同名字段。未配置 `clients` 时，starter 会继续按旧的顶层配置创建名为 `default` 的客户端。

`stream-read-timeout` 只影响 SSE 分片读取等待时间，默认 `0ms` 表示不限制，避免长流式响应被普通 read timeout 提前断开；连接、写入和同步请求读写仍使用 `timeout`。

## 自动装配规则

- `under.utils.ai.enabled=true` 时才创建默认 `AiClient`。
- 默认内置 `openai-compatible` provider。
- 配置 `under.utils.ai.clients.<name>.*` 时会创建 `AiClientRegistry`，并将默认客户端作为 `AiClient` Bean 暴露。
- 如需非兼容协议，可声明自定义 `AiClientProvider` Bean，并将顶层或命名客户端的 `provider` 设置为该 provider 名称。
- 用户自定义 `AiClient` Bean 时，自动装配会退让，不再强制创建注册表。
- `api-key` 为空时不会发送 Authorization header，适合本地兼容服务。

## 流式响应

默认创建的 `OpenAiCompatibleAiClient` 同时实现 `StreamingAiClient`，可用于 SSE 流式输出：

```java
if (aiClient instanceof StreamingAiClient streamingAiClient) {
    try (ChatStream stream = streamingAiClient.streamChat(ChatRequest.user("请逐步输出"))) {
        stream.stream()
                .filter(ChatStreamEvent::hasText)
                .forEach(event -> System.out.print(event.text()));
    }
}
```
