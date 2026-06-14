# Under-Utils Redis Starter

Spring Boot 自动装配模块，用于接入 `under-utils-redis` 的分布式能力。它会引入 `under-utils-spring-starter`，因此同样包含请求上下文、限流和防重复提交的本地能力。

本模块要求业务项目提供已经配置好的 `RedissonClient`，不会创建 Redis 连接。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-redis-starter</artifactId>
    <version>1.0.5</version>
</dependency>
```

## 自动装配 Bean

存在 `RedissonClient` 时，可按配置启用 Redis 相关 Bean：

- `RedisRateLimitStore`
- `RedisRepeatSubmitStore`
- `RedisIdempotencyStore`
- `DistributedLockTemplate`
- `CacheValueCodec`
- `CacheOptions`
- `CacheAsideTemplate`
- `LogicalExpireCacheOptions`
- `LogicalExpireCacheTemplate`
- `MicrometerCacheOperationObserver`，仅在存在 `MeterRegistry` 且没有用户自定义 `CacheOperationObserver` 时创建

## 配置

```yaml
under:
  utils:
    web:
      rate-limit:
        store: redis
      repeat-submit:
        store: redis
    idempotent:
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
        rebuild-lock-enabled: true
      logical-cache:
        enabled: true
        logical-ttl: 5m
        physical-ttl: 30m
      observation:
        enabled: true
```

## 退让规则

starter 不会替换业务项目中同角色 Bean。常见退让点：

- `RateLimitStore`
- `RepeatSubmitStore`
- `IdempotencyStore`
- `DistributedLockTemplate`
- `CacheValueCodec`
- `CacheOptions`
- `CacheAsideTemplate`
- `LogicalExpireCacheOptions`
- `LogicalExpireCacheTemplate`
- `CacheOperationObserver`

Redis 或 Redisson 异常默认向外传播。如果业务需要 fail-open、降级或自定义序列化策略，请在业务项目内提供自定义 store、template 或 codec。

## Micrometer 观测

当应用上下文存在 `MeterRegistry`，且没有用户自定义 `CacheOperationObserver` Bean 时，starter 会自动创建 `MicrometerCacheOperationObserver`，把 cache-aside 和 logical-cache 的事件写入 Micrometer。

默认指标和 observation：

- `under.utils.redis.cache.operations`
- `under.utils.redis.cache.duration`
- observation name：`under.utils.redis.cache`

如需关闭自动接入：

```yaml
under:
  utils:
    redis:
      observation:
        enabled: false
```

该 observer 不会把业务 key 或实际 cache key 写入 tag。
