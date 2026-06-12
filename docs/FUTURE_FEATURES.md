# 后续功能孵化

本文件用于记录尚未进入实现的新功能想法。它的目标是先把“为什么做、做到什么边界、什么不做”说清楚，再决定是否进入代码实现。

## 使用方式

- 新想法先进入本文件，不直接新增模块或 public API。
- 进入实现前，必须明确模块归属、依赖边界、配置项、失败语义、测试计划和兼容性影响。
- 已进入工程成熟度推进的事项，放到 [ENGINEERING_MATURITY.md](ENGINEERING_MATURITY.md)，不要在两个文件重复维护。

## 状态定义

| 状态 | 含义 |
|------|------|
| `想法` | 只有场景和方向，尚未确定是否适合进入项目。 |
| `候选` | 已确认与项目边界基本匹配，等待设计。 |
| `设计中` | 正在明确 API、依赖、配置、失败语义和验收标准。 |
| `实现中` | 已开始代码实现。 |
| `已实现` | 已进入 `main`，完成代码、测试和文档，等待正式版本发布。 |
| `已发布` | 已进入正式版本并完成文档、测试和变更记录。 |
| `拒绝` | 不符合项目边界，或已有成熟库更适合承载。 |

## 功能准入标准

新功能进入实现前，应同时满足：

- 解决跨项目重复出现的工程问题，而不是单个业务应用的流程。
- 不只是对 JDK、Spring、Hutool、Apache Commons、Guava 或成熟生态库的低价值转包。
- 有明确的 public API、配置 key、失败语义和资源释放边界。
- 单元测试可以离线运行；涉及外部系统的测试必须可通过 mock server、Testcontainers 或独立 profile 复现。
- 依赖重量与模块定位匹配，不让轻量模块被动引入大依赖。
- 可以在 `1.x` 版本内保持源码兼容，或者明确作为 minor/major 能力规划。

## F-001 AI 大模型基础调用封装

状态：`已实现`

### 背景

后续可能需要提供一个面向 Java/Spring 项目的 AI 大模型基础封装：调用方只配置模型服务的基本参数，就能完成最常见的文本对话调用。

这个能力的重点不是做完整 Agent 框架，而是收敛不同业务项目重复编写的模型调用基础代码，例如 base URL、API key、model、timeout、retry、错误处理和日志脱敏。

### 目标

- 通过最少配置创建可用的 AI client。
- 支持 OpenAI-compatible HTTP API 作为第一阶段协议目标。
- 提供同步文本对话调用，优先覆盖最常见的 user/system/assistant message 场景。
- 统一超时、重试、错误响应解析、trace header 和敏感信息脱敏。
- core API 不强依赖 Spring；Spring Boot 自动装配放入独立 starter 或配置层。

### 非目标

- 不做模型训练、微调、数据标注或计费系统。
- 不做完整 Agent、工作流编排、工具调用市场或多步骤推理框架。
- 不内置向量数据库、RAG 检索链路或知识库管理。
- 不承诺覆盖所有模型厂商的全部私有参数。
- 不在日志、异常或 debug 输出中暴露 API key、Authorization header 或完整敏感 prompt。

### 候选模块

| 模块 | 说明 |
|------|------|
| `under-utils-ai` | 模型调用抽象、请求响应模型、错误类型、OpenAI-compatible 执行器。 |
| `under-utils-ai-starter` | Spring Boot 自动装配、配置属性和默认 `AiClient` Bean。 |

### 候选 API 草案

```java
AiClient aiClient = AiClient.builder()
        .baseUrl("https://api.example.com/v1")
        .apiKey(apiKey)
        .model("your-model-name")
        .timeout(Duration.ofSeconds(30))
        .build();

ChatResponse response = aiClient.chat(ChatRequest.user("请总结这段文本"));
String text = response.text();
```

Spring Boot 配置草案：

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
      retry:
        max-attempts: 2
        backoff: 500ms
```

### 候选核心类型

| 类型 | 说明 |
|------|------|
| `AiClient` | 对外调用入口，提供 `chat` 等基础方法。 |
| `AiClientOptions` | base URL、API key、model、timeout、retry、headers 等配置。 |
| `ChatRequest` | message、temperature、max tokens、request id 等请求参数。 |
| `ChatMessage` | system/user/assistant 等角色消息。 |
| `ChatResponse` | 文本、模型名、token 用量、原始 request id 等响应信息。 |
| `AiException` | 统一模型调用异常，区分认证失败、限流、超时、服务端错误和响应解析失败。 |

### 第一阶段验收标准

- 只配置 base URL、API key 和 model 就能完成一次文本对话调用。
- API key 不会出现在日志、异常消息或 `toString()` 输出中。
- 单元测试通过 mock HTTP server 覆盖成功响应、401/429/5xx、超时和响应解析失败。
- 默认测试不访问外网。
- 文档提供 Java builder 和 Spring Boot YAML 两种用法。
- 依赖边界经过评估，不能让现有轻量模块被动引入 AI 或 HTTP 大依赖。
- 与 `under-utils-http` 的复用关系明确：可以复用执行器能力，但不能让 AI API 被 HTTP 内部模型绑死。

### 第一阶段实现记录

- 已新增 `under-utils-ai` 核心模块，提供 `AiClient`、`AiClientOptions`、`ChatRequest`、`ChatMessage`、`ChatResponse`、`TokenUsage`、`AiException` 和 `OpenAiCompatibleAiClient`。
- 第一阶段只实现同步文本对话，协议目标为 OpenAI-compatible Chat Completions。
- 复用 `under-utils-http` 的 `HttpRequest`、`HttpConfig` 和 `HttpResponse`，但 AI 模块对外不暴露 HTTP 内部请求/响应模型。
- 已用 `MockWebServer` 覆盖成功响应、认证失败、限流、服务端错误、超时、响应解析失败和敏感信息不进入 `toString()`。
- 已新增独立 `under-utils-ai-starter`，在 `under.utils.ai.enabled=true` 时按配置创建默认 `AiClient`；它不加入 `under-utils-starter` 聚合入口，避免普通 Spring/Redis 用户被动引入 AI 依赖。
- 已在 `under-utils-samples` 增加 `ai` profile 和 `/samples/ai/*` 示例接口，默认 profile 只暴露状态，不会在未配置模型服务时访问外部网络。

### 第一阶段结论

- 第一阶段只支持 OpenAI-compatible 协议，先不封装国内厂商原生私有 API。
- 第一阶段只提供同步文本对话，不引入流式响应、工具调用、RAG 或 Agent 编排。
- 第一阶段继续复用 `under-utils-http` 执行能力，AI 模块不把 HTTP 内部请求/响应模型暴露为 public API。
- 已暴露 token 用量、模型名和 finish reason；模型指纹等元数据暂不进入第一阶段 public API。
- starter 默认不创建 `AiClient`，必须显式设置 `under.utils.ai.enabled=true`。

## F-002 AI 流式响应与厂商扩展

状态：`已实现`

### 背景

F-001 已覆盖最小可用的同步文本对话。后续如果真实项目需要更低首 token 延迟、SSE 输出或国内模型厂商原生协议，可以进入第二阶段。

### 目标

- 提供 OpenAI-compatible SSE 流式响应 API。
- 提供 provider 扩展边界：优先通过 OpenAI-compatible 参数透传解决，只有协议差异无法兼容时才新增 provider。
- 保持同步 `AiClient` API 稳定，不把第二阶段能力强行塞进第一阶段入口。
- 明确取消、超时、连接中断和半截响应的失败语义。

### 非目标

- 不做完整 Agent 编排、工具调用市场、RAG 知识库或模型路由平台。
- 不引入具体厂商 SDK 作为默认依赖。
- 不让 `under-utils-ai-starter` 进入 `under-utils-starter` 聚合入口。

### 候选模块

| 模块 | 说明 |
|------|------|
| `under-utils-ai` | 增加流式响应抽象和 provider 扩展点。 |
| `under-utils-ai-starter` | 只在配置明确启用时装配第二阶段能力。 |

### 第二阶段验收标准

- 默认测试仍不访问外网。已通过 MockWebServer 覆盖。
- 流式 API 能通过 MockWebServer 覆盖正常分片、服务端错误、连接中断和取消。已覆盖。
- provider 扩展不能要求普通用户引入额外厂商 SDK。已通过 `AiClientProvider` 实现。
- 新增配置 key 和 public API 必须记录在 `CHANGELOG.md`、`docs/API_REVIEW.md` 和模块 README。已同步到 API Review 和模块 README。

### 第二阶段实现记录

- 新增 `StreamingAiClient`、`ChatStream` 和 `ChatStreamEvent`，流式能力与同步 `AiClient` 分离，避免同步入口膨胀。
- `OpenAiCompatibleAiClient` 实现 `StreamingAiClient`，请求体自动加入 `stream=true` 并消费 SSE `data:` 分片。
- `ChatStream` 只能消费一次并实现 `AutoCloseable`，调用方可通过 try-with-resources 主动取消和释放 HTTP 连接。
- 新增 `AiResponseMetadata`，同步响应和流式事件均可暴露 provider、调用方 request id、模型服务 response id、模型指纹和耗时。
- 新增 `AiClientProvider` 和 `OpenAiCompatibleAiClientProvider`，业务侧可以扩展 provider，不引入默认厂商 SDK。
- `under-utils-ai-starter` 支持按 `under.utils.ai.provider` 匹配自定义 `AiClientProvider` Bean；用户自定义 `AiClient` Bean 时继续退让。
- `under-utils-samples` 增加 `/samples/ai/chat/stream` SSE 示例。

### 第二阶段结论

- 先做 OpenAI-compatible SSE 流式响应，不做 WebSocket 或厂商私有流式协议。
- 使用独立 `StreamingAiClient` 承载流式能力，保持 `AiClient.chat(...)` 同步 API 简洁。
- provider 扩展只定义接口和 starter 路由，不内置国内厂商 SDK。
- 元数据模型进入 public API，但仍避免暴露完整 prompt、API key、Authorization header 或完整模型回复。

## F-003 AI 多模型客户端配置与命名路由

状态：`已实现`

### 背景

F-001 和 F-002 已经覆盖单个 OpenAI-compatible 客户端的同步与流式调用。真实业务项目常见需求是同一应用内同时接入多个模型服务，例如默认模型、低成本模型、长文本模型或不同供应商兼容端点。

### 目标

- 在 core 层提供按名称管理多个 `AiClient` 的注册表。
- 在 starter 层支持 `under.utils.ai.clients.<name>.*` 多客户端配置。
- 继续保留旧的单客户端顶层配置，避免已有 `under.utils.ai.base-url` / `model` 用户迁移成本。
- 默认客户端仍作为 `AiClient` Bean 暴露，普通单模型应用不需要理解 registry。
- 命名客户端可以继承顶层通用配置，并覆盖自己的 base URL、API key、model、headers 和 provider。

### 非目标

- 不做模型自动路由、成本优化、灰度调度或健康检查。
- 不新增厂商 SDK；命名客户端仍优先走 OpenAI-compatible 协议。
- 不把 AI starter 放入 `under-utils-starter` 聚合入口。

### 实现记录

- 新增 `AiClientRegistry` 和 `DefaultAiClientRegistry`，支持 `getDefaultClient()`、`get(name)`、`find(name)`、`names()` 和流式客户端获取。
- `under-utils-ai-starter` 新增 `default-client` 与 `clients` 配置；未配置 `clients` 时继续创建名为 `default` 的兼容客户端。
- `AiClientRegistry` 自动装配后，默认客户端继续以 `AiClient` Bean 暴露；用户自定义 `AiClient` Bean 时自动装配退让。
- `under-utils-samples` AI profile 增加 `secondary` 命名客户端示例和按名称调用的同步/流式接口。

### 验收结果

- 已通过 core registry 单元测试覆盖默认客户端、命名查找、缺失客户端和流式能力边界。
- 已通过 starter 测试覆盖旧单客户端配置、多命名客户端配置、默认客户端选择、headers 继承、自定义 provider 和用户自定义 `AiClient` 退让。
- 默认测试仍使用 MockWebServer，不访问外网。

## F-004 MyBatis Starter 零配置入口

状态：`已实现`

### 背景

真实 MyBatis-Plus 项目反复需要配置分页插件、乐观锁、防全表更新删除和审计字段填充。`under-utils-mybatis` 已有能力，但用户仍要手写配置类。

### 目标

- 新增独立 `under-utils-mybatis-starter`。
- 默认装配 `MybatisPlusInterceptor` 和 `DefaultMetaObjectHandler`。
- 通过 `under.utils.mybatis.*` 配置数据库类型、审计字段名和逻辑删除填充值。
- 用户已有同类型 Bean 时自动退让。

### 非目标

- 不加入 `under-utils-starter` 聚合入口。
- 不接管数据源、事务、mapper 扫描或多数据源路由。

### 实现记录

- 新增 `UnderUtilsMybatisAutoConfiguration` 和 `UnderUtilsMybatisProperties`。
- 新增 starter README 和自动装配测试，覆盖默认装配、配置关闭、审计字段配置和用户 Bean 退让。

## F-005 服务层业务幂等

状态：`已实现`

### 背景

`@PreventRepeat` 解决 HTTP 入口层重复点击，但 MQ 重试、RPC 重试和跨服务回调需要服务层业务幂等：同一业务 key 只执行一次，完成后重复调用返回第一次结果。

### 目标

- 新增 `@Idempotent`，独立于 `@PreventRepeat`。
- 同一 key 首次执行中，重复调用立即抛出处理中异常。
- 首次执行成功完成后，重复调用返回第一次结果。
- 提供本地 store、Redis store 和 JDBC store，支持 starter 配置切换。

### 非目标

- 第一阶段不做阻塞等待首结果。
- 不做分布式事务保证。
- 不缓存失败结果。

### 实现记录

- 新增 `IdempotentAspect`、`IdempotencyStore`、`LocalIdempotencyStore`、`RedisIdempotencyStore` 和 `IdempotencyResultCodec`。
- 新增 `under.utils.idempotent.*` 配置，支持 store、key prefix、processing/result TTL、本地容量和清理周期。
- 显式 SpEL key 解析失败会抛出异常，不再兜底，避免误幂等。
- store 完成和释放 key 均带执行 owner token，避免 processing TTL 过期后旧执行覆盖新执行。
- 单元测试覆盖本地状态机、切面行为、key 解析、结果编解码、Redis store mock；Testcontainers 集成测试放入 `under-utils-test`。

### 1.0.5 增强记录

- 新增 `under-utils-jdbc` 和 `under-utils-jdbc-starter`，通过业务数据库保存幂等处理中状态和完成态结果。
- JDBC store 不自动建表，不接管数据源或事务；starter 只有在 `under.utils.idempotent.store=jdbc` 且存在 `JdbcOperations` 时装配。
- JDBC 模块随包提供 MySQL/PostgreSQL 建表脚本，幂等表对 `expire_at` 建索引，便于过期记录清理。
- JDBC starter 默认创建过期记录清理任务，支持 `cleanup-enabled`、`cleanup-initial-delay` 和 `cleanup-interval` 配置。
- 新增 `IdempotencyObserver` SPI 和 `MicrometerIdempotencyObserver`，覆盖 begin、business、complete 和 release 事件。
- Micrometer 幂等指标只使用低基数 tag，不记录幂等 key、方法签名或业务参数。
- `IdempotentAspect` 在业务异常后释放 key 失败时保留原业务异常，把释放失败作为 suppressed exception。

## F-006 字段级加密与响应脱敏

状态：`已实现`

### 背景

企业项目中手机号、证件号、银行卡号等敏感字段常需要落库前加密、响应前脱敏。历史 `AESUtils` 属于兼容工具类，不适合继续承载安全治理语义。

### 目标

- 新增独立 `under-utils-security`。
- 提供 AES-GCM 字段级加密、key provider 和密文 envelope。
- 提供 MyBatis 显式加密 TypeHandler。
- 提供 security 侧响应脱敏注解。
- 提供 `under-utils-security-starter` 和 `under.utils.security.*` 配置。

### 非目标

- 不提供 KMS、密钥托管或轮换调度。
- 不做全局隐式字段加密。
- 不迁移或删除 `under-utils-spring` 的 `@Sensitive`。

### 实现记录

- 新增 `FieldEncryptor`、`AesGcmFieldEncryptor`、`KeyProvider`、`StaticKeyProvider`。
- 新增 `EncryptedStringTypeHandler`，要求实体字段显式声明 TypeHandler，并配合 `@TableName(autoResultMap = true)`。
- 新增 `@Mask`、`MaskType`、`MaskingJsonSerializer` 和 `MaskingUtils`。
- security starter 只有显式配置 Base64 AES key 且未关闭 `field-encryption.enabled` 时才创建默认 `FieldEncryptor` 并注册 TypeHandler 默认加密器。

## F-007 模块选择与轻量化治理

状态：`已实现`

### 背景

`1.0.4` 和 `1.0.5` 后，项目已经覆盖 Spring、Redis、HTTP、AI、Security、MyBatis、JDBC 和 Biz 等多个方向。依赖拆分仍然可控，但用户的认知重量开始上升：新用户需要知道该引哪个 starter、哪些能力会自动装配、哪些模块不会进入旧聚合入口。

### 目标

- 明确新项目优先按需选择独立 starter。
- 明确 `under-utils-starter` 只保留为兼容聚合入口。
- 提供模块选择矩阵和常见组合。
- 把 README、Quick Start、依赖审计和官网索引统一到同一套选择口径。
- 为后续功能评估增加“是否增加认知重量”的准入检查。

### 非目标

- 不新增运行时 public API。
- 不新增配置 key。
- 不新增 Maven artifact。
- 不修改 starter 的默认自动装配语义。

### 实现记录

- 新增 `docs/MODULE_SELECTION.md`，记录独立 starter、library 模块、旧聚合 starter 和不推荐组合。
- README 和 Quick Start 增加模块选择表，减少用户在多个 starter 之间猜测。
- 依赖审计增加 `1.0.6` 轻量化治理口径，明确后续新增能力默认不进入 `under-utils-starter`。
- 依赖审计已按当前 `1.0.5-SNAPSHOT` 构建产物重新采集主 jar 大小和 runtime 依赖树，量化各 module/starter 的重量。
- starter 的 Spring Boot configuration processor 改为编译期 annotation processor path，不再作为普通 optional 依赖出现在 starter POM 中。
- API Review 记录本轮为文档和治理增强，不改变已发布 API 的默认语义。

## 新功能记录模板

```markdown
## F-XXX 功能名称

状态：`想法`

### 背景

说明重复场景和当前痛点。

### 目标

- 目标 1
- 目标 2

### 非目标

- 不做什么

### 候选模块

说明模块归属和依赖边界。

### 候选 API 草案

给出最小可读示例。

### 第一阶段验收标准

- 可测试标准
- 文档标准
- 兼容性标准

### 待确认问题

- 尚未决定的问题
```
