# Under-Utils HTTP

基于 OkHttp 的 HTTP 调用和 OpenAPI 客户端治理模块。

模块分为两层：

- `HttpRequest`、`HttpResponse`、`HttpUtils`：直接 HTTP 调用。
- `OpenApiClient`：封装开放平台常见治理能力，例如 token 注入、签名、trace header、幂等 key、业务错误解码和重试决策。
- `RefreshingAccessTokenProvider`：封装 access token 本地缓存、提前刷新和并发收敛。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-http</artifactId>
    <version>1.0.3</version>
</dependency>
```

## 直接 HTTP 调用

```java
HttpResponse response = HttpRequest.builder()
        .url("https://api.example.com/users")
        .method(HttpMethod.GET)
        .timeout(5000)
        .retry(1)
        .build()
        .execute();

if (response.isSuccess()) {
    String body = response.asString();
}
```

常用方法可以直接从方法级快捷 builder 开始，并由 builder 直接执行：

```java
HttpResponse response = HttpRequest.get("https://api.example.com/users")
        .header("Authorization", "Bearer " + token)
        .param("page", "1")
        .timeout(Duration.ofSeconds(5))
        .execute();
```

POST JSON：

```java
HttpResponse response = HttpRequest.post("https://api.example.com/orders")
        .header("Content-Type", "application/json")
        .body(command)
        .execute();
```

便捷 API：

```java
String body = HttpUtils.get("https://api.example.com/users");
String created = HttpUtils.postJson("https://api.example.com/orders", command);
```

全局默认配置：

```java
HttpConfig config = HttpConfig.builder()
        .connectTimeoutDuration(Duration.ofSeconds(5))
        .readTimeoutDuration(Duration.ofSeconds(10))
        .maxRetries(1)
        .retryIntervalDuration(Duration.ofMillis(500))
        .addDefaultHeader("User-Agent", "my-service/1.0")
        .build();

HttpUtils.setDefaultConfig(config);
```

已有配置可以通过 `toBuilder()` 复制后调整，避免重复声明所有字段。

相同客户端配置会复用底层 `OkHttpClient` 和连接池；`HttpRequest.timeout(...)` 会作为单次调用的整体超时应用到当前请求。

文件下载建议使用流式入口，避免把完整响应体缓存在内存中：

```java
HttpRequest.get("https://api.example.com/files/report.csv")
        .timeout(Duration.ofSeconds(30))
        .downloadToFile(new File("/tmp/report.csv"));
```

## OpenAPI 客户端

当每次调用都需要统一 token、签名、幂等和业务错误处理时，使用 `DefaultOpenApiClient`。

```java
OpenApiClient client = new DefaultOpenApiClient(
        OpenApiClientOptions.builder()
                .connectTimeoutDuration(Duration.ofSeconds(3))
                .readTimeoutDuration(Duration.ofSeconds(8))
                .maxRetries(1)
                .retryIntervalDuration(Duration.ofMillis(300))
                .build(),
        request -> tokenService.getAccessToken(),
        request -> {
            request.header("X-Sign", signer.sign(request));
            request.query("timestamp", String.valueOf(System.currentTimeMillis()));
        },
        ApiErrorDecoder.httpStatus()
);

OpenApiResponse<OrderResult> response = client.execute(
        OpenApiRequest.builder()
                .url("https://open.example.com/orders")
                .method(HttpMethod.POST)
                .operationName("create-order")
                .traceId(traceId)
                .idempotencyKey(requestNo)
                .body(command)
                .build(),
        OrderResult.class
);
```

token 有有效期时，可以使用 `RefreshingAccessTokenProvider`：

```java
AccessTokenProvider tokenProvider = new RefreshingAccessTokenProvider(
        request -> {
            TokenResponse token = tokenGateway.refreshToken();
            return RefreshingAccessTokenProvider.AccessToken.of(
                    token.accessToken(),
                    token.expiresAt()
            );
        },
        Duration.ofSeconds(30)
);
```

`refreshAhead` 表示到期前多久开始刷新。该实现只负责当前 JVM 内的 token 缓存；多实例共享、分布式锁、持久化和上游刷新失败兜底应由业务项目在 `TokenFetcher` 中处理。

`OpenApiResponse` 会区分 HTTP 响应和治理层结果：

- `success` 是经过 HTTP 状态和业务错误解码后的结果。
- `statusCode` 是 HTTP 状态码；没有拿到响应时为 `0`。
- `errorCode`、`errorMessage`、`retryable` 来自配置的 `ApiErrorDecoder`。

## 业务错误解码

很多开放平台会用 HTTP 200 返回业务错误码。可以通过 `ApiErrorDecoder` 把规则从业务方法中移出。

```java
ApiErrorDecoder decoder = (request, httpResponse) -> {
    if (!httpResponse.isSuccess()) {
        return ApiErrorDecoder.httpStatus().decode(request, httpResponse);
    }

    Map<String, Object> body = JsonUtils.fromJson(
            httpResponse.asString(),
            new TypeReference<Map<String, Object>>() {
            }
    );
    String code = String.valueOf(body.get("code"));
    if ("OK".equals(code)) {
        return ApiErrorDecodeResult.success();
    }
    return ApiErrorDecodeResult.failure(
            code,
            String.valueOf(body.get("message")),
            "BUSY".equals(code)
    );
};
```

只有解码结果标记为 `retryable`，并且还有重试预算时，客户端才会重试。
同步客户端默认不会在重试前 sleep，即使设置了 `retryInterval` 也只会立即重试，避免占用 Web 请求线程。
如确需当前线程等待，可显式设置 `blockingRetryDelayEnabled(true)`；生产级退避、熔断和异步调度建议放在应用侧客户端治理层。

## 异常

直接 HTTP API 会针对网络和超时失败抛出模块异常：

```java
try {
    String body = HttpUtils.get("https://api.example.com/users");
} catch (HttpTimeoutException e) {
    // timeout
} catch (HttpNetworkException e) {
    // network failure
} catch (HttpException e) {
    // other HTTP module failure
}
```

## 注意事项

- 生产环境不要关闭 SSL 校验。
- 非幂等请求应关闭或谨慎配置重试。
- 超时时间应按上游接口特点配置，全局默认只作为基线。
- 大文件或二进制下载应使用 `downloadToFile` / `HttpUtils.download`，不要先 `execute()` 再 `saveToFile()`。
- 当 token、签名、幂等、trace 和业务错误处理在多个 service 方法中重复出现时，优先使用 `OpenApiClient`。
