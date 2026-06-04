package com.undernine.utils.http.request;

import com.undernine.utils.http.client.OkHttpRequestExecutor;
import com.undernine.utils.http.config.HttpConfig;
import com.undernine.utils.http.enums.HttpMethod;
import com.undernine.utils.http.response.HttpResponse;
import lombok.Data;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP 请求构建器
 * <p>
 * 使用构建器模式创建 HTTP 请求。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * HttpResponse response = HttpRequest.builder()
 *     .url("https://api.example.com/users")
 *     .method(HttpMethod.GET)
 *     .header("Authorization", "Bearer token")
 *     .param("page", "1")
 *     .timeout(5000)
 *     .build()
 *     .execute();
 * }</pre>
 * </p>
 *
 * @author deng
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class HttpRequest {

    /**
     * 请求 URL
     */
    private String url;

    /**
     * 请求方法
     */
    private HttpMethod method;

    /**
     * 请求头
     */
    private Map<String, String> headers;

    /**
     * URL 参数
     */
    private Map<String, String> params;

    /**
     * 请求体
     */
    private Object body;

    /**
     * 上传文件
     */
    private Map<String, File> files;

    /**
     * 表单参数
     */
    private Map<String, String> formParams;

    /**
     * 超时时间（毫秒）
     */
    private Integer timeout;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * HTTP 配置
     */
    private HttpConfig config;

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建指定方法和 URL 的请求构建器。
     *
     * @param url    请求 URL
     * @param method 请求方法
     * @return 构建器实例
     */
    public static Builder request(String url, HttpMethod method) {
        return builder().url(url).method(method);
    }

    /**
     * 创建 GET 请求构建器。
     *
     * @param url 请求 URL
     * @return 构建器实例
     */
    public static Builder get(String url) {
        return request(url, HttpMethod.GET);
    }

    /**
     * 创建 POST 请求构建器。
     *
     * @param url 请求 URL
     * @return 构建器实例
     */
    public static Builder post(String url) {
        return request(url, HttpMethod.POST);
    }

    /**
     * 创建 PUT 请求构建器。
     *
     * @param url 请求 URL
     * @return 构建器实例
     */
    public static Builder put(String url) {
        return request(url, HttpMethod.PUT);
    }

    /**
     * 创建 DELETE 请求构建器。
     *
     * @param url 请求 URL
     * @return 构建器实例
     */
    public static Builder delete(String url) {
        return request(url, HttpMethod.DELETE);
    }

    /**
     * 创建 PATCH 请求构建器。
     *
     * @param url 请求 URL
     * @return 构建器实例
     */
    public static Builder patch(String url) {
        return request(url, HttpMethod.PATCH);
    }

    /**
     * 创建 HEAD 请求构建器。
     *
     * @param url 请求 URL
     * @return 构建器实例
     */
    public static Builder head(String url) {
        return request(url, HttpMethod.HEAD);
    }

    /**
     * 创建 OPTIONS 请求构建器。
     *
     * @param url 请求 URL
     * @return 构建器实例
     */
    public static Builder options(String url) {
        return request(url, HttpMethod.OPTIONS);
    }

    /**
     * 基于当前请求创建构建器。
     *
     * @return 构建器实例
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * 执行请求（同步）
     *
     * @return HTTP 响应
     */
    public HttpResponse execute() {
        OkHttpRequestExecutor executor = new OkHttpRequestExecutor(resolveConfig());
        return executor.execute(this);
    }

    /**
     * 以流式方式下载响应体到文件。
     * <p>
     * 该方法不会把完整响应体缓存在 {@link HttpResponse} 中，适合大文件或二进制下载场景。
     * </p>
     *
     * @param targetFile 目标文件
     * @return 仅包含状态码和响应头的 HTTP 响应
     */
    public HttpResponse downloadToFile(File targetFile) {
        OkHttpRequestExecutor executor = new OkHttpRequestExecutor(resolveConfig());
        return executor.download(this, targetFile);
    }

    /**
     * 执行请求（异步）
     *
     * @return CompletableFuture 包装的 HTTP 响应
     */
    public CompletableFuture<HttpResponse> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute);
    }

    private HttpConfig resolveConfig() {
        return config != null ? config : HttpConfig.defaultConfig();
    }

    private static int toIntMillis(Duration duration, String fieldName) {
        long millis = Objects.requireNonNull(duration, fieldName + " must not be null").toMillis();
        if (millis < 0L) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return Math.toIntExact(millis);
    }

    /**
     * HTTP 请求构建器
     */
    public static class Builder {
        private final HttpRequest request;

        public Builder() {
            this.request = new HttpRequest();
            this.request.headers = new HashMap<>();
            this.request.params = new HashMap<>();
            this.request.files = new HashMap<>();
            this.request.formParams = new HashMap<>();
        }

        private Builder(HttpRequest source) {
            this();
            HttpRequest copySource = Objects.requireNonNull(source, "source must not be null");
            this.request.url = copySource.url;
            this.request.method = copySource.method;
            if (copySource.headers != null) {
                this.request.headers.putAll(copySource.headers);
            }
            if (copySource.params != null) {
                this.request.params.putAll(copySource.params);
            }
            this.request.body = copySource.body;
            if (copySource.files != null) {
                this.request.files.putAll(copySource.files);
            }
            if (copySource.formParams != null) {
                this.request.formParams.putAll(copySource.formParams);
            }
            this.request.timeout = copySource.timeout;
            this.request.maxRetries = copySource.maxRetries;
            this.request.config = copySource.config;
        }

        /**
         * 设置请求 URL
         *
         * @param url 请求 URL
         * @return 构建器实例
         */
        public Builder url(String url) {
            request.url = url;
            return this;
        }

        /**
         * 设置请求方法
         *
         * @param method 请求方法
         * @return 构建器实例
         */
        public Builder method(HttpMethod method) {
            request.method = method;
            return this;
        }

        /**
         * 添加请求头
         *
         * @param name  请求头名称
         * @param value 请求头值
         * @return 构建器实例
         */
        public Builder header(String name, String value) {
            request.headers.put(name, value);
            return this;
        }

        /**
         * 批量添加请求头
         *
         * @param headers 请求头集合
         * @return 构建器实例
         */
        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                request.headers.putAll(headers);
            }
            return this;
        }

        /**
         * 添加 URL 参数
         *
         * @param name  参数名称
         * @param value 参数值
         * @return 构建器实例
         */
        public Builder param(String name, String value) {
            request.params.put(name, value);
            return this;
        }

        /**
         * 批量添加 URL 参数
         *
         * @param params 参数集合
         * @return 构建器实例
         */
        public Builder params(Map<String, String> params) {
            if (params != null) {
                request.params.putAll(params);
            }
            return this;
        }

        /**
         * 设置请求体
         *
         * @param body 请求体对象
         * @return 构建器实例
         */
        public Builder body(Object body) {
            request.body = body;
            return this;
        }

        /**
         * 添加上传文件
         *
         * @param name 文件参数名称
         * @param file 文件对象
         * @return 构建器实例
         */
        public Builder file(String name, File file) {
            request.files.put(name, file);
            return this;
        }

        /**
         * 添加表单参数
         *
         * @param name  参数名称
         * @param value 参数值
         * @return 构建器实例
         */
        public Builder formParam(String name, String value) {
            request.formParams.put(name, value);
            return this;
        }

        /**
         * 批量添加表单参数
         *
         * @param formParams 表单参数集合
         * @return 构建器实例
         */
        public Builder formParams(Map<String, String> formParams) {
            if (formParams != null) {
                request.formParams.putAll(formParams);
            }
            return this;
        }

        /**
         * 设置超时时间
         *
         * @param timeout 超时时间（毫秒）
         * @return 构建器实例
         */
        public Builder timeout(int timeout) {
            if (timeout < 0) {
                throw new IllegalArgumentException("timeout must not be negative");
            }
            request.timeout = timeout;
            return this;
        }

        /**
         * 设置超时时间。
         *
         * @param timeout 超时时间
         * @return 构建器实例
         */
        public Builder timeout(Duration timeout) {
            request.timeout = toIntMillis(timeout, "timeout");
            return this;
        }

        /**
         * 设置最大重试次数
         *
         * @param maxRetries 最大重试次数
         * @return 构建器实例
         */
        public Builder retry(int maxRetries) {
            request.maxRetries = maxRetries;
            return this;
        }

        /**
         * 设置 HTTP 配置
         *
         * @param config HTTP 配置
         * @return 构建器实例
         */
        public Builder config(HttpConfig config) {
            request.config = config;
            return this;
        }

        /**
         * 构建 HTTP 请求
         *
         * @return HTTP 请求对象
         */
        public HttpRequest build() {
            if (request.url == null || request.url.trim().isEmpty()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
            if (request.method == null) {
                request.method = HttpMethod.GET;
            }
            return request;
        }

        /**
         * 构建并同步执行请求。
         *
         * @return HTTP 响应
         */
        public HttpResponse execute() {
            return build().execute();
        }

        /**
         * 构建并异步执行请求。
         *
         * @return CompletableFuture 包装的 HTTP 响应
         */
        public CompletableFuture<HttpResponse> executeAsync() {
            return build().executeAsync();
        }

        /**
         * 构建请求并以流式方式下载响应体到文件。
         *
         * @param targetFile 目标文件
         * @return 仅包含状态码和响应头的 HTTP 响应
         */
        public HttpResponse downloadToFile(File targetFile) {
            return build().downloadToFile(targetFile);
        }
    }
}
