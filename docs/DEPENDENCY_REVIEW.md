# 依赖重量审计

本文件记录 Under-Utils 的模块依赖重量、默认传递依赖和后续拆分判断。

审计目标不是追求最小 jar，而是避免用户为了一个窄能力被迫引入无关框架栈。

模块选择和组合建议见 [MODULE_SELECTION.md](MODULE_SELECTION.md)。本文件侧重依赖重量和模块边界判断。

## 当前结论

- 项目自身 jar 都很小，主要重量来自传递依赖。
- `1.0.6` 的治理重点从“还能加什么模块”转向“用户如何选择最少入口”，优先降低认知重量。
- starter 拆分后，Spring-only 用户不再被 `under-utils-starter` 强制带入 Redis/Redisson。
- `under-utils-starter` 继续保留为兼容聚合入口，但不再接收 AI、Security、MyBatis、JDBC 等新增能力。
- `under-utils-core` 仍会带入 Jackson。`JsonUtils` 已经发布为 public API，`1.x` 内不应直接移除这组依赖。
- `under-utils-redis` 的真正重量来自 Redisson/Netty；这是模块能力本身决定的，不适合在 patch 版本里伪装成轻量模块。
- `under-utils-redis` 和 `under-utils-redis-starter` 新增 Micrometer 可选依赖，只用于 `MicrometerCacheOperationObserver`，不会通过普通依赖路径强制引入到用户应用。
- `under-utils-biz` 当前实现只使用无外部依赖的 CSV/导入模板，已移除未使用的 Excel/POI/Jackson optional 依赖。
- `under-utils-http` 已移除未实现的 HttpClient5 optional 依赖，当前对外边界明确为 OkHttp 执行器和 OpenAPI 客户端治理。
- `under-utils-ai` 作为独立模块复用 `under-utils-http`；`under-utils-ai-starter` 也保持独立，不会通过聚合 starter 让普通 Spring/Redis 用户被动引入 AI 依赖。
- `under-utils-mybatis-starter` 保持独立，不进入 `under-utils-starter` 聚合入口，避免普通 Spring/Redis 用户被动引入 MyBatis-Plus。
- `under-utils-security` 的 MyBatis TypeHandler 依赖保持 optional；`under-utils-security-starter` 为了注册 TypeHandler 默认加密器显式依赖 MyBatis-Plus core，但同样不进入聚合 starter。
- `under-utils-jdbc` 依赖 Spring JDBC 和 `under-utils-spring` 的幂等 SPI；`under-utils-jdbc-starter` 保持独立，不进入聚合 starter，避免普通 Spring/Redis 用户被动引入 JDBC。
- 编译期注解处理器已从 starter 普通依赖中移出，统一通过父 POM 的 `annotationProcessorPaths` 显式声明 Lombok 和 Spring Boot configuration processor，减少 runtime 依赖树噪音和 javac 自动探测提示。

## 模块快照

数据基于 `1.0.5-SNAPSHOT` 本地 `mvn -B -ntp -Prelease -DskipTests package` 构建产物和 `runtime` 依赖树。

统计口径：

- 主 jar 大小使用各模块 `target/*-1.0.5-SNAPSHOT.jar`。
- runtime 树规模使用 `target/dependency-tree-runtime.txt` 行数，包含根坐标行。
- Maven dependency tree 会列出本模块声明的 optional 依赖；optional 不会作为普通传递依赖强制带给下游消费者。

| 模块 | 主 jar | runtime 树规模 | 主要默认依赖 | 判断 |
|------|--------|----------------|--------------|------|
| `under-utils-core` | 40K | 5 行 | Jackson databind/core/annotations/jsr310 | `1.x` 保留；`2.0.0` 再考虑 JSON 迁移。 |
| `under-utils-http` | 72K | 16 行 | core、SLF4J、OkHttp/Okio/Kotlin、Jackson | 默认 OkHttp 会带 Kotlin runtime；不再声明未实现的 HttpClient5 适配。 |
| `under-utils-ai` | 52K | 16 行 | http、core、OkHttp/Okio、Jackson、SLF4J | 独立 AI 能力模块，不放入 starter 聚合入口，避免扩大默认依赖面。 |
| `under-utils-ai-starter` | 12K | 18 行 | ai module、Boot autoconfigure、Spring context | 独立 AI starter，不被兼容聚合 starter 引入。 |
| `under-utils-security` | 20K | 9 行 | Jackson；MyBatis-Plus core optional | 独立安全能力模块，字段加密核心不强制业务使用 starter。 |
| `under-utils-security-starter` | 12K | 16 行 | security module、Boot autoconfigure、MyBatis-Plus core | 独立 security starter，不进入聚合入口。 |
| `under-utils-spring` | 108K | 23 行 | core、Spring context/web/webmvc、AspectJ、Validation、Jackson | Spring MVC/AOP 模块，重量和定位一致；Lombok 仅为编译期 optional。 |
| `under-utils-jdbc` | 12K | 22 行 | spring module、Spring JDBC | 独立 JDBC 幂等 store，不进入默认 starter。 |
| `under-utils-redis` | 56K | 54 行 | core、spring、Redisson/Netty、Jackson；Micrometer optional | Redisson 是主要重量；Micrometer 只服务可选观测适配。 |
| `under-utils-mybatis` | 28K | 13 行 | core、MyBatis-Plus、JSQLParser | 依赖和安全分页/审计能力匹配；Lombok 仅为编译期 optional。 |
| `under-utils-mybatis-starter` | 12K | 19 行 | mybatis module、Boot autoconfigure | 独立 MyBatis starter，不进入聚合入口。 |
| `under-utils-biz` | 44K | 8 行 | core、SLF4J；Lombok optional | 当前代码未使用 Excel/POI，基础 biz 模块保持无 Excel 栈。 |
| `under-utils-spring-starter` | 24K | 24 行 | spring module、Boot autoconfigure、Servlet API；Micrometer optional | 符合 Spring-only starter 定位；metadata processor 不再出现在 runtime 树中。 |
| `under-utils-jdbc-starter` | 12K | 24 行 | spring starter、jdbc module、Boot autoconfigure | 独立 JDBC starter，只在显式 `store=jdbc` 时接入。 |
| `under-utils-redis-starter` | 12K | 48 行 | spring starter、redis module、Boot autoconfigure；Micrometer optional | 符合 Redis starter 定位，主要重量来自 Redisson 链路。 |
| `under-utils-starter` | 4.0K | 50 行 | spring starter、redis starter | 兼容聚合入口，保持旧用户路径；新能力不再加入。 |

## 1.0.6 轻量化治理口径

`1.0.6` 不以新增大模块为目标，而是先把现有模块的选择边界讲清楚。

| 决策点 | 结论 |
|--------|------|
| 新项目默认入口 | 优先 `under-utils-spring-starter`，按需叠加独立 starter。 |
| 旧聚合 starter | 只保留兼容用途，不再加入新企业能力。 |
| Redis 与 JDBC 幂等 | 二选一配置默认 store，不在一个应用里同时声明两个默认幂等 store。 |
| starter 与 library | Spring Boot 自动装配选 starter，手动接线或非 Boot 场景选 library。 |
| 自动建表/建连接 | starter 不创建数据库表、Redis 连接、数据源、事务或 mapper 扫描。 |
| 新 public API | 非必要不新增；优先通过文档、测试和配置边界降低使用复杂度。 |

当前已补齐 `1.0.5-SNAPSHOT` 的 jar 大小和 runtime 依赖树。发版前如果 POM 或模块边界继续变化，需要按同一命令重采。

## 重点模块判断

### Core

`under-utils-core` 目前的重量几乎都来自 `JsonUtils`：

- `jackson-databind`
- `jackson-core`
- `jackson-annotations`
- `jackson-datatype-jsr310`

`JsonUtils` 已标记为兼容维护 API，但它仍是已发布 public API。`1.x` 里直接把 Jackson 改成 optional 或移出 core，会让老用户升级后在运行时缺类。

当前策略：

- `1.x` 保留 core 的 Jackson 依赖。
- 运行时模块内部逐步不再依赖 `JsonUtils`。
- `2.0.0` 再评估独立 JSON 模块或删除历史 JSON 工具。

### HTTP

`under-utils-http` 现在只有 OkHttp 执行器和基于该执行器的 OpenAPI 客户端治理能力。

风险：

- OkHttp 默认带入 Kotlin runtime，这是 HTTP 模块的主要额外重量。
- 如果后续新增 HttpClient5 实现，应作为明确适配器或独立模块提供，而不是只添加 optional 依赖。

后续选择：

- 已移除未使用的 `httpclient5` optional 依赖。
- 中期：评估拆成 `under-utils-http-core` + `under-utils-http-okhttp`，但这会影响 public API 路径，不适合在 patch 版本贸然做。

### AI

`under-utils-ai` 当前复用 `under-utils-http` 的同步请求执行能力，并为 SSE 流式响应显式使用 OkHttp。主要依赖来自 HTTP/AI 模块的 OkHttp、Okio、Kotlin runtime 和 Jackson。

当前策略：

- AI 能力只放在独立 `under-utils-ai` 坐标中，不加入 `under-utils-starter`。
- 不引入厂商 SDK，避免为 OpenAI-compatible 基础调用和流式 SSE 带入额外重量。
- Spring Boot 自动装配已进入独立 `under-utils-ai-starter`，不并入 Spring/Redis starter，也不并入兼容聚合 starter。

### Security

`under-utils-security` 提供字段级 AES-GCM 加密、响应脱敏和 MyBatis 显式加密 TypeHandler。

当前策略：

- 加密核心只依赖 JDK crypto 和 Jackson 脱敏序列化能力。
- MyBatis TypeHandler 所需 `mybatis-plus-core` 在 security 模块中保持 optional。
- `under-utils-security-starter` 为了配置 `FieldEncryptor` 并注册 `EncryptedStringTypeHandler` 默认加密器，显式依赖 `mybatis-plus-core`；该 starter 不进入聚合入口，避免无安全落库需求的用户被动引入。
- 设置 `under.utils.security.field-encryption.enabled=false` 或未配置 key 时，不创建默认 `FieldEncryptor`，也不注册 TypeHandler 默认加密器。
- 字段加密不使用历史 `under-utils-core` 的 `AESUtils`，避免继续扩展兼容工具类的安全语义。

### Redis

`under-utils-redis` 的主要重量来自 Redisson：

- Netty
- Reactor / Reactive Streams
- RxJava
- Kryo / JBoss Marshalling
- Jackson YAML

这部分和 Redisson 客户端本身绑定，不能靠简单 POM 调整消除。

Micrometer 观测适配以 optional dependency 形式提供：

- 不改变 `under-utils-redis` 默认 runtime 依赖面。
- 用户直接构造模板时，可以显式使用 `MicrometerCacheOperationObserver`。
- starter 只有在应用上下文存在 `MeterRegistry` 且没有自定义 `CacheOperationObserver` 时才自动创建适配器。

另一个边界问题是：`RedisRateLimitStore` 和 `RedisRepeatSubmitStore` 实现了 `under-utils-spring` 里的 store 接口，因此 `under-utils-redis` 会默认依赖 Spring 模块。cache/lock 用户理论上不需要 Spring，但当前坐标会一并带入。

后续选择：

- `1.x` 保持现状，避免让直接使用 `RedisRateLimitStore` 的用户缺少接口依赖。
- `2.0.0` 评估把限流/防重 store SPI 下沉到更轻的 API 模块，或拆出 `under-utils-redis-spring`。

### JDBC

`under-utils-jdbc` 当前只承载 JDBC 版服务层幂等 store。

当前策略：

- 依赖 `under-utils-spring` 的 `IdempotencyStore` SPI，避免为一个 store 新增第二套幂等接口。
- 只依赖 Spring JDBC，不引入连接池、迁移工具、JPA 或具体数据库驱动。
- `under-utils-jdbc-starter` 只有在 `under.utils.idempotent.store=jdbc` 且存在 `JdbcOperations` 时装配，不进入兼容聚合 starter。
- 表结构和迁移由业务项目管理，starter 不自动建表，避免对生产库产生不可控副作用。

### Biz

`under-utils-biz` 当前主代码集中在导入任务模板、CSV reader、进度快照和错误 CSV 导出。没有使用 EasyExcel、POI 或 Jackson。

后续选择：

- 已移除 `under-utils-biz` POM 中未使用的 EasyExcel、POI、Jackson optional 依赖。
- 中期：如果要提供 Excel 流式导入，新增独立 `under-utils-excel` 或 `under-utils-biz-excel`，不要把 Excel 栈放回基础 biz 模块。

## 建议执行顺序

1. 继续保持 `under-utils-core` 的 JSON 兼容策略，不在 `1.x` 里破坏老用户。
2. 在 `1.0.6` 收敛模块选择矩阵、README 入口和官网索引，先降低认知重量。
3. 发版前按同一口径重新采集所有独立 starter 和新模块的依赖树，确认没有新增默认重依赖。
4. 为 `2.0.0` 记录 Redis/Spring SPI 拆分方案。

## 采集命令

```bash
mvn -B -ntp -Prelease -DskipTests package
mvn -B -ntp dependency:tree -Dscope=runtime -DoutputFile=target/dependency-tree-runtime.txt
```
