# 变更日志

本文件记录 Under-Utils 的重要变更。

## [Unreleased]

### Changed

- `main` 分支 Maven 版本进入 `1.0.5-SNAPSHOT` 开发周期，`1.0.4` 保持为当前稳定版本和 API 兼容性基线。

## [1.0.4] - 2026-06-05

### 企业级能力

- 新增 `under-utils-mybatis-starter`，按 `under.utils.mybatis.*` 自动装配 MyBatis-Plus interceptor 和默认审计字段填充处理器，并在用户已有同类型 Bean 时退让；该 starter 不进入旧聚合 `under-utils-starter`。
- 新增服务层业务幂等能力：`@Idempotent`、`IdempotentAspect`、`IdempotencyStore`、`LocalIdempotencyStore`、`IdempotencyResultCodec` 和 Redis `RedisIdempotencyStore`。同 key 执行中重复调用会抛出 `IdempotentInProgressException`，首次成功完成后的重复调用返回第一次结果。
- `under-utils-spring-starter` 新增 `under.utils.idempotent.*` 配置，支持 local/redis store、key prefix、processing TTL、result TTL、本地容量和清理周期；`under-utils-redis-starter` 在存在 `RedissonClient` 且配置 `store=redis` 时装配 Redis 幂等 store。
- 新增 `under-utils-security` 和 `under-utils-security-starter`，提供 AES-GCM 字段级加密、MyBatis 显式 `EncryptedStringTypeHandler`、`@Mask` 响应脱敏和 `under.utils.security.*` 自动配置；security starter 不进入旧聚合 `under-utils-starter`。
- 服务层幂等 store 使用执行 owner token 完成和释放 key，避免 processing TTL 过期后旧执行覆盖新执行；业务成功但幂等完成态写入失败时不再释放 key。
- `under.utils.security.field-encryption.enabled=false` 会禁用默认字段加密器和 MyBatis TypeHandler 默认加密器注册；TypeHandler 默认加密器注册清理改为 owner token 保护。

### 兼容增强

- `OpenAiCompatibleAiClient` 的 SSE 解析支持同一个事件中的多行 `data:` 字段，忽略注释和非 data 字段，并在连接结束前处理未以空行结尾的最后一个事件。
- `DefaultOperationKeyResolver` 在参数无法被 Jackson 序列化时改用稳定摘要兜底，避免防重复提交和限流 key 因个别参数不可序列化而退化。
- Spring 可信代理 Header 开关相关构造器和状态读取方法为 additive public API，保留旧构造器兼容行为，不删除或修改既有签名。

### 安全边界

- Spring 请求上下文、请求日志和操作日志默认不再信任客户端传入的代理 IP Header；只有显式开启可信代理 Header 后才读取 `X-Forwarded-For` / `X-Real-IP`。
- 请求日志新增常见敏感 Header 脱敏，覆盖 Authorization、Cookie、API key、访问/刷新 token 和 CSRF token 等字段。
- 示例工程的 OpenAPI mock gateway 不再回显 Authorization 原文；兼容工具示例不再把 MD5/SHA-256 表达为生产密码存储方案，也不输出明文密码。

### 测试稳定性

- Spring context 相关测试在每个用例前清理 `OperationContextHolder`、`RequestContextHolder` 和 MDC，避免测试顺序导致 ThreadLocal 状态串扰。
- Testcontainers 版本纳入 BOM 管理并升级到 `1.21.4`，MySQL 集成测试镜像升级到 `mysql:8.4`，用于修复较新 Docker 环境下的集成测试兼容性。
- 只在刻意覆盖 deprecated 兼容 API 的测试/示例类上添加 class-level `@SuppressWarnings("deprecation")`，并限定 `RetryAspectTest` 的 unchecked 抑制范围；生产代码不新增警告抑制。

### 构建验证

- CI 的 `api-compat` 检查模块列表补齐 `under-utils-ai` 和 `under-utils-ai-starter`，继续以 `1.0.3` 作为 `1.0.4` 的 public API 基线。
- `docs/releases/v1.0.4.md` 收敛为正式发布说明，记录本地验证、CI 验证、API 兼容性检查和 Maven Central dry-run 验证边界。

### Changed

- 本轮 `1.0.4-SNAPSHOT` 开发内容收敛为 `1.0.4` 正式发布版本，`1.0.3` 继续作为 API 兼容性基线。

## [1.0.3] - 2026-06-04

### Added

- `under-utils-ai` 新增 `AiClientRegistry` 和 `DefaultAiClientRegistry`，支持在一个应用内按名称管理多个 AI client，并获取默认或指定客户端。
- `under-utils-ai-starter` 支持 `under.utils.ai.clients.<name>.*` 多客户端配置和 `under.utils.ai.default-client` 默认路由；旧的单客户端顶层配置继续兼容。
- `under-utils-samples` 的 AI profile 新增命名客户端示例和 `/samples/ai/clients/{clientName}/chat`、`/samples/ai/clients/{clientName}/chat/stream` 路由。
- `under-utils-ai` 的 `ChatRequest` 新增 OpenAI-compatible 原生消息、tools、tool_choice 和 response_format 参数入口，`ChatResponse` 新增原始 assistant 消息读取能力。
- `under-utils-ai` 和 `under-utils-ai-starter` 新增 `streamReadTimeout` / `under.utils.ai.stream-read-timeout`，单独控制 SSE 分片读取等待时间。
- `under-utils-mybatis` 新增 `AuditFillOptions`，支持自定义审计字段名、逻辑未删除值和关闭逻辑删除默认填充。

### Changed

- 本轮 `1.0.3-SNAPSHOT` 开发内容收敛为 `1.0.3` 正式发布版本，`1.0.2` 继续作为 API 兼容性基线。
- `LogicalExpireCacheOptions` 默认后台刷新执行器改为独立有界 daemon 线程池，不再使用 `ForkJoinPool.commonPool()`。
- `CsvImportRowReader` 默认忽略 UTF-8 BOM、限制单条记录最大字符数，并严格校验 CSV 引号语法；需要历史宽松行为时可显式设置 `strictQuotes(false)`。
- `docs/COMPATIBILITY.md` 增加 Java 21 / Spring Boot 3.1.x 运行环境矩阵和发布前验证要求。
- `api-compat` 默认基线更新为 `1.0.2`，并将 `under-utils-ai` 与 `under-utils-ai-starter` 纳入兼容性检查。
- `UnderUtilsAiAutoConfiguration` 恢复 `1.0.2` 已发布的默认 `AiClient` 工厂方法签名，避免 starter public API 破坏性变更。

## [1.0.2] - 2026-05-31

### Added

- 新增 `under-utils-spring-starter`，只自动装配 Spring 本地横切能力，不再强制引入 Redis/Redisson。
- 新增 `under-utils-redis-starter`，承载 Redis store、分布式锁、cache-aside 和逻辑过期缓存自动装配。
- 新增 `api-compat` Maven profile，并接入 CI，用于将稳定运行时模块的 public API 与 `1.0.1` 已发布构件做兼容性检查。
- 新增依赖重量审计文档，记录各模块默认传递依赖、optional 边界和后续拆分判断。
- 新增 `CacheMetrics` 和 `CountingCacheOperationObserver`，cache-aside 与逻辑过期缓存模板可零配置读取命中、未命中、加载、写入、锁和刷新计数。
- 新增工程成熟度推进文档和后续功能孵化文档，并在 README 提供入口。
- 贡献指南、PR 模板、Bug 模板和 release notes 模板增加回归测试命名、修复来源和 CHANGELOG 可追溯性要求。
- `HttpRequest` 增加方法级快捷 builder、builder 直接执行和 `toBuilder()` 复制修改；`HttpConfig`、`OpenApiClientOptions` 增加 `toBuilder()` 与 `Duration` 友好的链式配置方法。
- 新增 `under-utils-ai` 和 `under-utils-ai-starter`，提供 OpenAI-compatible 同步文本对话、SSE 流式响应、provider 扩展、Spring Boot 自动装配、基础错误分类、token 用量响应模型、响应元数据和敏感信息脱敏边界。
- `under-utils-samples` 新增 AI profile 示例，演示通过环境变量配置 `under-utils-ai-starter` 并调用同步文本对话和流式 SSE 接口。
- `under-utils-redis` 新增 `MicrometerCacheOperationObserver`，Redis starter 可在存在 `MeterRegistry` 时自动接入缓存事件 counter、duration timer 和 observation。
- `CacheAsideTemplate` 和 `LogicalExpireCacheTemplate` 新增链式单次调用入口，可通过 `template.cache(key, type).ttl(...).getOrLoad(...)` 简化缓存读取样板代码。
- 新增 `docs/CRYPTO_REDESIGN.md`、`docs/JSON_MODULE_MIGRATION.md` 和 `docs/releases/v1.0.2.md`，收口 crypto、core JSON 迁移和 `1.0.2` 发布准备边界。

### Changed

- 本轮 `1.0.2-SNAPSHOT` 开发内容收敛为 `1.0.2` 正式发布版本，`1.0.1` 继续作为 API 兼容性基线。
- `under-utils-starter` 调整为兼容聚合入口，继续保留旧坐标，但实际能力由 Spring/Redis 两个 starter 提供。
- `under-utils-http`、`under-utils-spring` 和 `under-utils-redis` 内部不再调用 `under-utils-core` 的历史 `JsonUtils`，JSON 行为保持不变，为后续 core JSON 迁移降低耦合。
- `under-utils-biz` 移除未使用的 EasyExcel、POI 和 Jackson optional 依赖，模块边界收敛为导入任务、进度查询和错误导出。
- `under-utils-http` 移除未实现的 HttpClient5 optional 依赖，模块边界收敛为 OkHttp 执行器和 OpenAPI 客户端治理。
- `RedisRateLimitStore` 在同 key 限流参数变化时会更新 Redisson limiter 配置，并避免每次请求无条件刷新 TTL。
- `IdGenerator` 默认构造器不再固定使用 0/0 节点 ID，改为优先读取配置，缺省时按主机和进程派生节点 ID。
- 本地限流、防重复提交和异步导入任务状态增加容量或保留期边界，避免内存状态无限增长。
- 本地限流、防重复提交和异步导入任务状态增加可关闭、可配置的主动清理线程；starter 本地 store 支持配置本地容量和清理间隔。
- `SafePageQuery` 限制单次请求最多 5 个排序字段，避免构造过宽 `ORDER BY`。
- `DefaultOpenApiClient` 默认不再在重试前阻塞当前线程等待 `retryInterval`；需要同步等待时必须显式开启。
- `AESUtils` 的 CBC 方法单独标记为 deprecated，并收敛文档中的推荐信号。

## [1.0.1] - 2026-05-23

### Changed

- 对外文档统一改回中文表达，保留 GitHub 开源项目常见结构和维护语气。
- 重写 README、快速开始、贡献指南、发布指南、路线图和模块 README，去除营销化表达，改为 GitHub 开源项目常见的边界、安装、示例和维护说明。
- 补充 `under-utils-redis`、`under-utils-starter`、`under-utils-biz` 模块 README。
- 发布后更新 README、快速开始、路线图和 API Review，改为面向 Maven Central 已发布版本的使用说明。
- `main` 分支 Maven 版本进入 `1.0.1-SNAPSHOT` 开发周期，避免重复发布已存在的 `1.0.0` 构件。
- `under-utils-starter` 在用户自定义 `LogicalExpireCacheTemplate` 时不再继续创建默认 `LogicalExpireCacheOptions`。
- README、贡献指南、Pull Request 模板和功能建议模板接入兼容性影响说明，要求面向用户的变更显式判断 patch/minor/deprecation/breaking 影响。

### Added

- `under-utils-biz` 新增异步导入任务模板、进度快照、进度监听器和行级错误 CSV 导出工具。
- `under-utils-redis` 新增 `CacheOperationObserver`、`CacheOperationEvent` 和 `CacheOperationType`，覆盖 cache-aside 与 logical-cache 的命中、未命中、加载、写入、重建锁和后台刷新观测事件。
- `under-utils-http` 新增 `RefreshingAccessTokenProvider`，支持 access token 本地缓存、提前刷新和并发刷新收敛。
- `under-utils-samples` 新增异步导入提交/查询/错误导出接口、OpenAPI token 刷新与业务错误解码示例，以及 `custom-store` profile 下的自定义 `RateLimitStore`、`RepeatSubmitStore`、`CacheValueCodec` 和 `CacheOperationObserver` 示例。
- `under-utils-mybatis` README 补充多数据源下 MyBatis-Plus interceptor 与审计填充配置示例。
- 新增 patch/minor 发布说明模板，统一后续 GitHub Release Notes 编写结构。
- 新增 `docs/COMPATIBILITY.md`，明确 1.x 版本兼容策略、public API 范围、破坏性变更定义、弃用流程和配置 key 迁移规则。
- `under-utils-starter` 自动装配测试补充 Redis store 切换、缺少 `RedissonClient` 的失败路径、用户自定义 store/lock/logical-cache Bean 退让行为。
- `under-utils-test` 新增 Redis Testcontainers 集成测试，覆盖 `CacheAsideTemplate` 命中复用、空值占位缓存和 `LogicalExpireCacheTemplate` 后台刷新。

## [1.0.0] - 2026-05-23

### Added

- 新增 GitHub Actions CI，覆盖默认编译、默认测试和 `under-utils-test` Testcontainers 集成测试。
- 新增 Maven 发布准备配置，包含开源元信息、release 构件 profile、sources/javadocs 生成和可选 GPG 签名 profile。
- 新增 Maven Central Portal 发布 profile、发布手册和手动 GitHub Actions 发布工作流。
- 新增发布前 API 审计记录 `docs/API_REVIEW.md`，跟踪 public API、配置 key 和模块边界收口结论。
- `under-utils-test` 新增 Testcontainers MySQL 集成测试依赖，集成验证不再依赖本地 MySQL。
- 新增 GitHub 开源社区文件：`LICENSE`、`CONTRIBUTING.md`、`SECURITY.md`、`CODE_OF_CONDUCT.md`、Issue 模板和 Pull Request 模板。
- 新增公开路线图 `ROADMAP.md`，替代内部阶段分析文档。
- 新增 Spring 操作上下文体系：`OperationContext`、`OperationContextFilter`、`OperationContextHolder`、`OperationContextSnapshot`、`OperationContextTaskDecorator`、`OperationContextExecutors`。
- 新增操作身份 SPI：`CurrentUserProvider`、`CurrentTenantProvider`、`TraceIdProvider`、`OperationContextCustomizer`。
- 新增操作 key 解析抽象：`OperationKeyResolver` 与默认实现，支持基于租户、用户、URI、方法参数摘要及 SpEL 生成限流/防重 key。
- 新增限流与防重复提交存储抽象：`RateLimitStore`、`RepeatSubmitStore`，并提供本地内存实现。
- 新增 Redis 工程模式能力：
  - `DistributedLockTemplate`
  - `RedisRateLimitStore`
  - `RedisRepeatSubmitStore`
  - `CacheAsideTemplate`
  - `LogicalExpireCacheTemplate`
- 新增 OpenAPI 客户端封装：
  - `OpenApiClient`
  - `DefaultOpenApiClient`
  - `OpenApiRequest`
  - `OpenApiResponse`
  - `OpenApiClientOptions`
  - `AccessTokenProvider`
  - `RequestSigner`
  - `ApiErrorDecoder`
- 新增 MyBatis-Plus 增强：
  - `SafePageQuery`
  - `SortFieldMapping`
  - `AuditorProvider`
  - `DefaultMetaObjectHandler` 构造注入审计用户能力
- 新增业务导入任务模板：
  - `ImportTaskTemplate`
  - `ImportRowHandler`
  - `ImportOptions`
  - `ImportResult`
  - `CsvImportRowReader`
  - `CsvRow`
- 新增 `under-utils-starter` 自动装配能力，覆盖 Web 横切、本地/Redis 状态存储、分布式锁、cache-aside、逻辑过期缓存等。
- 新增 `under-utils-samples` 可运行示例工程，覆盖上下文传播、限流防重、OpenAPI、本地导入、MyBatis 安全分页和 Redis 可选示例。
- 新增 samples Redis 验证环境：`docker-compose.yml` 与 `redis` profile 下的 `RedissonClient` 示例配置。

### Changed

- 重写根 README，明确项目定位：Under-Utils 不做 Hutool 替代品，不主打低复杂度通用工具方法，而主打复杂、重复、高工程价值的模式封装。
- 重写 `QUICK_START.md`，移除本地绝对路径、过时 JDK 版本和占位模块说明，改为 GitHub 开源上手流程。
- 更新 POM 描述，弱化“常用工具方法”表述，强调工程模式封装。
- Maven `groupId` 从 `com.undernineplaces` 收敛为 GitHub namespace `io.github.yexianglun-d`，降低 Maven Central namespace 验证成本。
- 父工程 Maven Compiler Plugin 开启 `parameters`，让 Spring 在干净编译后可直接读取方法参数名。
- `under-utils-samples` 标记为不参与 Maven deploy，保留构建验证但避免作为正式库模块发布。
- `central-publish` profile 默认跳过上传，用于本地验证 Central 发布生命周期；真实发布必须显式设置 `central.skipPublishing=false`。
- HTTP 模块底层 OkHttp 执行入口从 `OkHttpClient` 收敛为 `OkHttpRequestExecutor`，避免与三方库类型同名。
- `LogicalExpireCacheOptions` 增加 `physicalTtl > logicalTtl` 校验，确保逻辑过期缓存保留旧值兜底窗口。
- `LogicalExpireCacheTemplate.LogicalCachePayload` 收回为包内实现细节，不再作为 public API 暴露。
- `PageQuery` 标记为不推荐 API，前端可控排序场景应使用 `SafePageQuery` 与 `SortFieldMapping`。
- `OperationLog`、`Retry`、`TimeLog` 及对应历史切面标记为兼容维护 API，不再作为 Spring 模块新增能力主线。
- `OperationLog.recordParams` 默认值改为 `false`，需要记录请求参数时必须显式开启。
- `OperationLogAspect`、`RetryAspect`、`TimeLogAspect` 不再声明为 Spring `@Component`，兼容使用时需显式 `@Import` 或声明为 `@Bean`。
- `RetryAspect` 对非法 `maxAttempts`、负数 `delay` 和空异常类型进行防御处理，保留同步重试语义但降低错误配置风险。
- `under-utils-core` README 与包级 Javadoc 收敛为低耦合基础能力和历史工具兼容维护，不再按常用工具大全表达。
- `StringUtils`、`CollectionUtils`、`LocalDateTimeUtils`、`ValidationUtils`、`UUIDUtils`、`JsonUtils`、`MD5Utils`、`SHA256Utils`、`AESUtils` 标记为兼容维护 API，不再扩展低复杂度工具方法。
- `JsonUtils.getObjectMapper()` 标记为不推荐，避免外部修改共享 ObjectMapper 影响全局序列化行为。
- `AESUtils.encryptECB` 与 `AESUtils.decryptECB` 标记为不推荐，ECB 模式仅保留历史兼容。
- 移除 `under-utils-core` 中未使用的 Lombok、SLF4J、Apache Commons Lang、Guava 和 Bouncy Castle 可选依赖。
- 补充 `@RateLimit`、`@PreventRepeat`、`RateLimitStore`、`RepeatSubmitStore` 和 starter store 配置的失败语义、key 解析语义和集群环境说明。
- 收紧 starter 自动装配条件，用户自定义 `TaskDecorator`、`CacheValueCodec`、`CacheOptions`、`CacheAsideTemplate` 等 Bean 时自动退让。
- `CacheValueCodec` 改为 cache-aside 与 logical-cache 共享的 Redis 缓存基础设施，仅在相关能力启用时自动装配。
- Redis cache options 增加兼容别名，改善 `ttl/nullTtl/cacheNull` 与 value/null value 语义的可读性。
- 为 OpenAPI、Redis cache、Spring context、Biz import task、MyBatis page 等包补充边界说明和关键 Javadoc。
- 父工程增加 `spring-boot-maven-plugin` 版本管理，支持从根工程运行 samples 模块。
- `under-utils-test` 改为 Testcontainers 集成验证模块，通过 `integration-tests` profile 启用，不再进入默认构建链路。
- MyBatis 集成测试改为动态注入临时 MySQL 容器 datasource，并在每个测试前清理测试表。
- `CoreUtilsIntegrationTest` 移除不必要的 Spring 上下文启动。

### Removed

- 删除无实际职责的 `under-utils-placeholder` 占位模块。
- 删除各模块残留的空 `Placeholder.java`。
- 删除过时的内部分析、开发进度、测试报告和设计草稿文档。
- 删除 `under-utils-biz` 中的占位类，改由导入任务模板承载真实业务流程封装能力。

### Verified

- `mvn -pl under-utils-samples -am test`
- `mvn -DskipTests compile`
- `mvn clean test`
- `mvn -pl under-utils-core test`
- `mvn -pl under-utils-spring -am test`
- `mvn -pl under-utils-http,under-utils-redis,under-utils-mybatis,under-utils-starter -am test`
- `mvn -pl under-utils-spring,under-utils-redis,under-utils-starter -am test`
- `mvn test`
- `mvn -Prelease -DskipTests package`
- `mvn -Prelease,sign-artifacts -Dgpg.skip=true -DskipTests verify`
- `mvn -s docs/central-dry-run-settings.xml -Prelease,central-publish -Dcentral.publishing.server.id=central-dry-run -Dcentral.skipPublishing=true -Dgpg.skip=true -DskipTests deploy`
- `mvn -Pintegration-tests -pl under-utils-test -am -DskipTests test-compile`
- `git diff --check`
- samples 已在无 Redis/MySQL 的默认配置下完成 Spring Boot 启动与核心接口验证。

### Known Limitations

- 当前机器无 `docker` 命令，Redis samples 和 Testcontainers 集成测试无法在本机执行；相关验证需要在具备 Docker 的环境或 GitHub Actions 中运行。
