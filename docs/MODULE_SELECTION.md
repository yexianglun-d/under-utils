# 模块选择指南

本指南用于降低 Under-Utils 的认知重量。项目可以继续沉淀企业工程能力，但用户不应该为了一个窄能力被迫理解或引入整套工具包。

## 选择原则

1. 新项目优先选择独立 starter，不优先使用兼容聚合 `under-utils-starter`。
2. 只有真实使用某个技术栈时才引入对应模块，例如 MyBatis、Redis、JDBC、AI 或字段加密。
3. 普通业务服务从一个入口开始，后续按场景叠加，不一次性引入所有 starter。
4. library 模块适合手动装配或非 Spring Boot 场景；Spring Boot 项目通常优先选择对应 starter。
5. 新能力默认不进入 `under-utils-starter`，避免旧用户升级后被动增加依赖和自动装配行为。

## 快速决策

| 你的场景 | 推荐依赖 | 不建议 |
|----------|----------|--------|
| 只需要请求上下文、限流、防重复提交、服务层本地幂等 | `under-utils-spring-starter` | 为了这些能力引入 `under-utils-starter` |
| 多实例部署，需要 Redis 限流、防重、幂等、分布式锁或缓存模板 | `under-utils-redis-starter` | 同时再手动引入 `under-utils-spring-starter` |
| 不想引入 Redis，但要用业务数据库保存 `@Idempotent` 状态 | `under-utils-jdbc-starter` | 期待 starter 自动建表或接管事务 |
| MyBatis-Plus 需要分页插件、审计字段填充和安全分页工具 | `under-utils-mybatis-starter` | 把 MyBatis 能力放进普通 Spring starter |
| 需要字段级 AES-GCM 加密或响应脱敏 | `under-utils-security-starter` | 使用 `under-utils-core` 里的历史 `AESUtils` 承载安全治理 |
| 需要 OpenAI-compatible AI 客户端自动装配 | `under-utils-ai-starter` | 把 AI starter 加入普通 Web 服务默认依赖 |
| 只想手动使用某个能力，不想自动装配 | 对应 library 模块 | 引入 starter 后再关闭大量 Bean |

## 常见组合

### 普通单实例 Web 服务

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-spring-starter</artifactId>
</dependency>
```

适合本地限流、防重复提交、请求上下文传播和服务层本地幂等。local store 只保护当前 JVM，不适合作为集群级保护。

### Redis 分布式服务

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-redis-starter</artifactId>
</dependency>
```

`under-utils-redis-starter` 已经包含 Spring starter。业务项目仍需要自行提供 `RedissonClient`，本项目不创建 Redis 连接。

### 数据库幂等服务

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-jdbc-starter</artifactId>
</dependency>
```

`under-utils-jdbc-starter` 已经包含 Spring starter。它只在 `under.utils.idempotent.store=jdbc` 且存在 `JdbcOperations` 时装配，不自动建表，不接管数据源、事务或迁移工具。

### MyBatis-Plus 服务

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-mybatis-starter</artifactId>
</dependency>
```

该 starter 只处理 MyBatis-Plus interceptor 和审计字段填充。多数据源项目通常应由业务项目为每个 `SqlSessionFactory` 单独管理 interceptor。

### 安全字段治理

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-security-starter</artifactId>
</dependency>
```

字段加密要求显式配置 Base64 AES key，并要求 MyBatis 字段显式声明 `EncryptedStringTypeHandler`。本项目不做隐式全局字段加密。

## 不推荐的组合

- 新项目直接使用 `under-utils-starter` 作为默认入口。
- 同时引入 `under-utils-redis-starter` 和 `under-utils-spring-starter`，因为 Redis starter 已经包含 Spring starter。
- 同时配置 `under.utils.idempotent.store=redis` 和 `store=jdbc`。一个应用上下文只能选择一个默认幂等 store。
- 为了使用 library API 引入 starter，再通过配置关闭大部分自动装配。
- 期望 starter 自动创建数据库表、Redis 连接、数据源、事务管理器或 mapper 扫描。

## 聚合 starter 策略

`under-utils-starter` 只保留为兼容入口，继续覆盖 Spring 与 Redis 自动装配。后续新增的 AI、Security、MyBatis、JDBC 或其他企业能力默认不加入该聚合入口。

这样做的目的不是减少能力，而是保护老用户的升级路径：

- 不改变旧项目的传递依赖集合。
- 不在升级后新增未知自动装配 Bean。
- 不让普通 Web 服务被动引入 MyBatis、AI、JDBC 或字段加密依赖。

## 1.0.6 治理目标

`1.0.6` 优先做轻量化治理，而不是继续增加大模块：

- 明确模块选择矩阵。
- 重新审计 starter 边界和默认依赖。
- 保持新能力不进入兼容聚合 starter。
- 将 README、Quick Start、官网文档和依赖审计统一到同一套选择口径。
- 非必要不新增 public API、配置 key 或默认 Bean。
