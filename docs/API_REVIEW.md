# API Review

本文件记录 Under-Utils public API 审计结论，用于持续收敛命名、配置 key、异常语义和模块边界。

## 审计原则

- 优先稳定发布后难以修改的入口：包名、类名、方法签名、配置属性、注解属性和异常类型。
- 不把低复杂度通用工具方法作为新增方向，历史 core 工具只做兼容维护。
- starter 默认行为必须可解释，配置 key 一经发布尽量不破坏。
- 对存在安全边界的 API，默认路径应使用白名单、显式配置或封装模板。

## 第一轮结论

### HTTP

- 将底层 OkHttp 执行入口从 `OkHttpClient` 收敛为 `OkHttpRequestExecutor`，避免与 `okhttp3.OkHttpClient` 同名并误导使用者。
- `HttpRequest` 继续作为兼容的便捷请求模型，OpenAPI 治理能力仍以 `OpenApiClient`、`OpenApiRequest`、`OpenApiResponse` 为主线。

### Redis

- `LogicalExpireCacheOptions` 明确校验 `physicalTtl > logicalTtl`，避免逻辑过期缓存失去旧值兜底窗口。
- `LogicalExpireCacheTemplate.LogicalCachePayload` 收回为包内实现细节，不作为 public API 暴露。

### MyBatis

- `SafePageQuery` + `SortFieldMapping` 是对外推荐分页排序入口。
- `PageQuery` 直接接收数据库列名，已标记为不推荐 API，只适合内部可信代码，不建议暴露为 Web 入参。

### Starter

- `under.utils.*` 配置前缀保持稳定。
- Redis cache-aside 与 logical-cache 使用独立开关，共享 `CacheValueCodec`。
- GPG 签名、release profile 与 Testcontainers 集成测试已进入发布准备链路。

## 第二轮结论

### Spring Legacy AOP

- `OperationLog`、`Retry`、`TimeLog` 保留为兼容维护 API，并标记为 `@Deprecated`。
- `OperationLogAspect`、`RetryAspect`、`TimeLogAspect` 不再声明为 Spring `@Component`，避免直接扫描 `com.undernine.utils.spring` 时意外启用历史切面；仍需兼容时应显式 `@Import` 或声明为 `@Bean`。
- `OperationLog.recordParams` 默认值改为 `false`，降低误记录请求参数和敏感信息的风险；兼容旧行为需要显式设置 `recordParams = true`。
- `RetryAspect` 仍保留同步 sleep 语义，但会规整非法 `maxAttempts`、`delay` 和空异常列表，避免配置异常造成不可预期循环或等待。
- 新项目的审计、重试和耗时观测不再以轻量历史切面为主线，应优先使用业务统一审计、专用客户端治理、Micrometer 或 OpenTelemetry。

## 第三轮结论

### Core Historical Helpers

- `under-utils-core` 不再按“基础工具大全”表达，README 与包级 Javadoc 改为强调低耦合基础能力和历史工具兼容维护。
- `StringUtils`、`CollectionUtils`、`LocalDateTimeUtils`、`ValidationUtils`、`UUIDUtils`、`JsonUtils`、`MD5Utils`、`SHA256Utils`、`AESUtils` 标记为 `@Deprecated` 兼容 API，保留调用但不再扩展。
- `IdGenerator` 与 `MoneyUtils` 暂作为 core 主线保留能力：前者承担本地趋势递增 ID 生成，后者固化 BigDecimal 金额计算与分/元转换语义。
- `JsonUtils` 明确为历史兼容入口；复杂应用应注入业务自己的 `ObjectMapper` 或 codec，`getObjectMapper()` 标记为不推荐。
- `AESUtils.encryptECB` 与 `AESUtils.decryptECB` 标记为不推荐，ECB 仅保留历史兼容；MD5/SHA-256/AES 文档明确不作为推荐安全治理入口。
- 移除 `under-utils-core` POM 中未使用的 Lombok、SLF4J、Apache Commons Lang、Guava 和 Bouncy Castle 可选依赖，避免模块边界和开源定位被误读。

## 第四轮结论

### Spring Rate Limit And Repeat Submit

- 明确 `@RateLimit` 失败语义：超过窗口额度时抛出 `BizException`，消息来自 `message`；`limit <= 0` 等价于拒绝所有请求，`period <= 0` 按 1 秒窗口处理。
- 明确 `@PreventRepeat` 失败语义：同一 key 在窗口内重复提交时抛出 `BizException`，消息来自 `message`；`timeout <= 0` 按最小 1ms 处理。
- 明确 `releaseOnFailure` 只影响业务方法抛异常后的 key 释放；方法成功后 key 保持到窗口过期。
- 明确 key 解析语义：空 `key` 使用租户、用户、URI、方法名和参数摘要；SpEL 解析失败不会中断业务，会退回到表达式和参数摘要生成的兜底 key。
- 明确集群语义：local store 只在当前 JVM 内生效；多实例部署必须切换 Redis store 或自定义 `RateLimitStore` / `RepeatSubmitStore`。
- 明确 Redis 失败语义：Redisson 调用异常向外传播，如需 Redis 故障时放行或降级，应由业务自定义 store。

## 第五轮结论

### Maven Central Release

- 接入 Central Publisher Portal 的 Maven 发布链路，新增 `central-publish` profile；默认 `central.skipPublishing=true`，用于本地验证 deploy 生命周期，避免误上传。
- 发布插件固定为 `org.sonatype.central:central-publishing-maven-plugin:0.10.0`，不再引入旧 OSSRH / nexus-staging 发布路径。
- `central-publish` 通过 `excludeArtifacts` 排除 `under-utils-samples`，示例工程继续只参与构建和测试验证。
- 新增 `docs/RELEASE.md`，明确 namespace、Central Portal token、GPG/PGP 签名、手动校验发布和 CI secrets 要求。
- 新增手动触发的 GitHub Actions 发布工作流，默认上传后等待 `validated`，是否自动发布需要人工选择 `published` 模式。

## 第六轮结论

### Maven Coordinates

- Maven `groupId` 从 `com.undernineplaces` 收敛为 GitHub namespace `io.github.yexianglun-d`，避免依赖未持有域名的 DNS namespace 验证。
- Java 包名仍保持 `com.undernine.utils`，本轮只调整 Maven 坐标，不引入包名级破坏性迁移。

## 第七轮结论

### 1.0.0 Release

- `v1.0.0` 已发布到 Maven Central，发布源码 tag 固定在实际上传构件对应提交。
- `main` 分支进入 `1.0.1-SNAPSHOT` 开发周期，避免重复发布 Maven Central 已存在的 `1.0.0` 坐标。
- 发布后文档改为面向 Maven Central 使用，不再要求业务项目先本地 `mvn clean install`。

## 第八轮结论

### 文档与发布后测试覆盖

- README、快速开始、贡献指南、发布指南、路线图和模块 README 改为更接近社区维护文档的写法：少形容词，优先说明边界、安装、运行命令、失败语义和维护约束。
- 为 `under-utils-redis`、`under-utils-starter` 和 `under-utils-biz` 补齐模块 README，避免发布到 Maven Central 后缺少模块级入口说明。
- `under-utils-starter` 自动装配测试覆盖 Redis store 切换、缺少 `RedissonClient` 的失败路径，以及用户自定义 store、lock template、cache options/template 的退让行为。
- `LogicalExpireCacheOptions` 自动配置增加对用户自定义 `LogicalExpireCacheTemplate` 的退让，避免自定义模板场景下继续创建无用默认 options。
- `under-utils-test` 增加 Redis Testcontainers 集成测试，覆盖 cache-aside 命中复用、空值占位缓存和逻辑过期缓存后台刷新。

## 第九轮结论

### 兼容性策略

- 新增 `docs/COMPATIBILITY.md`，明确 `1.0.x`、`1.x.0` 和 `2.0.0` 的版本语义，patch/minor 版本默认保持源码兼容。
- 明确 public API 范围：Maven 坐标、public/protected Java API、注解属性、`under.utils.*` 配置 key、starter 自动装配 Bean、SPI 接口和已文档化的失败语义。
- 明确破坏性变更定义：删除/重命名 public API、修改签名或默认值、改变 starter 默认副作用、无迁移改变缓存 payload、改变 key/锁/缓存/错误解码语义等。
- 明确弃用流程：使用 `@Deprecated(since = "x.y.z", forRemoval = false)`，JavaDoc 说明替代方案，默认保留到下一 major 版本，并在 CHANGELOG/API Review 中记录。
- 将兼容性影响接入 README、贡献指南、PR 模板和功能建议模板，后续面向用户的变更需要显式标注 patch-compatible、minor-compatible、deprecation 或 breaking。

## 第十轮结论

### 文档语言

- 对外文档统一使用中文，保留 GitHub 开源项目常见的信息结构：边界、安装、配置、示例、兼容性、测试和贡献规则。
- 保留必要英文技术名词和代码标识，例如 Maven Central、public API、starter、SPI、Release Notes。
- Issue/PR 模板同步改为中文，降低中文贡献者提交成本。

## 第十一轮结论

### 1.0.1 增强 API

- `under-utils-biz` 新增 `AsyncImportTaskTemplate`、`ImportProgress`、`ImportProgressListener`、`ImportTaskStatus` 和 `ImportErrorExporter`。这些 API 只新增能力，不改变同步 `ImportTaskTemplate` 的既有返回结果和失败语义。
- `ImportOptions` 增加 `progressListener` 和 `toBuilder()`，默认 listener 为 no-op；listener 运行时异常会被记录并忽略，不影响导入主流程。
- `AsyncImportTaskTemplate` 默认使用 JVM 内存保存任务状态。跨实例查询、重启恢复和任务审计不进入默认实现，应由业务项目通过 `ImportProgressListener` 持久化。
- `under-utils-redis` 新增缓存观测 SPI：`CacheOperationObserver`、`CacheOperationEvent` 和 `CacheOperationType`。模板构造器新增 observer 重载，旧构造器继续可用并使用 no-op observer。
- starter 在存在 `CacheOperationObserver` Bean 时自动注入 cache-aside 与 logical-cache 模板，不新增默认 Bean，避免污染应用上下文。
- `under-utils-http` 新增 `RefreshingAccessTokenProvider`。它只负责当前 JVM 内 token 缓存、提前刷新和并发收敛；分布式 token 共享和刷新失败兜底仍属于业务实现边界。

## 第十二轮结论

### 1.0.1 Release Prep

- Maven reactor 版本从 `1.0.1-SNAPSHOT` 收敛为 `1.0.1`，用于 patch release 验证与 Central Portal 上传。
- 用户文档依赖版本同步更新为 `1.0.1`。
- `CHANGELOG.md` 将当前 Unreleased 内容归档到 `1.0.1`，并新增 `docs/releases/v1.0.1.md` 作为 GitHub Release Notes 草稿。

### 1.0.1 Release

- `v1.0.1` 已发布到 Maven Central，发布源码 tag 固定在实际上传构件对应提交。
- `main` 分支进入 `1.0.2-SNAPSHOT` 开发周期，避免重复发布 Maven Central 已存在的 `1.0.1` 坐标。

## 第十三轮结论

### Starter 轻量化

- 新增 `under-utils-spring-starter`，只提供请求上下文、限流、防重复提交、本地 store 和基础 SPI 自动装配，避免只接入 Spring 横切能力的项目被动引入 Redis/Redisson。
- 新增 `under-utils-redis-starter`，依赖 Spring starter 并承载 Redis store、分布式锁、缓存模板和缓存观测自动装配。
- `under-utils-starter` 保留为兼容聚合坐标，不删除旧 Maven artifact；旧用户可以继续使用，新用户按需选择轻量 starter。
- `under.utils.*` 配置 key 保持不变，本轮只调整自动装配模块边界，不改变现有默认行为和失败语义。

## 第十四轮结论

### API 兼容性门禁

- 新增 `api-compat` Maven profile，并接入 GitHub Actions CI，使用 japicmp 将当前构件和 `1.0.1` 已发布构件做 public API 对比。
- 默认纳入检查的模块为 `under-utils-core`、`under-utils-http`、`under-utils-spring`、`under-utils-redis`、`under-utils-mybatis` 和 `under-utils-biz`。
- 检查在 `verify` 阶段执行，并对二进制不兼容和源码不兼容修改失败退出。
- `under-utils-starter`、`under-utils-spring-starter` 和 `under-utils-redis-starter` 暂不纳入单 jar 对比。starter 拆分会产生类移动误报，兼容性先由旧聚合坐标、自动装配测试和配置 key 文档共同保证。
- 本地 Maven 如果配置了无法解析 japicmp 的镜像，可以使用 `docs/central-dry-run-settings.xml` 绕过全局镜像后再运行检查。

## 第十五轮结论

### Core JSON 解耦

- `JsonUtils` 继续留在 `under-utils-core`，只作为 `1.x` 兼容维护 API，不在 patch 版本移出 Jackson 依赖，避免老用户升级后缺少 transitive dependency。
- `under-utils-http`、`under-utils-spring` 和 `under-utils-redis` 内部不再调用 `JsonUtils`，改为模块内自有 Jackson mapper；异常仍沿用既有 unchecked 类型，避免改变用户可感知失败路径。
- 受影响模块显式声明 `jackson-datatype-jsr310`，确保脱离 `JsonUtils` 后仍支持 Java Time 类型。
- 本轮不是删除 core JSON，而是先降低运行时模块对历史工具入口的耦合。真正从 `under-utils-core` 移除 Jackson 需要等待 major 版本或提供明确迁移模块。

## 第十六轮结论

### Biz 依赖收敛

- `under-utils-biz` 当前 public API 只覆盖导入任务、CSV reader、进度查询和错误导出，不提供 Excel/POI/Jackson 类型签名。
- 移除 `under-utils-biz` POM 中未使用的 EasyExcel、POI 和 Jackson optional 依赖，不影响已发布 Java API。
- 后续如果提供 Excel 流式导入，应进入独立扩展模块，避免基础 biz 模块重新带入 Excel 栈。

## 第十七轮结论

### HTTP 客户端边界

- `under-utils-http` 当前 public API 基于 `HttpRequest`、`HttpResponse`、`HttpUtils`、`OkHttpRequestExecutor` 和 `OpenApiClient` 系列类型，不提供 HttpClient5 适配器。
- 移除 POM 中未实现的 HttpClient5 optional 依赖，不影响已发布 Java API，也避免用户误以为模块内置 HttpClient5 执行器。
- 后续如果需要多客户端实现，应新增明确的执行器抽象或独立适配模块，不能只通过 optional 依赖暗示能力。

## 第十八轮结论

### Runtime Boundary Fixes

- `RedisRateLimitStore` 不再忽略已存在 limiter 的配置漂移：`trySetRate` 失败后会读取当前配置，参数不一致时调用 `setRate` 更新，并改为只在 key 未设置 TTL 时补过期时间。
- `IdGenerator` 默认构造器不再固定使用 `datacenterId=0, workerId=0`；默认值优先来自系统属性或环境变量，未配置时按主机和进程派生。生产多节点仍应显式分配稳定节点 ID。
- `LocalRateLimitStore` 和 `LocalRepeatSubmitStore` 增加默认容量上限和节流过期清理，避免按用户、租户或业务 key 无限增长；`LocalRateLimitStore` 内部计数改为 `synchronized` 保护的普通 `int`。
- `AsyncImportTaskTemplate` 增加完成/失败任务状态保留期，默认 24 小时，并支持构造器自定义，避免结果和错误明细永久驻留当前 JVM。
- `SafePageQuery` 限制单次请求最多 5 个排序字段，排序白名单继续负责列名安全，数量上限负责防滥用。
- `DefaultOpenApiClient` 保持同步 API，但默认不再在重试前 `Thread.sleep`；需要同步等待的兼容调用方必须显式开启 `blockingRetryDelayEnabled`。
- Maven 坐标继续使用 `io.github.yexianglun-d`，Java 包名在 `1.x` 内保持 `com.undernine.utils`，避免包名级破坏性迁移；文档中明确这一兼容取舍。
- `AESUtils` 类和 CBC/ECB 方法均表达为历史兼容 API，不再给 CBC 方法保留“推荐”信号。

## 第十九轮结论

### Redis Cache Metrics

- `CacheAsideTemplate` 和 `LogicalExpireCacheTemplate` 内置 `CountingCacheOperationObserver`，调用方可以通过 `getMetrics()` 零配置读取缓存命中、未命中、加载、写入、重建锁和后台刷新计数。
- 新增 `CacheMetrics` 作为只读快照对象，派生 `lookupCount`、`hitRate`、`missRate`、`loadCount` 和 `errorCount`，避免业务侧重复聚合最常见的缓存指标。
- 保留 `CacheOperationObserver` SPI。外部 observer 继续接收所有事件，模板内置指标与外部 observer 相互独立；observer 抛出的运行时异常仍不会影响缓存主流程。
- starter 不新增默认 `CacheOperationObserver` Bean，避免污染应用上下文。Spring 应用可以直接从已装配的缓存模板 Bean 读取指标。

## 第二十轮结论

### Local State Active Cleanup

- `LocalRateLimitStore`、`LocalRepeatSubmitStore` 和 `AsyncImportTaskTemplate` 增加每实例后台清理线程，过期本地状态不再只依赖下一次访问或容量触发时被动清理。
- 三个类型均实现 `AutoCloseable`。后台清理线程为 daemon 线程，Spring 管理的 Bean 会在上下文销毁时关闭；手动创建的实例应在不再使用时调用 `close()`。
- 本轮不引入全局静态清理线程池，避免多个应用上下文、测试上下文或手动实例共享不可控生命周期。
- `under-utils-spring-starter` 为本地限流和本地防重复提交增加 `local-max-entries` 与 `local-cleanup-interval` 配置，继续保持 Redis store 配置语义不变。
- `RateLimitAspect` 和 `PreventRepeatAspect` 的默认本地 store 改为懒加载，避免 starter 注入外部 store 前先创建一个无用的后台清理线程。

## 第二十一轮结论

### Regression And Changelog Traceability

- 贡献指南明确 Bug 修复必须补独立回归测试。有外部 issue 时，测试类或方法应能追溯到 issue 编号；没有 issue 时，使用 `Regression...Test` 或 `regression_...` 命名，并标明来源类型。
- PR 模板增加追溯来源字段，区分 issue、PR、review-doc、internal-review、user-report 或 none，避免 `CHANGELOG.md` 只留下功能描述而缺少原因。
- Bug issue 模板增加建议回归测试段落，release notes 模板增加可追溯性段落，后续 patch/minor 发布需要同步说明 Issue/PR、Review 来源和回归测试。

## 第二十二轮结论

### Fluent API Experience

- `HttpRequest` 增加 `get/post/put/delete/patch/head/options` 方法级快捷 builder，直接 HTTP 调用可以从请求方法开始链式声明，不再必须先写 `builder().url(...).method(...)`。
- `HttpRequest.Builder` 增加 `execute()` 和 `executeAsync()`，常见一次性请求可以由 builder 直接构建并执行；原有 `build().execute()` 路径继续可用。
- `HttpRequest` 增加 `toBuilder()`，复制修改请求时会拷贝 headers、params、files 和 formParams，避免修改派生请求时污染原请求。
- `HttpConfig` 和 `OpenApiClientOptions` 增加 `toBuilder()` 与 `Duration` 友好的链式方法。为保持 Lombok builder 原有 `int/long` setter 兼容，新增方法使用 `connectTimeoutDuration`、`retryIntervalDuration` 等不冲突命名。
- `CacheOptions`、`LogicalExpireCacheOptions` 和 `ImportOptions` 已具备 builder/toBuilder。
- `CacheAsideTemplate` 新增 `cache(key, type)` / `key(key, type)` 链式入口，可在单次调用上配置 TTL、空值缓存、jitter 和重建锁后直接 `getOrLoad(...)`。
- `LogicalExpireCacheTemplate` 新增 `cache(key, type)` / `key(key, type)` 链式入口，可在单次调用上配置 logical TTL、physical TTL、刷新 executor 和失败处理器。

## 第二十三轮结论

### AI Basic Client

- 新增 `under-utils-ai` 模块，第一阶段只提供 OpenAI-compatible 同步文本对话客户端，不进入 Agent、RAG、流式响应、工具调用或厂商私有完整参数封装。
- 对外入口为 `AiClient.builder()` 和 `AiClientOptions`。调用方配置 `baseUrl`、`apiKey`、`model`、`timeout` 等基础参数后，可通过 `chat(ChatRequest)` 或 `chat(String)` 调用模型。
- 请求/响应模型使用 `ChatRequest`、`ChatMessage`、`ChatResponse` 和 `TokenUsage`，不直接暴露 `under-utils-http` 的 `HttpRequest` / `HttpResponse`，降低后续替换底层执行器的 API 风险。
- 错误统一映射为 `AiException`，通过 `AiErrorType` 区分鉴权失败、限流、超时、客户端错误、服务端错误、网络错误和响应解析失败，并暴露 `statusCode`、`errorCode` 与 `retryable`。
- 安全边界明确：异常信息和 `toString()` 不输出 API key、Authorization header 或完整 prompt/模型回复。
- 新增独立 `under-utils-ai-starter`，配置前缀为 `under.utils.ai`，默认 `enabled=false`，避免应用只引入 starter 坐标后就自动访问外部模型服务。
- `under-utils-ai-starter` 在存在用户自定义 `AiClient` Bean 时退让；当前 `provider` 只支持 `openai-compatible`，不通过配置暗示未实现的厂商原生协议。
- AI 模块复用 `under-utils-http` 的 HTTP 执行能力，因此会带入 OkHttp/Jackson；AI starter 没有放入 `under-utils-starter` 聚合入口，避免普通 Spring/Redis 用户被动引入 AI 依赖。
- `1.0.3` 发布前，AI 与 AI Starter 已使用 `1.0.2` 已发布构件作为基线纳入 `japicmp` public API 兼容检查。

## 第二十四轮结论

### Redis Micrometer Observation

- 新增 `MicrometerCacheOperationObserver`，将 `CacheOperationObserver` 事件桥接为 Micrometer counter、duration timer 和 observation。
- 新 observer 只使用低基数 tag：`cache.type`、`cache.operation`、`cache.outcome`、`cache.null` 和 `exception`；不会把业务 key 或实际 cache key 写入 tag。
- `under-utils-redis` 和 `under-utils-redis-starter` 仅以 optional dependency 引入 Micrometer，不改变普通 Redis 用户的默认依赖面。
- `under-utils-redis-starter` 在存在 `MeterRegistry`、没有用户自定义 `CacheOperationObserver` 且缓存能力启用时，自动创建 `MicrometerCacheOperationObserver`；配置 `under.utils.redis.observation.enabled=false` 可关闭。
- 如果用户已经声明自己的 `CacheOperationObserver`，starter 继续退让，不会叠加第二个 observer。

## 第二十五轮结论

### AI Streaming and Provider Extension

- 新增 `StreamingAiClient`、`ChatStream` 和 `ChatStreamEvent`，流式文本对话与同步 `AiClient` 分离，避免把同步入口扩成复杂回调 API。
- `OpenAiCompatibleAiClient` 实现 OpenAI-compatible SSE 流式响应，请求体强制设置 `stream=true`，按 `data:` 分片产出增量事件。
- `ChatStream` 实现 `AutoCloseable`，调用方可以通过 try-with-resources 或 `close()` 主动取消底层请求；同一个 stream 只能消费一次。
- 新增 `AiResponseMetadata`，同步 `ChatResponse` 和流式事件均可暴露 provider、调用方 request id、模型服务 response id、模型指纹和耗时。
- `ChatResponse#getRequestId()` 继续保留兼容语义；新增 `getResponseId()`、`getModelFingerprint()`、`getDuration()` 和 `getMetadata()` 用于更清晰地区分元数据。
- 新增 `AiClientProvider` 扩展点和 `OpenAiCompatibleAiClientProvider`，业务侧可接入非兼容协议，项目本身不引入具体厂商 SDK。
- `under-utils-ai-starter` 支持根据 `under.utils.ai.provider` 匹配自定义 `AiClientProvider` Bean；用户自定义 `AiClient` Bean 时继续退让。
- 相关测试通过 MockWebServer 覆盖流式正常分片、HTTP 错误、连接中断、主动取消、provider 扩展和 starter provider 路由。

## 第二十六轮结论

### AI Named Client Registry

- 新增 `AiClientRegistry` 和 `DefaultAiClientRegistry`，只做命名客户端管理和默认客户端选择，不引入模型自动路由、负载均衡或健康检查。
- `AiClientRegistry` 暴露 `getDefaultClient()`、`get(name)`、`find(name)`、`names()`、`getStreaming(name)` 和 `getDefaultStreaming()`，同步与流式边界继续分离。
- `under-utils-ai-starter` 支持 `under.utils.ai.clients.<name>.*` 多客户端配置，并通过 `under.utils.ai.default-client` 指定默认客户端。
- 未配置 `clients` 时，starter 继续按旧的顶层 `provider/base-url/api-key/model` 配置创建名为 `default` 的客户端，保持已有单客户端配置兼容。
- 顶层配置可作为命名客户端默认值，单个客户端可覆盖 provider、base URL、API key、model、chat completions path、timeout、retry、temperature、max tokens 和 headers。
- 默认客户端仍作为 `AiClient` Bean 暴露，业务代码只需要单模型时无需感知 registry；用户自定义 `AiClient` Bean 时自动装配继续退让。
- 相关测试覆盖 registry 行为、多客户端自动装配、默认客户端选择、headers 继承、自定义 provider 和缺失/不支持流式客户端的失败语义。

## 第二十七轮结论

### Enterprise Pain Point APIs

- 新增 `under-utils-mybatis-starter`，只自动装配 `MybatisPlusInterceptor` 和 `DefaultMetaObjectHandler`，配置前缀为 `under.utils.mybatis`。starter 不进入旧聚合 `under-utils-starter`，避免已有 Spring/Redis 用户被动引入 MyBatis。
- `under-utils-mybatis-starter` 所有 Bean 均使用 `@ConditionalOnMissingBean` 退让；多数据源项目仍应由业务自行管理每个 `SqlSessionFactory` 的 interceptor。
- 新增服务层 `@Idempotent`，与 HTTP 入口层 `@PreventRepeat` 分离。`@Idempotent` 面向 MQ 重试、RPC 重试和跨服务回调；执行中重复调用抛出 `IdempotentInProgressException`，完成后重复调用返回第一次结果。
- 新增 `IdempotencyStore`、`LocalIdempotencyStore`、`RedisIdempotencyStore` 和 `IdempotencyResultCodec`。本地 store 只保护当前 JVM；多实例部署应使用 Redis store 或自定义 store。失败结果默认不缓存，业务方法抛异常时默认按执行 owner token 释放 key。
- `IdempotencyStore` 的完成/释放语义带执行 owner token；旧执行在 processing TTL 过期后不能覆盖或释放新执行。业务方法已经成功但完成态写入失败时，不释放 key，避免重复执行业务。
- `@Idempotent` 显式 SpEL key 解析失败会抛出 `IdempotentKeyResolveException`，不使用兜底 key，避免错误 key 造成业务重复或误挡。
- 新增 `under.utils.idempotent.*` 配置，支持 `enabled`、`store`、`key-prefix`、`processing-ttl`、`result-ttl`、`local-max-entries` 和 `local-cleanup-interval`。
- 新增 `under-utils-security` 和 `under-utils-security-starter`。字段级加密只提供 AES-GCM、key provider 和显式 MyBatis TypeHandler；不使用历史 `AESUtils`，不做全局隐式字段加密。
- `EncryptedStringTypeHandler` 需要实体字段显式声明 `@TableField(typeHandler = EncryptedStringTypeHandler.class)`，并要求 MyBatis-Plus 实体启用 `@TableName(autoResultMap = true)`。
- 新增 `@Mask`、`MaskType` 和 `MaskingJsonSerializer` 作为 security 模块的响应脱敏 API；`under-utils-spring` 中已有 `@Sensitive` 继续保留兼容，不迁移包名。
- 新增 `under.utils.security.*` 配置。未配置字段加密 key 或显式设置 `under.utils.security.field-encryption.enabled=false` 时，security starter 不创建默认 `FieldEncryptor`，也不注册 MyBatis TypeHandler 默认加密器。

## 第二十八轮结论

### 1.0.5 Idempotency JDBC And Observation

- 新增 `under-utils-jdbc` 和 `under-utils-jdbc-starter`，作为 minor-compatible 新模块。它们不进入旧聚合 `under-utils-starter`，避免 Spring/Redis 老用户被动引入 JDBC 能力。
- `JdbcIdempotencyStore` 实现既有 `IdempotencyStore` SPI，不修改 `@Idempotent`、`LocalIdempotencyStore` 或 `RedisIdempotencyStore` 的默认语义。
- `under-utils-jdbc-starter` 只有在 `under.utils.idempotent.store=jdbc` 且存在 `JdbcOperations` 时装配；用户自定义 `IdempotencyStore` 继续优先，不会在未选择 JDBC store 时被动启动 JDBC 清理任务。
- 新增配置 key：`under.utils.idempotent.jdbc.table-name`、`under.utils.idempotent.jdbc.max-begin-retries`、`under.utils.idempotent.jdbc.cleanup-enabled`、`under.utils.idempotent.jdbc.cleanup-initial-delay` 和 `under.utils.idempotent.jdbc.cleanup-interval`。JDBC starter 不自动建表，不接管数据源、事务或迁移工具。
- `under-utils-jdbc` 随包提供 MySQL/PostgreSQL 建表脚本，包含 `expire_at` 索引；`JdbcIdempotencyCleanupScheduler` 会在 JDBC store 启用时默认定期调用 `cleanupExpired()` 删除过期记录。
- `JdbcIdempotencyStoreOptions` 对表名做白名单校验，只允许未加引号的 `table` 或 `schema.table`；业务 key、执行 token 和结果 payload 仍全部使用参数绑定。
- 新增 `IdempotencyObserver`、`IdempotencyEvent`、`IdempotencyOperation`、`IdempotencyOutcome` 和 `MicrometerIdempotencyObserver`，作为 additive public API。
- `MicrometerIdempotencyObserver` 只输出低基数 tag：`idempotency.operation`、`idempotency.outcome` 和 `exception`，不把幂等 key、方法签名或业务参数写入指标。
- 新增配置 key：`under.utils.idempotent.observation.enabled`，默认开启但只有存在 `MeterRegistry` 时才创建 observer；用户自定义 `IdempotencyObserver` 时自动退让。
- `IdempotentAspect` 在业务异常后释放 key 失败时保留原业务异常，并将释放失败作为 suppressed exception，属于符合既有失败语义的兼容行为修复。
