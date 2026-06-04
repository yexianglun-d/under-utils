# Spring Boot 防重复提交：从按钮连点到重复下单，一个 AOP 注解真的够吗？

做后台系统时，你大概率遇到过这些问题：

- 用户连续点了两次“提交订单”，数据库里多了一条订单。
- 前端按钮已经置灰了，但弱网环境下请求重试，后端还是收到了两次。
- 短信验证码接口被连续点击，几秒内发出多条短信。
- 后台导入任务跑得慢，运营以为没点上，又点了一次，结果启动了两个任务。
- 第三方回调、页面刷新、浏览器返回重试，让同一笔业务又进了一次接口。

这些问题看起来都可以甩给前端：

> 按钮点完置灰不就好了？

但真实系统里，前端只能减少误操作，不能保证后端不会收到重复请求。浏览器重试、网络抖动、接口超时、用户刷新、多端登录、多实例负载均衡，都会让重复请求绕过前端控制。

所以后端一定要有自己的入口保护。

很多 Spring Boot 项目第一次处理这个问题时，会写一个注解：

```java
@PreventRepeat
@PostMapping("/orders")
public Long createOrder(@RequestBody CreateOrderCommand command) {
    return orderService.create(command);
}
```

再写一个 AOP，在方法执行前生成 key，放到本地 `Map` 或 Redis。短时间内再次命中同一个 key，就拒绝请求。

这个方向没错。

但如果只做到“一个注解 + 一个切面”，很快就会遇到几个更真实的问题。

## 哪些接口需要防重复提交？

不是所有接口都需要加防重复提交。

真正适合做入口防重的，通常是“短时间内重复请求没有意义，甚至会制造脏数据”的接口。

比如：

| 场景 | 重复请求后果 |
|------|--------------|
| 创建订单 | 产生重复订单，后续支付、库存、账务都受影响 |
| 发送短信验证码 | 浪费短信费用，用户体验也很差 |
| 提交表单 | 生成重复工单、重复审批、重复记录 |
| 发起导入任务 | 同一份文件被处理多次，占用线程和资源 |
| 提交支付前置请求 | 可能创建多笔待支付记录 |
| 后台操作按钮 | 状态被重复推进，审计日志混乱 |

这些接口的共同点是：它们通常不是简单查询，而是会改变状态、占用资源或触发外部动作。

注意，防重复提交不是完整幂等方案。

如果是支付回调、消息消费、订单状态机这类核心一致性场景，最终还是要靠数据库唯一约束、幂等表、状态流转校验、消息去重等机制兜底。

`@PreventRepeat` 解决的是入口层短时间重复请求，不应该替代业务一致性设计。

## 一个常见但不够稳的实现

很多项目会这样写：

```java
@Aspect
@Component
public class RepeatSubmitAspect {

    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    @Around("@annotation(preventRepeat)")
    public Object around(ProceedingJoinPoint point, PreventRepeat preventRepeat) throws Throwable {
        String key = point.getSignature().toShortString();
        long now = System.currentTimeMillis();
        Long expireAt = cache.get(key);
        if (expireAt != null && expireAt > now) {
            throw new RuntimeException("请勿重复提交");
        }
        cache.put(key, now + 5000);
        return point.proceed();
    }
}
```

这个实现能跑，但上线后问题很多。

### 1. key 粒度太粗

上面的 key 只按方法签名生成。

这意味着所有用户访问同一个接口都会命中同一个 key。用户 A 提交订单后，用户 B 在 5 秒内也提交订单，可能被误判为重复提交。

如果改成按用户生成 key，又可能误伤同一个用户在不同页面上的不同操作。

如果把完整参数直接拼到 key 里，又可能遇到：

- 参数太长；
- 敏感信息进入 key；
- 字段顺序变化导致 key 不稳定；
- 同一个业务请求因为无关字段变化无法命中。

所以 key 不是简单字符串拼接，而是防重复提交里最容易写散的部分。

### 2. 本地 Map 只能保护单个 JVM

本地 `ConcurrentHashMap` 在单机开发环境里看起来没问题，但线上服务通常不止一个实例。

用户第一次请求打到 A 实例，第二次请求打到 B 实例。A 和 B 各自维护本地 Map，互相不知道对方的状态，防重复提交就失效了。

这也是为什么很多项目一开始测试正常，上线到多实例环境后又出现重复提交。

### 3. 状态会无限增长

本地 Map 如果没有容量上限和主动清理，会随着用户、租户、接口、参数组合不断增长。

短期看不出来，长期运行就可能变成隐性内存问题。

防重复提交不是只需要“放进去”，还要考虑“什么时候清理、最多保留多少、应用关闭时怎么释放”。

### 4. 失败语义不清楚

重复提交时应该返回什么？

是 HTTP 429，还是业务异常？

业务方法执行失败后，要不要释放 key？

Redis 挂了，是拒绝请求，还是降级放行？

这些都不是注解名字能解决的问题，而是公共能力必须提前定义清楚的边界。

## 防重复提交真正要抽象什么？

如果把防重复提交当成一个工程模式，它至少要拆成几件事：

- 注解：声明哪些接口需要保护。
- key 解析：决定什么叫“同一次提交”。
- 状态存储：决定状态放本地、Redis，还是业务自定义存储。
- 失败语义：重复提交、业务异常、存储异常分别怎么处理。
- 生命周期：本地状态怎么清理，资源怎么释放。
- 多实例边界：什么时候必须切到 Redis 或其他共享存储。

所以我不太建议把它写死在一个项目里的 AOP 里。

更好的方式是：把注解、key 解析、store 接口和默认实现拆开，让业务项目按部署形态选择。

这也是我在 Under-Utils 里做防重复提交能力的原因。

## Under-Utils 是怎么处理的？

Under-Utils 把防重复提交拆成三层：

- `@PreventRepeat`：声明接口需要防重复提交。
- `OperationKeyResolver`：负责根据请求上下文、用户、租户、参数等信息生成稳定 key。
- `RepeatSubmitStore`：负责状态存储，可以是本地，也可以是 Redis，也可以由业务项目自己实现。

普通 Spring Boot 项目可以先引入轻量 starter。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.yexianglun-d</groupId>
            <artifactId>under-utils-bom</artifactId>
            <version>1.0.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.yexianglun-d</groupId>
        <artifactId>under-utils-spring-starter</artifactId>
    </dependency>
</dependencies>
```

然后在接口上加注解：

```java
@PreventRepeat(timeout = 5, timeUnit = TimeUnit.SECONDS, message = "请勿重复提交")
@PostMapping("/orders")
public Long createOrder(@RequestBody CreateOrderCommand command) {
    return orderService.create(command);
}
```

这个写法适合单实例服务、开发环境、后台工具，或者明确只需要当前 JVM 保护的接口。

## 多实例环境切到 Redis

如果服务是多实例部署，就不应该依赖本地 store。

这时可以引入 Redis starter：

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-redis-starter</artifactId>
</dependency>
```

配置切换：

```yaml
under:
  utils:
    web:
      repeat-submit:
        enabled: true
        store: redis
```

这里有一个刻意保留的边界：Redis starter 不会替业务项目创建 Redis 连接，业务项目需要自己提供已经配置好的 `RedissonClient`。

原因很简单：Redis 地址、认证、连接池、集群部署、监控、熔断、降级，通常都是业务系统或基础设施平台负责，不应该被一个工具库偷偷接管。

Under-Utils 做的是把防重复提交状态写入 Redis store，而不是替你管理 Redis 基础设施。

## key 可以显式声明

默认 key 会结合租户、用户、请求 URI、方法名和参数摘要生成，避免最粗糙的“所有人共用一个 key”。

但有些接口需要更明确的业务粒度，比如同一个 `requestNo` 在短时间内只能提交一次。

可以用 SpEL 显式声明：

```java
@PreventRepeat(
        key = "#userId + ':' + #args[0].requestNo",
        timeout = 10,
        timeUnit = TimeUnit.SECONDS,
        message = "订单正在提交中，请勿重复操作"
)
@PostMapping("/orders")
public Long createOrder(@RequestBody CreateOrderCommand command) {
    return orderService.create(command);
}
```

可用变量包括：

- `#args`
- `#userId`
- `#tenantId`
- `#traceId`
- `#requestUri`
- `#context`

这里的重点不是“支持 SpEL”这件事，而是让 key 的生成逻辑显式、稳定、可 review。

## 业务失败后要不要释放 key？

这是一个很容易被忽略的问题。

比如用户提交订单，业务校验失败了。用户修正参数后希望马上重新提交。如果失败后 key 不释放，他可能会继续收到“请勿重复提交”的提示。

但也不是所有失败都应该释放。某些接口即使失败，也可能已经触发了外部动作或状态变化。

所以这个行为不能写死在切面里，而应该由注解显式表达：

```java
@PreventRepeat(
        timeout = 5,
        timeUnit = TimeUnit.SECONDS,
        releaseOnFailure = true,
        message = "请勿重复提交"
)
```

`releaseOnFailure = true` 表示业务方法抛异常时释放防重 key，允许用户在修正后重新提交。

## 本地 store 也要有资源边界

如果使用 local store，也不能只是一个无限增长的 Map。

Under-Utils 的本地 `LocalRepeatSubmitStore` 会处理：

- key 到期清理；
- 默认容量边界；
- 每实例后台清理线程；
- `close()` 释放资源。

starter 里可以配置本地 store 容量和清理间隔：

```yaml
under:
  utils:
    web:
      repeat-submit:
        store: local
        local-max-entries: 100000
        local-cleanup-interval: 1s
```

这不是为了让本地 store 替代 Redis，而是让它在适合的场景里不至于变成内存风险。

## Redis 异常要不要自动放行？

有些工具库会在 Redis 异常时直接吞掉异常，然后让请求继续执行。

这看起来“用户体验更好”，但风险是：业务方根本不知道防重复提交已经失效了。

Under-Utils 的默认策略是：Redis 或 Redisson 异常向外传播。

如果你的业务确实需要 fail-open、熔断或降级，可以自己实现 `RepeatSubmitStore`，把策略写在业务项目里。

这个选择看起来不够“自动”，但边界更清楚：公共库不替业务决定数据一致性风险。

## 什么时候不要用它？

防重复提交适合挡住入口层短时间重复请求。

但下面这些场景，不应该只靠它：

- 支付回调；
- 消息消费；
- 订单状态流转；
- 库存扣减；
- 金额入账；
- 跨系统最终一致性。

这些场景必须有业务幂等方案，比如唯一索引、幂等表、状态机、消息去重、事务边界等。

换句话说：

> `@PreventRepeat` 是入口保护，不是业务幂等的最终防线。

## 小结

Spring Boot 防重复提交不难，难的是别把它写成一个只能在当前项目里凑合用的 AOP。

真正需要考虑的是：

- 哪些接口值得防重；
- key 粒度怎么设计；
- 单机和多实例怎么切换；
- 业务失败后 key 是否释放；
- 本地状态如何清理；
- Redis 异常由谁决定降级；
- 它和业务幂等的边界在哪里。

Under-Utils 不是想再做一个 Hutool 式工具集合，而是把业务系统里反复出现、实现细节多、容易写散的工程模式沉淀成可测试、可替换、可发布的组件。

如果你正在做 Spring Boot 后端项目，刚好也遇到接口保护、Redis 缓存、OpenAPI 客户端治理、AI Client 接入这些重复问题，可以看一下这个项目。

项目地址：

- GitHub：https://github.com/yexianglun-d/under-utils
- 官网：https://under-utils.howied.me/
- Maven Central：https://central.sonatype.com/artifact/io.github.yexianglun-d/under-utils-starter

如果这个边界对你有参考价值，欢迎 Star，或者在 GitHub Discussions 里丢一个真实业务场景。我会优先把跨项目可复用、边界清晰、有测试价值的问题沉淀进后续版本。
