# Under-Utils JDBC

Spring JDBC 基础设施模块，目前提供数据库版服务层幂等状态存储。

本模块不创建数据源、不管理事务、不自动建表。业务项目需要自行提供 `JdbcOperations` 并创建幂等状态表。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-jdbc</artifactId>
    <version>1.0.5-SNAPSHOT</version>
</dependency>
```

## 主要 API

| API | 说明 |
|-----|------|
| `JdbcIdempotencyStore` | JDBC 版 `IdempotencyStore`，支持执行中状态、完成结果复用、owner token 完成和释放。 |
| `JdbcIdempotencyStoreOptions` | 表名、key 前缀和 begin 重试次数配置。 |

## 建表

构件内置了常用数据库脚本，推荐复制到业务项目的 Flyway、Liquibase 或现有迁移流程中：

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

`tableName` 只支持未加引号的 `table` 或 `schema.table`，避免 SQL 注入。业务 key、执行 token 和结果 payload 全部使用参数绑定。
`cleanupExpired()` 可用于删除已经过期的处理中记录或完成结果；Spring Boot 项目使用 starter 时会默认创建清理任务。

## 直接使用

```java
JdbcIdempotencyStore store = new JdbcIdempotencyStore(
        jdbcTemplate,
        resultCodec,
        JdbcIdempotencyStoreOptions.builder()
                .tableName("under_utils_idempotency")
                .keyPrefix("app:idempotent:")
                .maxBeginRetries(3)
                .build()
);
```

Spring Boot 项目通常优先使用 `under-utils-jdbc-starter`。
