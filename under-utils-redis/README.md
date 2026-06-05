# Under-Utils Redis

基于 Redisson 的基础设施模式模块，供 Spring 应用和 starter 使用。

本模块要求业务项目提供已配置的 `RedissonClient`，不会自行创建 Redis 连接。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-redis</artifactId>
    <version>1.0.4</version>
</dependency>
```

## 主要 API

| API | 说明 |
|-----|------|
| `DistributedLockTemplate` | 在 Redisson lock 下执行回调，并确保释放锁。 |
| `RedisRateLimitStore` | 分布式 `RateLimitStore` 实现。 |
| `RedisRepeatSubmitStore` | 分布式 `RepeatSubmitStore` 实现。 |
| `RedisIdempotencyStore` | 分布式 `IdempotencyStore` 实现，支持执行中状态和完成结果复用。 |
| `CacheAsideTemplate` | cache-aside 读穿模板，支持空值缓存、TTL 抖动和重建锁。 |
| `LogicalExpireCacheTemplate` | 热点 key 缓存模板，逻辑过期后先返回旧值并后台刷新。 |
| `CacheMetrics` | 缓存模板内置指标快照，包含命中、未命中、加载、写入、锁和刷新计数。 |
| `CacheValueCodec` | cache 模板共享的序列化边界。 |
| `CacheOperationObserver` | 缓存命中、未命中、加载、写入、锁和刷新事件观测 SPI。 |
| `MicrometerCacheOperationObserver` | 可选 Micrometer 适配器，记录缓存事件 counter、duration timer 和 observation。 |

## 分布式锁

```java
Long orderId = distributedLockTemplate.execute(
        "order:create:" + command.requestNo(),
        1,
        30,
        TimeUnit.SECONDS,
        () -> orderService.create(command)
);
```

无法获取锁时，模板抛出 `DistributedLockException`。

## Cache-Aside

```java
CacheOptions options = CacheOptions.builder()
        .keyPrefix("app:cache:")
        .ttl(Duration.ofMinutes(5))
        .nullTtl(Duration.ofSeconds(30))
        .jitter(Duration.ofSeconds(10))
        .cacheNull(true)
        .rebuildLockEnabled(true)
        .build();

UserProfile profile = cacheAsideTemplate.getOrLoad(
        "user:profile:" + userId,
        UserProfile.class,
        options,
        key -> userRepository.findProfile(userId)
);
```

也可以使用模板提供的链式入口，把单次调用的 key、类型和局部配置写在一起：

```java
UserProfile profile = cacheAsideTemplate.cache("user:profile:" + userId, UserProfile.class)
        .ttl(Duration.ofMinutes(3))
        .nullValueTtl(Duration.ofSeconds(10))
        .jitter(Duration.ofSeconds(5))
        .rebuildLockEnabled(true)
        .getOrLoad(key -> userRepository.findProfile(userId));
```

行为：

- 缓存命中时不调用 loader。
- loader 返回 null 时，可以用更短 TTL 缓存空值占位。
- TTL jitter 用于降低集中失效。
- 重建锁用于降低缓存击穿。
- loader 异常会向外传播，不写入缓存。

## 逻辑过期缓存

```java
LogicalExpireCacheOptions options = LogicalExpireCacheOptions.builder()
        .keyPrefix("app:logical-cache:")
        .logicalTtl(Duration.ofMinutes(5))
        .physicalTtl(Duration.ofMinutes(30))
        .refreshExecutor(refreshExecutor)
        .build();

ProductView view = logicalExpireCacheTemplate.getOrLoad(
        "product:view:" + productId,
        ProductView.class,
        options,
        key -> productRepository.findView(productId)
);
```

链式入口同样支持逻辑过期缓存：

```java
ProductView view = logicalExpireCacheTemplate.cache("product:view:" + productId, ProductView.class)
        .logicalTtl(Duration.ofMinutes(2))
        .physicalTtl(Duration.ofMinutes(20))
        .refreshExecutor(refreshExecutor)
        .getOrLoad(key -> productRepository.findView(productId));
```

行为：

- key 不存在时同步加载。
- 逻辑未过期时直接返回缓存值。
- 逻辑过期时立即返回旧值，并提交后台刷新。
- `physicalTtl` 必须大于 `logicalTtl`，否则旧值无法作为兜底窗口。
- 配置 `LogicalExpireCacheRefreshFailureHandler` 后，后台刷新失败会回调处理器。
- 未显式配置 `refreshExecutor` 时，默认使用独立有界 daemon 线程池，不占用 `ForkJoinPool.commonPool()`。

生产环境仍建议按业务吞吐和下游容量显式传入 `refreshExecutor`，避免热点 key 刷新任务和应用其他后台任务互相影响。

## 限流、防重复提交和业务幂等 store

`RedisRateLimitStore`、`RedisRepeatSubmitStore` 和 `RedisIdempotencyStore` 实现了 `under-utils-spring` 中的 store 接口。
`RedisRateLimitStore` 会在同一个 key 的限流参数变化时更新 Redisson limiter 配置，并只在 key 没有 TTL 时设置过期时间。
`RedisIdempotencyStore` 使用 Redis 原子 set-if-absent 语义写入处理中状态，并使用执行 owner token 条件完成或释放 key；首次成功完成后保存结果并按结果 TTL 复用。

通常由 `under-utils-redis-starter` 在以下配置下装配：

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
```

Redis 或 Redisson 异常不会被吞掉。如果业务需要 fail-open 或兜底策略，请在应用内提供自定义 store。

## 缓存观测

缓存模板默认内置基础指标，不需要额外注册 observer 就可以读取命中率和加载计数。

```java
CacheMetrics metrics = cacheAsideTemplate.getMetrics();
long hitCount = metrics.getHitCount();
long missCount = metrics.getMissCount();
double hitRate = metrics.getHitRate();
```

业务项目需要接入 Micrometer、OpenTelemetry、日志或内部监控时，仍可以实现 `CacheOperationObserver`。

```java
CacheOperationObserver observer = new CacheOperationObserver() {
    @Override
    public void onHit(CacheOperationEvent event) {
        metrics.counter("cache.hit", "type", event.getOperationType().name()).increment();
    }

    @Override
    public void onLoadFailure(CacheOperationEvent event) {
        log.warn("cache load failed: {}", event.getCacheKey(), event.getError());
    }
};

CacheAsideTemplate template = new CacheAsideTemplate(redissonClient, codec, options, observer);
```

如果项目已经引入 Micrometer，也可以直接使用内置适配器：

```java
CacheOperationObserver observer = new MicrometerCacheOperationObserver(meterRegistry, observationRegistry);
CacheAsideTemplate template = new CacheAsideTemplate(redissonClient, codec, options, observer);
```

内置适配器记录：

- counter：`under.utils.redis.cache.operations`
- timer：`under.utils.redis.cache.duration`
- observation name：`under.utils.redis.cache`

指标 tag 只包含 `cache.type`、`cache.operation`、`cache.outcome`、`cache.null` 和 `exception`，不会把业务 key 或实际 cache key 写入 tag。

在 starter 中，如果应用声明了 `CacheOperationObserver` Bean，cache-aside 和 logical-cache 模板都会自动接入；如果没有自定义 observer 但上下文中存在 `MeterRegistry`，starter 会自动创建 `MicrometerCacheOperationObserver`。observer 抛出的运行时异常会被模板记录并忽略，不影响缓存主流程。

## 集成测试

Redis 缓存模板的真实 Redis 行为由 `under-utils-test` 通过 Testcontainers 覆盖：

```bash
mvn -Pintegration-tests -pl under-utils-test -am test -Dtest=RedisCacheTemplateIntegrationTest
```
