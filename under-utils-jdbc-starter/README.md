# Under-Utils JDBC Starter

Spring Boot 自动装配模块，用于把 `under-utils-jdbc` 的数据库幂等 store 接入服务层 `@Idempotent`。

本 starter 不进入旧聚合 `under-utils-starter`，只有业务项目显式引入并配置 `under.utils.idempotent.store=jdbc` 时才生效。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-jdbc-starter</artifactId>
    <version>1.0.5</version>
</dependency>
```

## 自动装配 Bean

满足以下条件时自动创建 `JdbcIdempotencyStore`：

- 应用上下文存在 `JdbcOperations`。
- `under.utils.idempotent.store=jdbc`。
- 用户没有自定义 `IdempotencyStore` Bean。

满足 `store=jdbc`、存在 `JdbcIdempotencyStore` 且未关闭 `under.utils.idempotent.jdbc.cleanup-enabled` 时，还会创建 `JdbcIdempotencyCleanupScheduler`，定期删除过期幂等记录。

缺少 `JdbcOperations` 且仍配置 `store=jdbc` 时，`IdempotentAspect` 会因为没有可用 `IdempotencyStore` 而启动失败，这是有意的失败方式。

## 配置

```yaml
under:
  utils:
    idempotent:
      enabled: true
      store: jdbc
      key-prefix: "app:idempotent:"
      processing-ttl: 30s
      result-ttl: 5m
      jdbc:
        table-name: under_utils_idempotency
        max-begin-retries: 3
        cleanup-enabled: true
        cleanup-initial-delay: 1m
        cleanup-interval: 1m
```

## 建表

starter 不自动建表。构件内置了 MySQL/PostgreSQL 脚本：

- `META-INF/under-utils/jdbc/under_utils_idempotency_mysql.sql`
- `META-INF/under-utils/jdbc/under_utils_idempotency_postgresql.sql`

```sql
create table if not exists under_utils_idempotency (
    idem_key varchar(512) primary key,
    status varchar(32) not null,
    execution_token varchar(128),
    result_payload text,
    expire_at timestamp(3) not null,
    created_at timestamp(3) not null,
    updated_at timestamp(3) not null,
    index idx_under_utils_idem_expire_at (expire_at)
);
```

starter 不负责自动建表，也不替业务项目选择数据库事务策略。默认清理任务只删除 `expire_at` 已到期的记录；如果项目已有数据库定时任务，可关闭 starter 清理任务。
