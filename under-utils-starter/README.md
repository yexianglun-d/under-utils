# Under-Utils Starter

兼容聚合 starter，用于同时接入 `under-utils-spring-starter` 和 `under-utils-redis-starter`。

从 `1.0.2` 起，推荐按需选择轻量 starter：

- 只需要 Spring 本地横切能力：使用 `under-utils-spring-starter`。
- 需要 Redis 分布式能力：使用 `under-utils-redis-starter`。
- 已经使用旧坐标且暂时不想改依赖：继续使用 `under-utils-starter`。

starter 会在业务项目提供同类基础设施 Bean 时自动退让。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

## 聚合内容

`under-utils-starter` 本身只保留兼容入口，实际自动装配来自：

- `under-utils-spring-starter`
- `under-utils-redis-starter`

## 自动装配 Bean

默认 Bean：

- `CurrentUserProvider`
- `CurrentTenantProvider`
- `TraceIdProvider`
- `OperationKeyResolver`
- `OperationContextFilter`
- `OperationContextTaskDecorator`
- `RateLimitAspect`
- `PreventRepeatAspect`
- local `RateLimitStore`
- local `RepeatSubmitStore`

存在 `RedissonClient` 时，可按配置启用 Redis 相关 Bean：

- `RedisRateLimitStore`
- `RedisRepeatSubmitStore`
- `DistributedLockTemplate`
- `CacheValueCodec`
- `CacheOptions`
- `CacheAsideTemplate`
- `LogicalExpireCacheOptions`
- `LogicalExpireCacheTemplate`

## 配置

本地状态存储：

```yaml
under:
  utils:
    web:
      operation-context:
        enabled: true
        task-decorator-enabled: true
      rate-limit:
        enabled: true
        store: local
      repeat-submit:
        enabled: true
        store: local
```

Redis 状态存储和缓存模板：

```yaml
under:
  utils:
    web:
      rate-limit:
        store: redis
      repeat-submit:
        store: redis
    redis:
      lock-enabled: true
      cache:
        enabled: true
        ttl: 5m
        null-ttl: 30s
        jitter: 10s
        rebuild-lock-enabled: true
      logical-cache:
        enabled: true
        logical-ttl: 5m
        physical-ttl: 30m
```

## 退让规则

starter 不会替换业务项目中同角色 Bean。常见退让点：

- `CurrentUserProvider`
- `CurrentTenantProvider`
- `TraceIdProvider`
- `OperationKeyResolver`
- `TaskDecorator`
- `RateLimitStore`
- `RepeatSubmitStore`
- `CacheValueCodec`
- `CacheOptions`
- `CacheAsideTemplate`
- `LogicalExpireCacheOptions`
- `LogicalExpireCacheTemplate`

业务项目可以保留自己的安全上下文、key 策略、序列化 codec、缓存选项或降级策略。

## 注意事项

- Redis store 需要业务项目提供 `RedissonClient`，starter 不负责创建连接。
- local store 适合单实例开发和测试，不适合集群级保护。
- Redis 异常默认通过 Redisson 向外传播，业务可以通过自定义 store 接管降级。
- `under.utils.redis.logical-cache.physical-ttl` 必须大于 `logical-ttl`。
