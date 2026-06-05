# Under-Utils

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](#requirements)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36.svg)](#requirements)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.x-6DB33F.svg)](#requirements)
[![CI](https://github.com/yexianglun-d/under-utils/actions/workflows/ci.yml/badge.svg)](https://github.com/yexianglun-d/under-utils/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.yexianglun-d/under-utils-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.yexianglun-d/under-utils-starter)
[![Website](https://img.shields.io/badge/Website-under--utils.howied.me-2563eb.svg)](https://under-utils.howied.me/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

[中文](README.md) | English

Website: [https://under-utils.howied.me/](https://under-utils.howied.me/)

Under-Utils is an engineering-pattern utility toolkit for Java 21 and Spring Boot projects. It captures reusable infrastructure code that repeatedly appears in business systems and is easy to scatter across applications.

This project is not a replacement for Hutool, Apache Commons, or Guava. New features should solve reusable engineering problems with testable behavior, such as request-context propagation, rate limiting, repeat-submit protection, Redis distributed locks, cache rebuild patterns, OpenAPI client governance, basic AI model calls, safe pagination, audit filling, and import task workflows.

Current stable version: `1.0.3`. The `main` branch is currently in the `1.0.4-SNAPSHOT` development cycle.

Maven coordinates use the GitHub namespace `io.github.yexianglun-d`. Java packages remain under `com.undernine.utils` throughout `1.x` to avoid a package-level breaking change for published public APIs.

## Typical Use Cases

- Rate limiting, repeat-submit protection, and request context should not be scattered across controllers, AOP code, and task executors. Under-Utils centralizes key resolution, failure semantics, and local/Redis store switching.
- Redis cache-aside, logical-expire caches, distributed locks, and cache metrics are easy to rewrite inconsistently. Under-Utils templates collect TTL, null-value handling, jitter, rebuild locks, and observation boundaries.
- Third-party OpenAPI and OpenAI-compatible model calls often need token refresh, signing, idempotency headers, error decoding, retry behavior, and sensitive-data masking behind one reusable client boundary.

## Project Scope

Features that belong in this project should have a clear reuse boundary and encapsulate enough repeated complexity to justify a shared library.

Good fits:

- Infrastructure patterns reusable across services, not business rules for one application.
- Features with explicit failure semantics, resource boundaries, and test coverage.
- APIs that can remain stable after release.
- External-system dependencies hidden behind small, explicit interfaces.

Poor fits:

- Small utility methods already covered well by the JDK, Spring, Hutool, Apache Commons, or Guava.
- One-off workflows for a single business line.
- Shortcuts without error handling, concurrency boundaries, or test coverage.

`under-utils-core` keeps several historical static utility classes for compatibility. They are not the main direction for new features.

Dependency weight and module-splitting decisions are documented in [docs/DEPENDENCY_REVIEW.md](docs/DEPENDENCY_REVIEW.md). Engineering maturity work is tracked in [docs/ENGINEERING_MATURITY.md](docs/ENGINEERING_MATURITY.md), and future feature incubation is tracked in [docs/FUTURE_FEATURES.md](docs/FUTURE_FEATURES.md). Crypto redesign and core JSON migration notes are available in [docs/CRYPTO_REDESIGN.md](docs/CRYPTO_REDESIGN.md) and [docs/JSON_MODULE_MIGRATION.md](docs/JSON_MODULE_MIGRATION.md).

## Modules

| Module | Description |
|------|------|
| `under-utils-bom` | Centralizes versions for Under-Utils modules and related dependencies. |
| `under-utils-core` | Low-coupling basics such as Snowflake IDs and money helpers; historical static utilities are compatibility-only. |
| `under-utils-spring` | Spring Web context propagation, rate-limit/repeat-submit abstractions, response models, exception handling, and JSON masking. |
| `under-utils-redis` | Redisson-based distributed locks, Redis stores, cache-aside, logical-expire cache templates, built-in metrics, and optional Micrometer observation. |
| `under-utils-http` | HTTP convenience calls and OpenAPI client governance, including token refresh, signing, trace/idempotency headers, error decoding, and retry behavior. |
| `under-utils-ai` | Basic OpenAI-compatible AI model calls, including sync/streaming chat, named client registry, provider extension, response metadata, error classification, and sensitive-data protection. |
| `under-utils-security` | AES-GCM field encryption, explicit MyBatis encryption TypeHandler, and response masking annotations. |
| `under-utils-mybatis` | MyBatis-Plus safe pagination, sort whitelisting, audit filling, and page result models. |
| `under-utils-biz` | Reusable business workflow templates, currently focused on CSV import, async progress lookup, and error export. |
| `under-utils-ai-starter` | Spring Boot AI autoconfiguration that creates a default or multiple named `AiClient` instances from configuration. |
| `under-utils-security-starter` | Spring Boot security autoconfiguration that creates a field encryptor from explicit configuration and registers the MyBatis encryption TypeHandler default encryptor. |
| `under-utils-mybatis-starter` | Spring Boot MyBatis autoconfiguration that creates the MyBatis-Plus interceptor and default audit fill handler. |
| `under-utils-spring-starter` | Spring Boot starter for local Spring cross-cutting features only. |
| `under-utils-redis-starter` | Spring Boot Redis starter that includes the Spring starter and Redis-backed distributed features. |
| `under-utils-starter` | Compatibility aggregate starter that continues to cover Spring and Redis autoconfiguration. |
| `under-utils-samples` | Runnable sample application, not deployed as a formal Maven library artifact. |
| `under-utils-test` | Testcontainers integration-test module, enabled only with the `integration-tests` profile. |

## Requirements

- Java 21
- Maven 3.9+
- Spring Boot 3.1.x
- Docker, required only for Testcontainers integration tests or the Redis sample environment

## Installation

Import the BOM first, then add the starter or module you need. Starting from `1.0.2`, Spring Boot services should prefer lighter starters by need:

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

Use the Spring starter when you only need local Spring cross-cutting features:

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-spring-starter</artifactId>
</dependency>
```

Use the Redis starter when you need Redis distributed locks, Redis-backed rate-limit/repeat-submit stores, or cache templates:

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-redis-starter</artifactId>
</dependency>
```

The old `under-utils-starter` remains as an aggregate compatibility starter for projects that cannot change coordinates immediately.

If you only need basic AI model calls, add the AI module directly:

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-ai</artifactId>
</dependency>
```

For Spring Boot projects that want a configured default `AiClient`, add the standalone AI starter:

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-ai-starter</artifactId>
</dependency>
```

Use the standalone MyBatis starter when you want MyBatis-Plus pagination and audit filling autoconfigured without pulling it into the aggregate starter:

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-mybatis-starter</artifactId>
</dependency>
```

Use the standalone security starter when you need field encryption and response masking:

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-security-starter</artifactId>
</dependency>
```

Local development:

```bash
git clone https://github.com/yexianglun-d/under-utils.git
cd under-utils
mvn test
```

## Starter Example

The starter uses local in-memory state by default. Before switching to Redis, the application must provide a configured `RedissonClient`.

```yaml
under:
  utils:
    web:
      operation-context:
        enabled: true
        task-decorator-enabled: true
      rate-limit:
        enabled: true
        store: redis
      repeat-submit:
        enabled: true
        store: redis
    idempotent:
      enabled: true
      store: redis
      key-prefix: "under-utils:idempotent:"
      processing-ttl: 30s
      result-ttl: 10m
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

Rate limiting and repeat-submit protection reject requests by throwing `BizException` by default; the error message comes from annotation configuration. Local stores only work inside the current JVM. Multi-instance deployments should use Redis or custom `RateLimitStore` / `RepeatSubmitStore` implementations.

`@Idempotent` targets service-layer idempotency. Repeated calls while the first execution is still running throw `IdempotentInProgressException`; repeated calls after the first successful execution return the first result. If the business method has succeeded but the completed state cannot be persisted, the key is not released to avoid duplicate business execution. Multi-instance deployments should use Redis or a custom `IdempotencyStore`.

When a `MeterRegistry` exists and no custom `CacheOperationObserver` is provided, the Redis starter automatically enables Micrometer cache observation. Set `under.utils.redis.observation.enabled=false` to disable it.

## Usage Examples

Request-context propagation:

```java
OperationContext context = OperationContextHolder.getContext();
String traceId = context == null ? null : context.getTraceId();

Runnable task = OperationContextSnapshot.capture().wrap(() -> {
    OperationContext asyncContext = OperationContextHolder.getContext();
});
```

Rate limiting and repeat-submit protection:

```java
@RateLimit(limit = 10, period = 60, message = "too many requests")
@PostMapping("/sms/send")
public void sendSms(@RequestBody SendSmsCommand command) {
    smsService.send(command);
}

@PreventRepeat(timeout = 5, message = "duplicate request")
@PostMapping("/orders")
public Long createOrder(@RequestBody CreateOrderCommand command) {
    return orderService.create(command);
}
```

Service-layer business idempotency:

```java
@Idempotent(namespace = "order:create", key = "#command.requestNo")
public OrderResult createOrder(CreateOrderCommand command) {
    return orderService.create(command);
}
```

Redis cache-aside:

```java
UserProfile profile = cacheAsideTemplate.cache("user:profile:" + userId, UserProfile.class)
        .ttl(Duration.ofMinutes(3))
        .nullValueTtl(Duration.ofSeconds(10))
        .getOrLoad(key -> userRepository.findProfile(userId));
```

AI sync and streaming chat:

```java
AiClient aiClient = AiClient.builder()
        .baseUrl("https://api.example.com/v1")
        .apiKey(apiKey)
        .model("your-model-name")
        .build();

ChatResponse response = aiClient.chat(ChatRequest.user("Summarize this text."));

if (aiClient instanceof StreamingAiClient streamingAiClient) {
    try (ChatStream stream = streamingAiClient.streamChat(ChatRequest.user("Write step by step."))) {
        stream.stream()
                .filter(ChatStreamEvent::hasText)
                .forEach(event -> System.out.print(event.text()));
    }
}
```

Async import task:

```java
AsyncImportTaskTemplate importTasks = new AsyncImportTaskTemplate(executor);
String taskId = importTasks.submit(rows, handler);
ImportProgress progress = importTasks.findProgress(taskId).orElseThrow();
```

Safe pagination:

```java
SortFieldMapping mapping = SortFieldMapping.builder()
        .add("createdAt", "create_time")
        .add("status", "status")
        .build();

SafePageQuery query = SafePageQuery.of(page, size)
        .orderByDesc("createdAt");
```

Explicit field encryption with MyBatis:

```java
@TableName(value = "customer_profile", autoResultMap = true)
public class CustomerProfile {

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String idCardNo;
}
```

## Sample Application

```bash
mvn -pl under-utils-samples -am spring-boot:run
```

The sample application runs on port `18080` by default and does not require Redis or MySQL in the default profile. Redis setup and request examples are documented in [under-utils-samples/README.md](under-utils-samples/README.md).

## Verification

Default checks:

```bash
mvn -DskipTests compile
mvn test
```

Release artifact check:

```bash
mvn -Prelease -DskipTests package
```

Testcontainers integration tests:

```bash
mvn -Pintegration-tests -pl under-utils-test -am test
```

Public API compatibility check:

```bash
mvn -Papi-compat \
  -pl under-utils-core,under-utils-http,under-utils-ai,under-utils-ai-starter,under-utils-spring,under-utils-redis,under-utils-mybatis,under-utils-biz \
  -am \
  -DskipTests \
  verify
```

The `api-compat` profile compares current artifacts with the latest published stable version. It covers stable runtime modules and the public API of published starters by default. Changes that affect autoconfiguration behavior still need autoconfiguration tests and documentation.

The release process is documented in [docs/RELEASE.md](docs/RELEASE.md).

## Contributing

Before proposing a new feature, read [CONTRIBUTING.md](CONTRIBUTING.md). The key question is whether the feature is a reusable engineering pattern rather than application code, framework-provided behavior, or functionality already covered by mature utility libraries.

Public API changes follow [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md). Patch and minor releases should remain source-compatible unless there is a clear security or correctness exception.

Community entry points are listed in [docs/COMMUNITY.md](docs/COMMUNITY.md). Usage questions should start in GitHub Discussions; approachable tasks use the `good first issue`, `help wanted`, or `roadmap` labels.

## License

Under-Utils is licensed under the [MIT License](LICENSE).
