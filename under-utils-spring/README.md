# Under-Utils Spring

Spring Web 支持模块，提供请求上下文传播、限流、防重复提交、服务层幂等、返回结果、异常处理和 JSON 字段脱敏。

如果需要自动装配，优先使用 `under-utils-spring-starter`。直接使用本模块时，建议只显式注册需要的 Bean。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-spring</artifactId>
    <version>1.0.5</version>
</dependency>
```

## 主要 API

| 领域 | API |
|------|-----|
| 请求上下文 | `OperationContext`、`OperationContextFilter`、`OperationContextHolder`、`OperationContextSnapshot`、`OperationContextTaskDecorator` |
| 身份 SPI | `CurrentUserProvider`、`CurrentTenantProvider`、`TraceIdProvider`、`OperationContextCustomizer` |
| 限流 | `@RateLimit`、`RateLimitAspect`、`RateLimitStore`、`LocalRateLimitStore` |
| 防重复提交 | `@PreventRepeat`、`PreventRepeatAspect`、`RepeatSubmitStore`、`LocalRepeatSubmitStore` |
| 服务层幂等 | `@Idempotent`、`IdempotentAspect`、`IdempotencyStore`、`LocalIdempotencyStore`、`IdempotencyResultCodec` |
| Web 响应 | `Result`、`ResultCode`、`BizException`、`GlobalExceptionHandler` |
| JSON 脱敏 | `@Sensitive`、`SensitiveJsonSerializer`、`DesensitizeUtils` |
| 兼容 AOP | `@OperationLog`、`@Retry`、`@TimeLog` 及对应切面 |

## 请求上下文

`OperationContextFilter` 会写入 `OperationContextHolder`。默认只信任 `X-Trace-Id` 作为链路标识，不会把客户端传入的 `X-User-Id` / `X-Tenant-Id` 当作可信身份来源，也不会把 `X-Forwarded-For` / `X-Real-IP` 当作可信客户端 IP 来源。

```java
OperationContext context = OperationContextHolder.getContext();
String traceId = context == null ? null : context.getTraceId();
```

异步任务中可以捕获并恢复上下文：

```java
Runnable task = OperationContextSnapshot.capture().wrap(() -> {
    OperationContext asyncContext = OperationContextHolder.getContext();
});
```

`OperationContextTaskDecorator` 可挂到 Spring 线程池。使用 starter 时，如果业务项目没有自定义 `TaskDecorator`，可以自动装配。

如果服务部署在可信网关之后，且网关已经清洗用户、租户和代理 IP Header，可以在 starter 中显式开启：

```yaml
under:
  utils:
    web:
      operation-context:
        trusted-identity-headers: true
        trusted-proxy-headers: true
```

## 限流和防重复提交

```java
@RateLimit(limit = 10, period = 60, message = "请求过于频繁")
@PostMapping("/sms/send")
public void sendSms(@RequestBody SendSmsCommand command) {
    smsService.send(command);
}

@PreventRepeat(timeout = 5, timeUnit = TimeUnit.SECONDS, message = "请勿重复提交")
@PostMapping("/orders")
public Long createOrder(@RequestBody CreateOrderCommand command) {
    return orderService.create(command);
}
```

运行时行为：

- `@RateLimit` 超过额度时抛出 `BizException`。
- `limit <= 0` 表示拒绝所有请求；`period <= 0` 按 1 秒窗口处理。
- `@PreventRepeat` 在 key 未过期前再次提交时抛出 `BizException`。
- `timeout <= 0` 按最小 1ms 窗口处理。
- `releaseOnFailure = true` 时，业务方法抛异常会释放防重 key。

key 解析规则：

- `key` 为空时，使用租户、用户、URI、方法名和参数摘要生成默认 key。
- `key` 非空时按 SpEL 解析，可用变量包括 `#args`、`#userId`、`#tenantId`、`#traceId`、`#requestUri`、`#context`。
- SpEL 解析失败不会中断请求，会退回到确定性 key。

存储选择：

- `LocalRateLimitStore` 和 `LocalRepeatSubmitStore` 只保护当前 JVM。
- 本地 store 会按窗口清理过期 key，并设置默认最大容量，避免不同用户或业务 key 无限制增长。
- 本地 store 默认启用每实例后台清理线程，支持 `close()` 释放资源；作为 Spring Bean 使用时会随应用上下文关闭。
- 多实例服务应使用 `under-utils-redis` 提供的 Redis store，或自行实现 store。
- Redis 异常默认向外传播；如需降级，应由业务自定义 store。

starter 本地 store 可配置容量和清理间隔：

```yaml
under:
  utils:
    web:
      rate-limit:
        store: local
        local-max-entries: 100000
        local-cleanup-interval: 1s
      repeat-submit:
        store: local
        local-max-entries: 100000
        local-cleanup-interval: 1s
```

直接接入时需要注册 `RateLimitAspect`、`PreventRepeatAspect`、`OperationKeyResolver` 和对应 store。普通 Spring Boot 应用使用 `under-utils-spring-starter` 更简单。

## 服务层业务幂等

`@Idempotent` 面向 MQ 重试、RPC 重试和跨服务回调等服务层重复执行场景，不依赖 HTTP 请求上下文。

```java
@Idempotent(namespace = "order:create", key = "#command.requestNo")
public CreateOrderResult createOrder(CreateOrderCommand command) {
    return orderRepository.create(command);
}
```

运行时行为：

- 首次调用获得执行业务资格。
- 相同 key 首次执行中，重复调用会抛出 `IdempotentInProgressException`。
- 首次调用成功完成后，相同 key 重复调用会返回第一次结果。
- 业务方法抛异常时默认释放 key，允许后续重试重新执行业务。
- 业务方法已成功但完成态写入失败时不会释放 key，避免重复执行业务。
- 显式 SpEL key 解析失败会抛出 `IdempotentKeyResolveException`，不会退回到兜底 key。

本地 `LocalIdempotencyStore` 只保护当前 JVM，并带容量上限和懒启动的后台过期清理。完成态结果通过 `IdempotencyResultCodec` 保存和恢复，避免重复调用直接复用同一个可变对象引用。多实例服务应使用 `under-utils-redis` 提供的 Redis store、`under-utils-jdbc` 提供的 JDBC store，或自行实现 `IdempotencyStore`。

如果项目已经引入 Micrometer，可以使用 `MicrometerIdempotencyObserver` 记录幂等事件：

```java
IdempotencyObserver observer = new MicrometerIdempotencyObserver(meterRegistry, observationRegistry);
```

默认指标和 observation：

- `under.utils.idempotency.operations`
- `under.utils.idempotency.duration`
- observation name：`under.utils.idempotency`

该 observer 只记录 `idempotency.operation`、`idempotency.outcome` 和 `exception`，不会把幂等 key 或方法参数写入 tag。

## 返回结果和异常处理

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }
}
```

如需统一异常响应，注册 `GlobalExceptionHandler`：

```java
@Configuration
@Import(GlobalExceptionHandler.class)
public class WebConfiguration {
}
```

使用 starter 时也可以显式开启：

```yaml
under:
  utils:
    web:
      exception-handling:
        enabled: true
```

`Result` 只是轻量响应模型，不是强制约束。已有统一响应模型的应用，可以继续使用自己的 contract，同时复用上下文、限流和防重能力。

## 敏感字段脱敏

```java
public class UserView {

    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
}
```

使用时需要确保 Jackson 序列化器已注册，或通过 starter 接入相关基础设施。

## 兼容 AOP

`@OperationLog`、`@Retry`、`@TimeLog` 保留用于兼容，不是新增能力主线。starter 不会自动启用它们。

确需使用时，请显式导入：

```java
@Configuration
@EnableAspectJAutoProxy
@Import({
    OperationLogAspect.class,
    RetryAspect.class,
    TimeLogAspect.class
})
public class LegacyAopConfiguration {
}
```

注意：

- `@OperationLog` 默认不记录请求参数，除非显式设置 `recordParams = true`。
- `@Retry` 是历史兼容同步重试，会使用当前线程 sleep；新代码不要把它作为 Web 请求线程上的客户端治理方案。
- 生产级观测和重试治理建议使用应用侧 tracing、metrics、队列或客户端治理组件。
