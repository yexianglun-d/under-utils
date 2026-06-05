# Under-Utils

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](#环境要求)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36.svg)](#环境要求)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.x-6DB33F.svg)](#环境要求)
[![CI](https://github.com/yexianglun-d/under-utils/actions/workflows/ci.yml/badge.svg)](https://github.com/yexianglun-d/under-utils/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.yexianglun-d/under-utils-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.yexianglun-d/under-utils-starter)
[![官网](https://img.shields.io/badge/%E5%AE%98%E7%BD%91-under--utils.howied.me-2563eb.svg)](https://under-utils.howied.me/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

中文 | [English](README_EN.md)

官网：[https://under-utils.howied.me/](https://under-utils.howied.me/)

Under-Utils 是一组面向 Java 21 / Spring Boot 项目的工程模式工具包，用来沉淀业务系统里反复出现、实现细节多、容易写散的基础设施代码。

这个项目不定位为 Hutool、Apache Commons 或 Guava 的替代品。新增能力应解决可复用的工程问题，并且有明确行为可以测试，例如请求上下文传播、限流、防重复提交、Redis 分布式锁、缓存重建、OpenAPI 客户端治理、AI 模型基础调用、安全分页、审计填充和导入任务流程。

当前稳定版本：`1.0.3`。`main` 分支当前为 `1.0.4-SNAPSHOT` 开发周期。

Maven 坐标使用 GitHub namespace `io.github.yexianglun-d`。Java 包名在 `1.x` 内继续保持
`com.undernine.utils`，这是为了避免已发布 public API 发生包名级破坏性迁移。

## 典型场景

- 接口限流、防重复提交和操作上下文不想散落在 Controller、AOP 和线程池配置里，希望统一 key 解析、失败语义和本地/Redis 存储切换。
- MQ 重试、RPC 重试和跨服务回调不想重复执行业务，希望服务层按业务 key 做幂等，执行中重复立即返回处理中，完成后复用第一次结果。
- Redis cache-aside、逻辑过期缓存、分布式锁和缓存指标反复手写，希望以模板方式收口 TTL、空值、抖动、重建锁和观测边界。
- 调用第三方 OpenAPI 或 OpenAI-compatible 模型服务时，希望统一 token 刷新、签名、幂等 header、错误解码、重试和敏感信息脱敏。
- MyBatis-Plus 项目希望零配置接入分页插件、乐观锁、防全表更新删除和审计字段填充；敏感字段希望通过显式 TypeHandler 做 AES-GCM 落库加密。

## 项目边界

适合进入本项目的能力，应有清晰复用边界，并能封装足够多的重复复杂度。

适合的方向：

- 跨服务复用的基础设施模式，而不是单个应用的业务规则。
- 具备明确失败语义、资源边界和测试覆盖的能力。
- 发布后 API 可以保持稳定。
- 外部系统依赖被收口在小而清晰的接口后面。

不适合的方向：

- JDK、Spring、Hutool、Apache Commons、Guava 已成熟覆盖的小工具方法。
- 只服务单个业务线的一次性流程。
- 没有错误处理、并发边界或测试覆盖的快捷封装。

`under-utils-core` 中保留了一些历史静态工具类，用于兼容已有调用；它们不是后续新增能力的主线。

模块依赖重量和拆分判断见 [docs/DEPENDENCY_REVIEW.md](docs/DEPENDENCY_REVIEW.md)。
工程成熟度推进见 [docs/ENGINEERING_MATURITY.md](docs/ENGINEERING_MATURITY.md)，后续功能孵化见 [docs/FUTURE_FEATURES.md](docs/FUTURE_FEATURES.md)。
Crypto 重新建模和 core JSON 迁移分别见 [docs/CRYPTO_REDESIGN.md](docs/CRYPTO_REDESIGN.md) 与 [docs/JSON_MODULE_MIGRATION.md](docs/JSON_MODULE_MIGRATION.md)。

## 模块

| 模块 | 说明 |
|------|------|
| `under-utils-bom` | 统一管理 Under-Utils 模块和相关依赖版本。 |
| `under-utils-core` | 低耦合基础能力，例如雪花 ID、金额工具；历史静态工具仅做兼容维护。 |
| `under-utils-spring` | Spring Web 上下文传播、限流/防重抽象、返回结果、异常处理和 JSON 脱敏。 |
| `under-utils-redis` | 基于 Redisson 的分布式锁、限流/防重存储、cache-aside、逻辑过期缓存模板、内置指标和可选 Micrometer 观测适配。 |
| `under-utils-http` | HTTP 便捷调用与 OpenAPI 客户端治理，包括 token 刷新、签名、trace/idempotency header、错误解码和重试。 |
| `under-utils-ai` | OpenAI-compatible AI 大模型基础调用封装，覆盖同步/流式文本对话、原生消息和 tools 参数透传、命名客户端注册表、provider 扩展、响应元数据、基础错误分类和敏感信息脱敏。 |
| `under-utils-security` | 字段级 AES-GCM 加密、MyBatis 显式加密 TypeHandler 和响应脱敏注解。 |
| `under-utils-mybatis` | MyBatis-Plus 安全分页、排序白名单、审计填充和分页结果封装。 |
| `under-utils-biz` | 可复用业务流程模板，目前主要是 CSV 导入、异步导入进度查询和错误导出。 |
| `under-utils-ai-starter` | Spring Boot AI 自动装配入口，按配置创建默认或多个命名 `AiClient`。 |
| `under-utils-security-starter` | Spring Boot 安全自动装配入口，按显式配置创建字段加密器并注册 MyBatis 加密 TypeHandler 默认加密器。 |
| `under-utils-mybatis-starter` | Spring Boot MyBatis 自动装配入口，创建 MyBatis-Plus interceptor 和默认审计字段填充处理器。 |
| `under-utils-spring-starter` | Spring Boot 自动装配入口，只包含 Spring 本地横切能力。 |
| `under-utils-redis-starter` | Spring Boot Redis 自动装配入口，包含 Spring starter 并接入 Redis 分布式能力。 |
| `under-utils-starter` | 兼容聚合 starter，继续覆盖 Spring 与 Redis 自动装配。 |
| `under-utils-samples` | 可运行示例工程，不作为正式 Maven 库构件发布。 |
| `under-utils-test` | Testcontainers 集成测试模块，仅通过 `integration-tests` profile 启用。 |

## 环境要求

- Java 21
- Maven 3.9+
- Spring Boot 3.1.x
- Docker，仅在运行 Testcontainers 集成测试或 Redis 示例环境时需要

兼容性策略、支持矩阵和发布前验证命令见 [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)。当前 `1.0.3` 发布说明见 [docs/releases/v1.0.3.md](docs/releases/v1.0.3.md)。

## 安装

建议先引入 BOM，再按需引入 starter 或单模块。`1.0.2` 起普通 Spring Boot 服务优先按需选择轻量 starter：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.yexianglun-d</groupId>
            <artifactId>under-utils-bom</artifactId>
            <version>1.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.yexianglun-d</groupId>
        <artifactId>under-utils-spring-starter</artifactId>
    </dependency>
</dependencies>
```

只需要 Spring 本地横切能力时使用：

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-spring-starter</artifactId>
</dependency>
```

如果需要 Redis 分布式锁、Redis 限流/防重或缓存模板，使用 Redis starter：

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-redis-starter</artifactId>
</dependency>
```

如果需要 MyBatis-Plus 默认插件和审计字段自动填充，引入独立 MyBatis starter：

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-mybatis-starter</artifactId>
</dependency>
```

如果需要字段级加密和响应脱敏，引入独立 security starter：

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-security-starter</artifactId>
</dependency>
```

旧入口 `under-utils-starter` 会保留为聚合 starter，适合暂时不调整依赖坐标的项目。

如果只需要 AI 大模型基础调用封装，单独引入：

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-ai</artifactId>
</dependency>
```

Spring Boot 项目需要按配置创建默认 `AiClient` 时，引入独立 AI starter：

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-ai-starter</artifactId>
</dependency>
```

本地开发：

```bash
git clone https://github.com/yexianglun-d/under-utils.git
cd under-utils
mvn test
```

## Starter 示例

starter 默认使用本地内存状态存储。切换到 Redis 前，业务项目需要先提供 `RedissonClient`。

```yaml
under:
  utils:
    web:
      operation-context:
        enabled: true
        task-decorator-enabled: true
        trusted-identity-headers: false
        trusted-proxy-headers: false
      exception-handling:
        enabled: false
      rate-limit:
        enabled: true
        store: redis
      repeat-submit:
        enabled: true
        store: redis
    idempotent:
      enabled: true
      store: redis
      processing-ttl: 30s
      result-ttl: 5m
    redis:
      lock-enabled: true
      cache:
        enabled: true
        ttl: 5m
        null-ttl: 30s
        jitter: 10s
        cache-null: true
        key-prefix: "app:cache:"
        rebuild-lock-enabled: true
      logical-cache:
        enabled: true
        logical-ttl: 5m
        physical-ttl: 30m
        cache-null: true
        key-prefix: "app:logical-cache:"
      observation:
        enabled: true
```

限流和防重复提交默认失败语义是拒绝请求并抛出 `BizException`，异常消息来自注解配置。本地 store 只在当前 JVM 内生效，多实例部署应使用 Redis 或自定义 `RateLimitStore` / `RepeatSubmitStore`。
`@Idempotent` 面向服务层业务幂等：执行中重复会抛出 `IdempotentInProgressException`，首次成功完成后的重复调用会返回第一次结果；业务已成功但完成态写入失败时不会释放 key，避免重复执行业务；多实例部署应使用 Redis 或自定义 `IdempotencyStore`。
`trusted-identity-headers` 只有在可信网关已清洗 `X-User-Id` / `X-Tenant-Id` 时才应开启；`exception-handling.enabled=true` 后才会注册 Under-Utils 的 `GlobalExceptionHandler`。
存在 `MeterRegistry` 且没有自定义 `CacheOperationObserver` 时，Redis starter 会自动接入 Micrometer 缓存观测；需要关闭时设置 `under.utils.redis.observation.enabled=false`。

## 使用示例

请求上下文传播：

```java
OperationContext context = OperationContextHolder.getContext();
String traceId = context == null ? null : context.getTraceId();

Runnable task = OperationContextSnapshot.capture().wrap(() -> {
    OperationContext asyncContext = OperationContextHolder.getContext();
});
```

限流和防重复提交：

```java
@RateLimit(limit = 10, period = 60, message = "请求过于频繁")
@PostMapping("/sms/send")
public void sendSms(@RequestBody SendSmsCommand command) {
    smsService.send(command);
}

@PreventRepeat(timeout = 5, message = "请勿重复提交")
@PostMapping("/orders")
public Long createOrder(@RequestBody CreateOrderCommand command) {
    return orderService.create(command);
}
```

服务层业务幂等：

```java
@Idempotent(namespace = "order:create", key = "#command.requestNo")
public CreateOrderResult createOrder(CreateOrderCommand command) {
    return orderRepository.create(command);
}
```

Redis cache-aside：

```java
UserProfile profile = cacheAsideTemplate.get(
        "user:profile:" + userId,
        UserProfile.class,
        () -> userRepository.findProfile(userId)
);
```

异步导入任务：

```java
AsyncImportTaskTemplate importTasks = new AsyncImportTaskTemplate(executor);
String taskId = importTasks.submit(rows, handler);
ImportProgress progress = importTasks.findProgress(taskId).orElseThrow();
```

安全分页：

```java
SortFieldMapping mapping = SortFieldMapping.builder()
        .add("createdAt", "create_time")
        .add("status", "status")
        .build();

SafePageQuery query = SafePageQuery.of(page, size)
        .orderByDesc("createdAt");
```

MyBatis 字段级加密：

```java
@TableName(value = "sys_user", autoResultMap = true)
public class SysUser {

    @TableField(value = "mobile", typeHandler = EncryptedStringTypeHandler.class)
    private String mobile;
}
```

## 示例工程

```bash
mvn -pl under-utils-samples -am spring-boot:run
```

示例工程默认端口为 `18080`，默认不依赖 Redis 或 MySQL。Redis 示例和请求样例见 [under-utils-samples/README.md](under-utils-samples/README.md)。

## 验证

默认检查：

```bash
mvn -DskipTests compile
mvn test
```

发布构件检查：

```bash
mvn -Prelease -DskipTests package
```

Testcontainers 集成测试：

```bash
mvn -Pintegration-tests -pl under-utils-test -am test
```

public API 兼容性检查：

```bash
mvn -Papi-compat \
  -pl under-utils-core,under-utils-http,under-utils-ai,under-utils-ai-starter,under-utils-spring,under-utils-redis,under-utils-mybatis,under-utils-biz \
  -am \
  -DskipTests \
  verify
```

`api-compat` profile 会把当前构件和最近一个已发布稳定版本做 public API 对比。默认覆盖稳定运行时模块和已发布 starter 的 public API；涉及自动装配行为的变更仍需要结合自动装配测试和文档记录维护兼容性。

发布流程见 [docs/RELEASE.md](docs/RELEASE.md)。

## 贡献

提交新能力前请先阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。核心判断是：这个能力是否属于可复用工程模式，而不是应用代码、框架已有能力或成熟工具库已有能力。

public API 变更遵循 [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)。除非有明确的安全或正确性例外，patch 和 minor 版本应保持源码兼容。

社区参与入口见 [docs/COMMUNITY.md](docs/COMMUNITY.md)。使用问题优先发起 GitHub Discussions；可接手任务会使用 `good first issue`、`help wanted` 或 `roadmap` 标签。

## 许可证

Under-Utils 使用 [MIT License](LICENSE)。
