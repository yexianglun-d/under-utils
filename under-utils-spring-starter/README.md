# Under-Utils Spring Starter

Spring Boot 自动装配模块，只接入 `under-utils-spring` 的本地横切能力，不引入 `under-utils-redis` 和 Redisson。

适合只需要请求上下文、限流、防重复提交、操作 key 和本地状态存储的服务。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-spring-starter</artifactId>
    <version>1.0.3</version>
</dependency>
```

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

默认不会注册 `GlobalExceptionHandler`，避免接管业务已有响应契约。

## 配置

```yaml
under:
  utils:
    web:
      operation-context:
        enabled: true
        task-decorator-enabled: true
        trusted-identity-headers: false
        trusted-proxy-headers: false
      exception-handling:
        enabled: false
      rate-limit:
        enabled: true
        store: local
      repeat-submit:
        enabled: true
        store: local
```

`trusted-identity-headers` 只有在服务位于可信网关之后，且网关会清洗 `X-User-Id` / `X-Tenant-Id` 时才应开启。
`trusted-proxy-headers` 只有在可信网关会清洗 `X-Forwarded-For` / `X-Real-IP` 时才应开启；默认使用 servlet container 提供的 remote address。

`exception-handling.enabled=true` 后，starter 才会注册 `GlobalExceptionHandler` 并返回 Under-Utils `Result` 响应模型。

## 退让规则

starter 不会替换业务项目中同角色 Bean。常见退让点：

- `CurrentUserProvider`
- `CurrentTenantProvider`
- `TraceIdProvider`
- `OperationKeyResolver`
- `TaskDecorator`
- `RateLimitStore`
- `RepeatSubmitStore`

多实例服务如果需要集群级限流和防重复提交，应使用 `under-utils-redis-starter`，或自行实现 `RateLimitStore` / `RepeatSubmitStore`。
