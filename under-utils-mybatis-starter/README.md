# Under-Utils MyBatis Starter

Spring Boot 自动装配模块，为 MyBatis-Plus 项目提供分页插件、乐观锁、防全表更新删除和审计字段填充的默认 Bean。

本 starter 假设业务项目已经引入并配置 MyBatis-Plus、数据源和 mapper 扫描。它只负责 Under-Utils MyBatis 能力的自动装配，不接管数据源、事务或 mapper 扫描。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-mybatis-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

## 默认装配

默认创建：

- `MybatisPlusInterceptor`：分页、乐观锁、防全表更新删除。
- `DefaultMetaObjectHandler`：创建/更新时间、创建/更新人和逻辑删除默认值填充。

所有 Bean 都会在业务项目已经声明同类型 Bean 时退让。

## 配置

```yaml
under:
  utils:
    mybatis:
      enabled: true
      interceptor:
        enabled: true
        db-type: mysql
      audit:
        enabled: true
        create-time-field: createTime
        update-time-field: updateTime
        create-by-field: createBy
        update-by-field: updateBy
        deleted-field: deleted
        not-deleted-value: 0
        fill-deleted: true
```

如果需要接入当前用户 ID，业务项目声明 `AuditorProvider`：

```java
@Bean
public AuditorProvider auditorProvider() {
    return () -> 1001L;
}
```

多数据源项目通常需要为每个 `SqlSessionFactory` 单独配置 interceptor，不建议依赖全局默认 Bean。此时可以关闭本 starter 的 interceptor 自动装配：

```yaml
under:
  utils:
    mybatis:
      interceptor:
        enabled: false
```
