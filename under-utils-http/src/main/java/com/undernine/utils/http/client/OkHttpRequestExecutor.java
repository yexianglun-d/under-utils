package com.undernine.utils.http.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.undernine.utils.core.json.JsonException;
import com.undernine.utils.http.config.HttpConfig;
import com.undernine.utils.http.enums.HttpMethod;
import com.undernine.utils.http.exception.HttpException;
import com.undernine.utils.http.exception.HttpNetworkException;
import com.undernine.utils.http.exception.HttpTimeoutException;
import com.undernine.utils.http.request.HttpRequest;
import com.undernine.utils.http.response.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp 请求执行器。
 * <p>
 * 基于 OkHttp 执行 Under-Utils {@link HttpRequest}，避免将底层 {@code okhttp3.OkHttpClient}
 * 直接暴露为业务调用入口。
 * </p>
 *
 * @author deng
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class OkHttpRequestExecutor {

    private static final ObjectMapper JSON_MAPPER = createJsonMapper();
    private static final Map<ClientKey, okhttp3.OkHttpClient> CLIENT_CACHE = new ConcurrentHashMap<>();

    /**
     * OkHttp 客户端实例
     */
    private final okhttp3.OkHttpClient client;

    /**
     * HTTP 配置
     */
    private final HttpConfig config;

    /**
     * 构造方法
     *
     * @param config HTTP 配置
     */
    public OkHttpRequestExecutor(HttpConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.client = sharedClient(this.config);
    }

    private static okhttp3.OkHttpClient sharedClient(HttpConfig config) {
        ClientKey key = ClientKey.from(config);
        return CLIENT_CACHE.computeIfAbsent(key, ignored -> buildClient(config));
    }

    /**
     * 构建 OkHttp 客户端
     *
     * @param config HTTP 配置
     * @return OkHttp 客户端实例
     */
    private static okhttp3.OkHttpClient buildClient(HttpConfig config) {
        okhttp3.OkHttpClient.Builder builder = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .followRedirects(config.isFollowRedirects())
                .retryOnConnectionFailure(config.getMaxRetries() > 0);

        // 连接池配置
        ConnectionPool connectionPool = new ConnectionPool(
                config.getMaxConnections(),
                config.getKeepAliveTime(),
                TimeUnit.MILLISECONDS
        );
        builder.connectionPool(connectionPool);

        // SSL 配置
        if (!config.isVerifySsl()) {
            configureTrustAllSsl(builder);
        }

        // 日志拦截器
        if (config.isLoggingEnabled()) {
            builder.addInterceptor(chain -> {
                Request request = chain.request();
                log.info("HTTP Request: {} {}", request.method(), request.url());
                Response response = chain.proceed(request);
                log.info("HTTP Response: {} {} - Status: {}", 
                        request.method(), request.url(), response.code());
                return response;
            });
        }

        return builder.build();
    }

    /**
     * 配置信任所有 SSL 证书（仅用于开发环境）
     *
     * @param builder OkHttp 构建器
     */
    private static void configureTrustAllSsl(okhttp3.OkHttpClient.Builder builder) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new HttpException("Failed to configure SSL", e);
        }
    }

    /**
     * 执行 HTTP 请求
     *
     * @param httpRequest HTTP 请求对象
     * @return HTTP 响应
     */
    public HttpResponse execute(HttpRequest httpRequest) {
        Request request = buildRequest(httpRequest);

        int retries = httpRequest.getMaxRetries() != null ? 
                httpRequest.getMaxRetries() : config.getMaxRetries();

        HttpException lastException = null;
        for (int i = 0; i <= retries; i++) {
            try {
                return executeRequest(httpRequest, request);
            } catch (HttpTimeoutException | HttpNetworkException e) {
                lastException = e;
                if (i < retries) {
                    log.warn("Request failed, retrying... ({}/{})", i + 1, retries);
                    try {
                        Thread.sleep(config.getRetryInterval());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new HttpException("Request interrupted", ie);
                    }
                }
            }
        }

        throw lastException != null ? lastException : 
                new HttpException("Request failed after " + retries + " retries");
    }

    /**
     * 以流式方式下载响应体到文件。
     *
     * @param httpRequest HTTP 请求对象
     * @param targetFile  目标文件
     * @return 仅包含状态码和响应头的 HTTP 响应
     */
    public HttpResponse download(HttpRequest httpRequest, File targetFile) {
        Request request = buildRequest(httpRequest);

        int retries = httpRequest.getMaxRetries() != null ?
                httpRequest.getMaxRetries() : config.getMaxRetries();

        HttpException lastException = null;
        for (int i = 0; i <= retries; i++) {
            try {
                return executeDownloadRequest(httpRequest, request, targetFile);
            } catch (HttpTimeoutException | HttpNetworkException e) {
                lastException = e;
                if (i < retries) {
                    log.warn("Download failed, retrying... ({}/{})", i + 1, retries);
                    try {
                        Thread.sleep(config.getRetryInterval());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new HttpException("Download interrupted", ie);
                    }
                }
            }
        }

        throw lastException != null ? lastException :
                new HttpException("Download failed after " + retries + " retries");
    }

    /**
     * 执行请求
     *
     * @param request OkHttp 请求对象
     * @return HTTP 响应
     */
    private HttpResponse executeRequest(HttpRequest httpRequest, Request request) {
        Call call = client.newCall(request);
        applyCallTimeout(httpRequest, call);
        try (Response response = call.execute()) {
            return buildResponse(response);
        } catch (InterruptedIOException e) {
            throw new HttpTimeoutException("Request timeout", e);
        } catch (IOException e) {
            throw new HttpNetworkException("Network error", e);
        }
    }

    private HttpResponse executeDownloadRequest(HttpRequest httpRequest, Request request, File targetFile) {
        Call call = client.newCall(request);
        applyCallTimeout(httpRequest, call);
        try (Response response = call.execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new HttpNetworkException("Response body is empty");
            }
            prepareTargetFile(targetFile);
            Files.copy(responseBody.byteStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return HttpResponse.builder()
                    .statusCode(response.code())
                    .headers(extractHeaders(response))
                    .build();
        } catch (InterruptedIOException e) {
            throw new HttpTimeoutException("Request timeout", e);
        } catch (IOException e) {
            throw new HttpNetworkException("Network error", e);
        }
    }

    private void applyCallTimeout(HttpRequest httpRequest, Call call) {
        Integer timeout = httpRequest.getTimeout();
        if (timeout == null) {
            return;
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        call.timeout().timeout(timeout, TimeUnit.MILLISECONDS);
    }

    private void prepareTargetFile(File targetFile) throws IOException {
        Objects.requireNonNull(targetFile, "targetFile must not be null");
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create parent directory: " + parentDir);
        }
    }

    /**
     * 构建 OkHttp 请求对象
     *
     * @param httpRequest HTTP 请求对象
     * @return OkHttp 请求对象
     */
    private Request buildRequest(HttpRequest httpRequest) {
        Request.Builder builder = new Request.Builder();

        // 构建 URL（包含查询参数）
        String url = buildUrl(httpRequest.getUrl(), httpRequest.getParams());
        builder.url(url);

        // 添加请求头
        addHeaders(builder, httpRequest);

        // 设置请求方法和请求体
        setMethodAndBody(builder, httpRequest);

        return builder.build();
    }

    /**
     * 构建完整 URL（包含查询参数）
     *
     * @param baseUrl 基础 URL
     * @param params  查询参数
     * @return 完整 URL
     */
    private String buildUrl(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
        params.forEach(urlBuilder::addQueryParameter);
        return urlBuilder.build().toString();
    }

    /**
     * 添加请求头
     *
     * @param builder     OkHttp 请求构建器
     * @param httpRequest HTTP 请求对象
     */
    private void addHeaders(Request.Builder builder, HttpRequest httpRequest) {
        // 添加默认请求头
        if (config.getDefaultHeaders() != null) {
            config.getDefaultHeaders().forEach(builder::addHeader);
        }

        // 添加自定义请求头
        if (httpRequest.getHeaders() != null) {
            httpRequest.getHeaders().forEach(builder::addHeader);
        }
    }

    /**
     * 设置请求方法和请求体
     *
     * @param builder     OkHttp 请求构建器
     * @param httpRequest HTTP 请求对象
     */
    private void setMethodAndBody(Request.Builder builder, HttpRequest httpRequest) {
        HttpMethod method = httpRequest.getMethod();
        RequestBody requestBody = buildRequestBody(httpRequest);

        switch (method) {
            case GET:
                builder.get();
                break;
            case POST:
                builder.post(requestBody != null ? requestBody : 
                        RequestBody.create(new byte[0], null));
                break;
            case PUT:
                builder.put(requestBody != null ? requestBody : 
                        RequestBody.create(new byte[0], null));
                break;
            case DELETE:
                if (requestBody != null) {
                    builder.delete(requestBody);
                } else {
                    builder.delete();
                }
                break;
            case PATCH:
                builder.patch(requestBody != null ? requestBody : 
                        RequestBody.create(new byte[0], null));
                break;
            case HEAD:
                builder.head();
                break;
            case OPTIONS:
                builder.method("OPTIONS", null);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    /**
     * 构建请求体
     *
     * @param httpRequest HTTP 请求对象
     * @return OkHttp 请求体
     */
    private RequestBody buildRequestBody(HttpRequest httpRequest) {
        // 文件上传（multipart/form-data）
        if (httpRequest.getFiles() != null && !httpRequest.getFiles().isEmpty()) {
            return buildMultipartBody(httpRequest);
        }

        // 表单提交（application/x-www-form-urlencoded）
        if (httpRequest.getFormParams() != null && !httpRequest.getFormParams().isEmpty()) {
            return buildFormBody(httpRequest.getFormParams());
        }

        // JSON 请求体
        if (httpRequest.getBody() != null) {
            return buildJsonBody(httpRequest.getBody());
        }

        return null;
    }

    /**
     * 构建 Multipart 请求体（文件上传）
     *
     * @param httpRequest HTTP 请求对象
     * @return Multipart 请求体
     */
    private RequestBody buildMultipartBody(HttpRequest httpRequest) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // 添加文件
        httpRequest.getFiles().forEach((name, file) -> {
            RequestBody fileBody = RequestBody.create(file, 
                    MediaType.parse("application/octet-stream"));
            builder.addFormDataPart(name, file.getName(), fileBody);
        });

        // 添加表单参数
        if (httpRequest.getFormParams() != null) {
            httpRequest.getFormParams().forEach(builder::addFormDataPart);
        }

        return builder.build();
    }

    /**
     * 构建表单请求体
     *
     * @param formParams 表单参数
     * @return 表单请求体
     */
    private RequestBody buildFormBody(Map<String, String> formParams) {
        FormBody.Builder builder = new FormBody.Builder();
        formParams.forEach(builder::add);
        return builder.build();
    }

    /**
     * 构建 JSON 请求体
     *
     * @param body 请求体对象
     * @return JSON 请求体
     */
    private RequestBody buildJsonBody(Object body) {
        String json;
        if (body instanceof String) {
            json = (String) body;
        } else {
            json = toJson(body);
        }
        return RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
    }

    private static String toJson(Object body) {
        try {
            return JSON_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to serialize HTTP request body to JSON: " + body.getClass().getName(), e);
        }
    }

    private static ObjectMapper createJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    /**
     * 构建 HTTP 响应对象
     *
     * @param response OkHttp 响应对象
     * @return HTTP 响应对象
     * @throws IOException 如果读取响应体失败
     */
    private HttpResponse buildResponse(Response response) throws IOException {
        // 读取响应体
        byte[] bodyBytes = null;
        String bodyString = null;
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            bodyBytes = responseBody.bytes();
            bodyString = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
        }

        return HttpResponse.builder()
                .statusCode(response.code())
                .headers(extractHeaders(response))
                .body(bodyString)
                .bodyBytes(bodyBytes)
                .build();
    }

    private Map<String, String> extractHeaders(Response response) {
        Map<String, String> headers = new HashMap<>();
        response.headers().forEach(pair -> headers.put(pair.getFirst(), pair.getSecond()));
        return headers;
    }

    private record ClientKey(int connectTimeout,
                             int readTimeout,
                             int writeTimeout,
                             boolean followRedirects,
                             boolean retryOnConnectionFailure,
                             int maxConnections,
                             long keepAliveTime,
                             boolean loggingEnabled,
                             boolean verifySsl) {

        private static ClientKey from(HttpConfig config) {
            return new ClientKey(
                    config.getConnectTimeout(),
                    config.getReadTimeout(),
                    config.getWriteTimeout(),
                    config.isFollowRedirects(),
                    config.getMaxRetries() > 0,
                    config.getMaxConnections(),
                    config.getKeepAliveTime(),
                    config.isLoggingEnabled(),
                    config.isVerifySsl()
            );
        }
    }
}
