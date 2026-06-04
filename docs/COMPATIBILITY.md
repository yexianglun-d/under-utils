# 兼容性策略

本文档定义 Under-Utils 在 `1.0.0` 之后如何处理 public API 变更。

目标很直接：用户升级 patch 或 minor 版本时，不应该遇到无关源码改动、配置重写或 starter 行为突变。

## 版本语义

Under-Utils 采用语义化版本，并按以下规则执行。

| 版本 | 用途 |
|------|------|
| Patch：`1.0.x` | bug 修复、文档、测试、内部重构、兼容行为修复。 |
| Minor：`1.x.0` | 新模块、新 public API、新可选配置和兼容默认行为。 |
| Major：`2.0.0` | 有计划的破坏性变更，例如删除或修改 public contract。 |

Patch 版本不应要求业务项目修改源码。

Minor 版本可以新增 API 或配置，但不应删除或重命名已有 contract。

破坏性变更默认只进入 major 版本。只有在旧行为不安全、无法继续维护，或法律/安全原因要求修改时，才允许提前破坏兼容；这种例外必须写入 `CHANGELOG.md` 和 Release Notes。

## 支持范围

仓库主要支持最新发布线和 `main`。

| 分支或版本线 | 支持策略 |
|--------------|----------|
| `main` | 活跃开发。 |
| 最新 `1.x` release | 在维护能力允许时提供 bug 修复和安全修复。 |
| 更旧 release 线 | 尽力维护，不承诺固定修复周期。 |

## 运行环境矩阵

当前 `1.x` 明确支持的主线环境如下：

| 维度 | 支持范围 | 发布前验证要求 |
|------|----------|----------------|
| JDK | Java 21 | `mvn -B -ntp clean test` 必须通过。 |
| Maven | 3.9+ | release profile、sources 和 javadocs 打包必须通过。 |
| Spring Boot | 3.1.x | Spring/starter 自动装配测试必须通过。 |
| MyBatis-Plus | 3.5.x | 单元测试和 Testcontainers MySQL 集成测试覆盖核心行为。 |
| Redisson | 3.23.x | 单元测试和 Testcontainers Redis 集成测试覆盖锁、限流和缓存模板。 |
| OkHttp | 4.12.x | HTTP、OpenAPI 和 AI 模块 MockWebServer 测试覆盖请求、超时和错误映射。 |

Java 17、Spring Boot 2.x/3.0.x、Jakarta EE 以外的旧 Servlet API 不在当前承诺范围内。业务项目如果需要这些组合，应在自己的集成测试中锁定依赖版本，并把兼容性结果补回 release notes。

## 公开 API 范围

以下内容一旦发布，就按 public API 对待：

- Maven 坐标：`groupId`、`artifactId`、模块列表和发布 packaging。
- `com.undernine.utils.*` 下 public/protected Java 类型、构造器、方法、字段、注解、枚举值，以及已声明或文档化的异常 contract。
- 注解属性及其默认值。
- `under.utils.*` 下 Spring Boot 配置 key、默认值和已文档化行为。
- starter 自动装配的 Bean 类型和已文档化 Bean 名称。
- 面向业务项目实现的 SPI，例如 store、provider、codec、decoder、signer、handler。
- 已文档化的失败语义、缓存行为、锁行为、重试行为、key 生成行为和线程安全假设。

以下内容不视为 public contract：

- 包可见类和成员。
- 测试类、测试 fixture。
- `under-utils-samples` 的接口路径和请求样例。
- README、模块文档、JavaDoc 或 API Review 中未提到的内部实现细节。
- 构建产物和生成文件。

## 兼容变更

通常可以视为兼容的变更：

- 新增类、方法重载、构造器重载、枚举类型、模块或可选配置 key。
- 给 SPI 新增 default 方法，前提是已有实现仍能编译和运行。
- 新增 Spring Bean，前提是它会对用户自定义 Bean 退让，且不改变已有装配路径。
- 修复行为，使其符合已文档化 contract。
- 收紧校验，前提是旧行为会产生非法状态、隐藏数据损坏或明显错误配置。
- 改进文档、测试、日志或内部实现。

兼容变更如果可能影响运行时行为，也需要写入 `CHANGELOG.md`。

## 破坏性变更

以下变更属于破坏性变更，默认应等待 major 版本：

- 删除或重命名 public 类型、方法、构造器、字段、注解、枚举值、模块、Maven 坐标或配置 key。
- 修改方法签名、返回类型、泛型边界、checked exception 声明或注解属性默认值。
- 降低已有 public 类型或成员的可见性。
- 修改 starter 默认值，并导致新副作用或关闭已有行为。
- 替换必要依赖，导致业务项目必须调整接入方式。
- 修改已序列化缓存 payload 格式，且没有迁移方案或兼容读取逻辑。
- 修改 key 生成、锁、重试、限流、防重复提交、缓存过期或错误解码语义，导致已有调用假设失效。
- 在下一个 major 版本之前删除 deprecated API。

如果必须在 major 版本前引入破坏性变更，PR 必须说明原因，并提供迁移路径。

## 弃用流程

替换 public API 时，默认走弃用流程。

规则：

- Java API 使用 `@Deprecated(since = "x.y.z", forRemoval = false)` 标记，前提是源码级别支持该写法。
- JavaDoc 说明替代 API；如果没有替代 API，需要说明原因。
- deprecated API 默认在当前 major 版本剩余周期内继续可用。
- 如果 deprecated 路径仍有用户，应保留兼容测试。
- 弃用信息需要写入 `CHANGELOG.md`；影响 API 边界时，也写入 `docs/API_REVIEW.md`。

默认只有 major 版本才能删除 deprecated API。安全或正确性例外必须明确记录。

## 配置迁移

配置 key 是用户可见 API。

替换配置 key 时：

- 可行时，旧 key 至少保留一个 minor 版本作为别名。
- 别名期优先打印 warning，而不是直接失败。
- 文档中写明旧 key、新 key、默认值和迁移示例。
- patch 版本避免修改默认值，除非旧默认值明确错误或不安全。

## Starter 自动装配

starter 变更需要额外谨慎，因为它会影响应用启动行为。

新增自动装配时，PR 需要确认：

- 用户自定义 Bean 仍通过 `@ConditionalOnMissingBean` 或等价条件优先。
- Redis 等可选外部系统只在显式选择相关能力时才成为必要条件。
- 本地默认路径不依赖 Docker、Redis、MySQL 或私有基础设施。
- 缺少必要 Bean 时的失败方式已文档化。
- 已文档化的 Bean 名称保持稳定。

## PR 检查项

任何面向用户的变更，PR 都应回答：

- 这是否是 public API 变更？
- 它是 patch-compatible、minor-compatible、deprecation 还是 breaking？
- 是否需要先弃用？
- 是否修改配置 key、默认值、异常类型、生成 key、缓存 payload 或 starter Bean？
- README、模块文档、`CHANGELOG.md` 和 `docs/API_REVIEW.md` 是否已更新？
- 旧路径和新路径是否都有兼容性测试？

运行时模块还应执行 public API 兼容性检查：

```bash
mvn -gs docs/central-dry-run-settings.xml -s docs/central-dry-run-settings.xml \
  -Papi-compat \
  -pl under-utils-core,under-utils-http,under-utils-ai,under-utils-ai-starter,under-utils-spring,under-utils-redis,under-utils-mybatis,under-utils-biz \
  -am \
  -DskipTests \
  verify
```

该检查以最近一个已发布稳定版本作为默认基线。`1.0.3` 发布前的默认基线是 `1.0.2`；新增模块在首次发布前没有旧基线，暂跳过单 jar 对比；starter 模块涉及自动装配和模块边界，仍需要结合自动装配测试、配置 key 文档和迁移说明人工确认。

## 发布说明

Release Notes 应拆分说明：

- 新增兼容能力。
- 修复行为。
- Deprecated API。
- 迁移说明。
- Breaking changes，仅用于 major 版本或已记录的例外。
