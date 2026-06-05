# 依赖重量审计

本文件记录 Under-Utils 的模块依赖重量、默认传递依赖和后续拆分判断。

审计目标不是追求最小 jar，而是避免用户为了一个窄能力被迫引入无关框架栈。

## 当前结论

- 项目自身 jar 都很小，主要重量来自传递依赖。
- starter 拆分后，Spring-only 用户不再被 `under-utils-starter` 强制带入 Redis/Redisson。
- `under-utils-core` 仍会带入 Jackson。`JsonUtils` 已经发布为 public API，`1.x` 内不应直接移除这组依赖。
- `under-utils-redis` 的真正重量来自 Redisson/Netty；这是模块能力本身决定的，不适合在 patch 版本里伪装成轻量模块。
- `under-utils-redis` 和 `under-utils-redis-starter` 新增 Micrometer 可选依赖，只用于 `MicrometerCacheOperationObserver`，不会通过普通依赖路径强制引入到用户应用。
- `under-utils-biz` 当前实现只使用无外部依赖的 CSV/导入模板，已移除未使用的 Excel/POI/Jackson optional 依赖。
- `under-utils-http` 已移除未实现的 HttpClient5 optional 依赖，当前对外边界明确为 OkHttp 执行器和 OpenAPI 客户端治理。
- `under-utils-ai` 作为独立模块复用 `under-utils-http`；`under-utils-ai-starter` 也保持独立，不会通过聚合 starter 让普通 Spring/Redis 用户被动引入 AI 依赖。
- `under-utils-mybatis-starter` 保持独立，不进入 `under-utils-starter` 聚合入口，避免普通 Spring/Redis 用户被动引入 MyBatis-Plus。
- `under-utils-security` 的 MyBatis TypeHandler 依赖保持 optional；`under-utils-security-starter` 为了注册 TypeHandler 默认加密器显式依赖 MyBatis-Plus core，但同样不进入聚合 starter。

## 模块快照

数据基于 `1.0.2-SNAPSHOT` 本地构建产物和 `runtime` 依赖树。

| 模块 | 主 jar | runtime 树规模 | 主要默认依赖 | 判断 |
|------|--------|----------------|--------------|------|
| `under-utils-core` | 36K | 5 行 | Jackson databind/core/annotations/jsr310 | `1.x` 保留；`2.0.0` 再考虑 JSON 迁移。 |
| `under-utils-http` | 64K | 16 行 | core、SLF4J、OkHttp/Okio/Kotlin、Jackson | 默认 OkHttp 会带 Kotlin runtime；不再声明未实现的 HttpClient5 适配。 |
| `under-utils-ai` | 28K | 11 行 | http、core、OkHttp/Okio、Jackson、SLF4J | 独立 AI 能力模块，不放入 starter 聚合入口，避免扩大默认依赖面。 |
| `under-utils-ai-starter` | 7.5K | 16 行 | ai module、Boot autoconfigure、Spring context | 独立 AI starter，不被兼容聚合 starter 引入。 |
| `under-utils-security` | 待采集 | 待采集 | Jackson；MyBatis-Plus core optional | 独立安全能力模块，字段加密核心不强制业务使用 starter。 |
| `under-utils-security-starter` | 待采集 | 待采集 | security module、Boot autoconfigure、MyBatis-Plus core | 独立 security starter，不进入聚合入口。 |
| `under-utils-spring` | 72K | 20 行 | core、Spring context/web/webmvc、AspectJ、Validation、Jackson | Spring MVC/AOP 模块，重量和定位一致。 |
| `under-utils-redis` | 40K | 45 行 | core、spring、Redisson/Netty、Jackson；Micrometer optional | Redisson 是主要重量；Micrometer 只服务可选观测适配。 |
| `under-utils-mybatis` | 24K | 9 行 | core、MyBatis-Plus、JSQLParser | 依赖和安全分页/审计能力匹配。 |
| `under-utils-mybatis-starter` | 待采集 | 待采集 | mybatis module、Boot autoconfigure | 独立 MyBatis starter，不进入聚合入口。 |
| `under-utils-biz` | 40K | 4 行 | core、SLF4J | 当前代码未使用 Excel/POI/Jackson，基础 biz 模块保持无 Excel 栈。 |
| `under-utils-spring-starter` | 16K | 10 行 | spring module、Boot autoconfigure、Servlet API | 符合 Spring-only starter 定位。 |
| `under-utils-redis-starter` | 8K | 9 行 | spring starter、redis module、Boot autoconfigure | 符合 Redis starter 定位。 |
| `under-utils-starter` | 4K | 3 行 | spring starter、redis starter | 兼容聚合入口，保持旧用户路径。 |

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

### Biz

`under-utils-biz` 当前主代码集中在导入任务模板、CSV reader、进度快照和错误 CSV 导出。没有使用 EasyExcel、POI 或 Jackson。

后续选择：

- 已移除 `under-utils-biz` POM 中未使用的 EasyExcel、POI、Jackson optional 依赖。
- 中期：如果要提供 Excel 流式导入，新增独立 `under-utils-excel` 或 `under-utils-biz-excel`，不要把 Excel 栈放回基础 biz 模块。

## 建议执行顺序

1. 继续保持 `under-utils-core` 的 JSON 兼容策略，不在 `1.x` 里破坏老用户。
2. 为 `2.0.0` 记录 Redis/Spring SPI 拆分方案。

## 采集命令

```bash
mvn -Prelease -DskipTests package

mvn -pl under-utils-core dependency:tree -Dscope=runtime
mvn -pl under-utils-http dependency:tree -Dscope=runtime
mvn -pl under-utils-ai dependency:tree -Dscope=runtime
mvn -pl under-utils-ai-starter dependency:tree -Dscope=runtime
mvn -pl under-utils-security dependency:tree -Dscope=runtime
mvn -pl under-utils-security-starter dependency:tree -Dscope=runtime
mvn -pl under-utils-spring dependency:tree -Dscope=runtime
mvn -pl under-utils-redis dependency:tree -Dscope=runtime
mvn -pl under-utils-mybatis dependency:tree -Dscope=runtime
mvn -pl under-utils-mybatis-starter dependency:tree -Dscope=runtime
mvn -pl under-utils-biz dependency:tree -Dscope=runtime
```
