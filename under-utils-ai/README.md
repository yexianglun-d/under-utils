# Under-Utils AI

AI 大模型基础调用封装模块，提供 OpenAI-compatible 文本对话客户端。

本模块目标是让业务项目只配置模型服务的基础参数，就能完成最常见的同步文本对话和 SSE 流式对话调用；它不是 Agent 框架，也不覆盖 RAG、向量数据库或厂商私有全部协议。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-ai</artifactId>
    <version>1.0.5</version>
</dependency>
```

## 快速使用

```java
AiClient aiClient = AiClient.builder()
        .baseUrl("https://api.example.com/v1")
        .apiKey(apiKey)
        .model("your-model-name")
        .timeout(Duration.ofSeconds(30))
        .streamReadTimeout(Duration.ZERO)
        .build();

ChatResponse response = aiClient.chat(ChatRequest.user("请总结这段文本"));
String text = response.text();
```

构建器会调用 `{baseUrl}/chat/completions`，请求体使用 OpenAI-compatible Chat Completions 结构：

```json
{
  "model": "your-model-name",
  "messages": [
    {
      "role": "user",
      "content": "请总结这段文本"
    }
  ]
}
```

## 流式响应

`OpenAiCompatibleAiClient` 同时实现 `StreamingAiClient`。流式响应基于 OpenAI-compatible SSE，调用方必须关闭 `ChatStream`：

```java
StreamingAiClient streamingClient = (StreamingAiClient) aiClient;

try (ChatStream stream = streamingClient.streamChat(ChatRequest.user("请逐步输出一句话"))) {
    for (ChatStreamEvent event : stream) {
        if (event.hasText()) {
            System.out.print(event.text());
        }
        if (event.isDone()) {
            break;
        }
    }
}
```

流式请求会在请求体中写入 `"stream": true`，并读取 `data:` SSE 分片。`ChatStreamEvent` 暴露增量文本、角色、结束原因、token 用量和元数据；连接中断、超时、HTTP 错误和分片解析失败都会映射为 `AiException`。

`timeout` 控制连接、普通同步请求读写和流式写入超时；`streamReadTimeout` 只控制 SSE 分片读取等待时间。默认值为 `Duration.ZERO`，表示不限制长连接分片等待，避免模型长时间思考或低频输出时被普通 read timeout 提前断开。

## 多轮消息

```java
ChatResponse response = aiClient.chat(ChatRequest.builder()
        .system("你是一个简洁的助手")
        .user("请把下面内容压缩成三句话")
        .assistant("请提供原文")
        .user(content)
        .temperature(0.2D)
        .maxTokens(512)
        .requestId(requestId)
        .build());
```

`ChatRequest` 可以按请求覆盖默认模型，也可以通过 `extraBody` 透传少量兼容参数：

```java
ChatRequest request = ChatRequest.builder()
        .user("ping")
        .model("another-model")
        .extraBody("top_p", 0.9D)
        .build();
```

## 原生消息和工具参数

文本 helper 无法表达多模态 content 数组、tool 结果或厂商兼容扩展时，可以使用 OpenAI-compatible 原生消息结构。原生消息不能和 `.user()`、`.system()` 等 typed message helper 混用，避免消息顺序语义不清。

```java
ChatRequest request = ChatRequest.builder()
        .nativeMessage(Map.of(
                "role", "user",
                "content", List.of(Map.of(
                        "type", "text",
                        "text", "请按 JSON 输出"
                ))
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
        .build();
```

当模型返回 `tool_calls` 或 `function_call` 且 `content=null` 时，`ChatResponse.text()` 返回空字符串，原始 assistant 消息可通过 `response.getRawMessage()` 读取。本模块只负责兼容协议传输和基础解析，工具执行编排仍由业务侧负责。

## 多客户端注册表

该能力自 `1.0.3` 起可在稳定版本坐标中使用。

同一个应用需要同时接入多个模型服务时，可以使用 `AiClientRegistry` 按名称路由：

```java
Map<String, AiClient> clients = new LinkedHashMap<>();
clients.put("deepseek", AiClient.builder()
        .baseUrl("https://api.deepseek.example/v1")
        .apiKey(deepseekKey)
        .model("deepseek-chat")
        .build());
clients.put("qwen", AiClient.builder()
        .baseUrl("https://dashscope-compatible.example/v1")
        .apiKey(qwenKey)
        .model("qwen-plus")
        .build());

AiClientRegistry registry = new DefaultAiClientRegistry(clients, "deepseek");

String text = registry.get("qwen")
        .chat(ChatRequest.user("请总结这段文本"))
        .text();
```

`AiClientRegistry#getDefaultClient()` 返回默认客户端，`getStreaming(name)` 会在目标客户端不支持流式响应时抛出明确异常。

## 响应元数据

`ChatResponse` 提供文本、模型名、结束原因和 token 用量，同时通过 `AiResponseMetadata` 暴露 provider、调用方 request id、模型服务 response id、模型指纹和请求耗时：

```java
ChatResponse response = aiClient.chat(ChatRequest.builder()
        .user("ping")
        .requestId("req-001")
        .build());

String responseId = response.getResponseId();
String modelFingerprint = response.getModelFingerprint();
Duration duration = response.getDuration();
```

`getRequestId()` 保留为兼容字段，优先返回模型服务响应 ID；如果需要区分调用方 request id 与模型服务 response id，请使用 `response.getMetadata()`。

## Provider 扩展

默认 provider 为 `openai-compatible`。如果业务项目需要接入非兼容协议，可以实现 `AiClientProvider`，不需要把具体厂商 SDK 放进 Under-Utils 默认依赖面：

```java
AiClientProvider provider = new AiClientProvider() {
    @Override
    public String provider() {
        return "native-vendor";
    }

    @Override
    public AiClient create(AiClientOptions options) {
        return new NativeVendorAiClient(options);
    }
};

AiClient client = AiClient.builder()
        .baseUrl("https://api.example.com")
        .model("vendor-model")
        .provider(provider)
        .build();
```

## 错误语义

模型调用失败统一抛出 `AiException`：

| 分类 | 触发场景 |
|------|----------|
| `AUTHENTICATION` | HTTP 401/403。 |
| `RATE_LIMIT` | HTTP 429。 |
| `SERVER_ERROR` | HTTP 5xx。 |
| `CLIENT_ERROR` | 其他 HTTP 4xx。 |
| `TIMEOUT` | 连接、读取或写入超时。 |
| `NETWORK` | 网络连接失败。 |
| `RESPONSE_PARSE` | 成功响应无法解析，或缺少 assistant 文本。 |

`AiException` 暴露 `statusCode`、`errorCode` 和 `retryable`，调用方可以据此决定重试或降级。异常信息不会包含 API key、Authorization header 或完整请求体。

## 安全边界

- `AiClientOptions.toString()` 会隐藏 API key，只显示是否已配置。
- `ChatRequest.toString()` 不输出完整 prompt，只输出消息数量和参数摘要。
- `ChatResponse.toString()` 不输出完整模型回复，只输出文本长度和元数据。
- 默认测试使用 `MockWebServer`，不会访问外网。

## 当前限制

- 流式响应只覆盖 OpenAI-compatible SSE，不封装 WebSocket 或厂商私有流式协议。
- 支持 tools/function calling 参数透传和原始响应读取，但不负责工具执行、Agent 工作流和多步骤推理。
- 默认不引入具体厂商 SDK；厂商原生协议应通过业务侧 `AiClientProvider` 扩展。
- Spring Boot 自动装配已放入独立 `under-utils-ai-starter`，不会被 `under-utils-starter` 聚合引入。
