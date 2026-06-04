# Under-Utils MyBatis

MyBatis-Plus 支持模块，提供审计字段、逻辑删除约定、安全分页和排序字段白名单。

本模块假设业务项目已经使用 MyBatis-Plus。它不会隐藏 MyBatis-Plus，而是在项目级默认能力和 Web 入参安全方面做少量补充。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-mybatis</artifactId>
    <version>1.0.2</version>
</dependency>
```

## 主要 API

| API | 说明 |
|-----|------|
| `BaseEntity` | 通用 `id`、创建/更新时间、创建/更新人、逻辑删除字段。 |
| `DefaultMetaObjectHandler` | insert/update 时填充审计字段。 |
| `AuditorProvider` | 提供当前审计用户 ID。 |
| `SafePageQuery` | 面向请求入参的分页模型，避免暴露数据库列名。 |
| `SortFieldMapping` | 将公开排序字段映射到允许的数据库列。 |
| `PageResult` | 基于 MyBatis-Plus page 的轻量响应封装。 |
| `MybatisPlusConfig` | 常用 MyBatis-Plus interceptor 工厂。 |

## 基础配置

```java
@Configuration
public class MybatisConfiguration {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return MybatisPlusConfig.mybatisPlusInterceptor(DbType.MYSQL);
    }

    @Bean
    public AuditorProvider auditorProvider() {
        return () -> 1001L;
    }

    @Bean
    public DefaultMetaObjectHandler metaObjectHandler(AuditorProvider auditorProvider) {
        return new DefaultMetaObjectHandler(auditorProvider);
    }
}
```

典型逻辑删除配置：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## 实体

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String email;
    private Integer status;
}
```

对应表字段：

```sql
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(100),
  status INT DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME,
  create_by BIGINT,
  update_by BIGINT,
  deleted INT DEFAULT 0
);
```

## 安全分页

对外暴露排序字段名，不暴露数据库列名：

```java
SortFieldMapping mapping = SortFieldMapping.builder()
        .add("createdAt", "create_time")
        .add("username", "username")
        .add("status", "status")
        .build();

SafePageQuery pageQuery = SafePageQuery.of(1L, 20L)
        .orderByDesc("createdAt");

IPage<SysUser> page = userMapper.selectPage(
        pageQuery.buildPage(mapping),
        new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
);

PageResult<SysUser> result = PageResult.of(page);
```

`SafePageQuery` 会拒绝不在 `SortFieldMapping` 中的排序字段，避免请求原始值进入 `ORDER BY`。
单次请求最多允许 5 个排序字段，防止接口入参构造过宽的 `ORDER BY`。

`PageQuery` 仍保留用于兼容，但不建议作为 Web 入参。

## 审计字段

`DefaultMetaObjectHandler` 会填充：

- insert 时填充 `createTime` 和 `createBy`。
- insert/update 时填充 `updateTime` 和 `updateBy`。
- insert 时如果 `deleted` 为空，填充默认未删除值。

可以从安全上下文提供审计用户：

```java
@Bean
public AuditorProvider auditorProvider() {
    return () -> {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof YourUserDetails user)) {
            return null;
        }
        return user.getId();
    };
}
```

字段名或逻辑未删除值与 `BaseEntity` 不一致时，可以显式配置：

```java
@Bean
public DefaultMetaObjectHandler metaObjectHandler(AuditorProvider auditorProvider) {
    AuditFillOptions options = AuditFillOptions.builder()
            .createTimeField("createdAt")
            .updateTimeField("updatedAt")
            .createByField("creatorId")
            .updateByField("updaterId")
            .deletedField("isDeleted")
            .notDeletedValue(false)
            .build();
    return new DefaultMetaObjectHandler(auditorProvider, options);
}
```

没有逻辑删除字段的实体体系可以使用 `AuditFillOptions.builder().fillDeleted(false).build()`，避免 handler 默认写入 `deleted`。

## 多数据源场景

多数据源项目通常应为每个 `SqlSessionFactory` 单独配置 MyBatis-Plus interceptor，不建议把数据源选择逻辑放进本模块。

```java
@Configuration
public class MultiDatasourceMybatisConfiguration {

    @Bean
    public MybatisPlusInterceptor mysqlInterceptor() {
        return MybatisPlusConfig.mybatisPlusInterceptor(DbType.MYSQL);
    }

    @Bean
    public MybatisPlusInterceptor postgresInterceptor() {
        return MybatisPlusConfig.mybatisPlusInterceptor(DbType.POSTGRE_SQL);
    }

    @Bean
    public DefaultMetaObjectHandler metaObjectHandler(AuditorProvider auditorProvider) {
        return new DefaultMetaObjectHandler(auditorProvider);
    }
}
```

审计用户仍通过一个 `AuditorProvider` 提供。不同数据源的字段命名、逻辑删除值和 mapper 扫描范围，应留在业务项目自己的 MyBatis 配置里。

## 集成测试

MySQL 集成测试位于 `under-utils-test`，通过 Testcontainers 运行：

```bash
mvn -Pintegration-tests -pl under-utils-test -am test -Dtest=MybatisIntegrationTest
```

默认 Maven 构建不会启动 MySQL 或 Docker。

## 注意事项

- `deleted` 字段需要与 MyBatis-Plus 逻辑删除配置保持一致。
- Web/API 入参优先使用 `SafePageQuery`。
- 数据库特定行为应放在应用或专门模块中，本模块只提供可复用 MyBatis-Plus 辅助能力。
