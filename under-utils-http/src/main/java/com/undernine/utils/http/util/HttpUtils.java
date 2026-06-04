package com.undernine.utils.http.util;

import com.undernine.utils.http.config.HttpConfig;
import com.undernine.utils.http.enums.HttpMethod;
import com.undernine.utils.http.request.HttpRequest;
import com.undernine.utils.http.response.HttpResponse;

import java.io.File;
import java.util.Map;

/**
 * HTTP 工具类
 * <p>
 * 提供便捷的 HTTP 请求方法。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // GET 请求
 * String result = HttpUtils.get("https://api.example.com/users");
 *
 * // POST JSON 请求
 * User user = new User("John", 25);
 * String result = HttpUtils.postJson("https://api.example.com/users", user);
 *
 * // 文件上传
 * File file = new File("/path/to/file.jpg");
 * String result = HttpUtils.upload("https://api.example.com/upload", "file", file);
 * }</pre>
 * </p>
 *
 * @author deng
 * @version 1.0.0
 * @since 1.0.0
 */
public final class HttpUtils {

    /**
     * 默认配置
     */
    private static HttpConfig defaultConfig = HttpConfig.defaultConfig();

    /**
     * 私有构造方法，防止实例化
     *
     * @throws UnsupportedOperationException 如果尝试实例化此类
     */
    private HttpUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 设置全局默认配置
     *
     * @param config HTTP 配置
     */
    public static void setDefaultConfig(HttpConfig config) {
        defaultConfig = config;
    }

    /**
     * 获取全局默认配置
     *
     * @return HTTP 配置
     */
    public static HttpConfig getDefaultConfig() {
        return defaultConfig;
    }

    // ==================== GET 请求 ====================

    /**
     * 发送 GET 请求
     *
     * @param url 请求 URL
     * @return 响应体字符串
     */
    public static String get(String url) {
        HttpResponse response = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.GET)
                .config(defaultConfig)
                .build()
                .execute();
        return response.asString();
    }

    /**
     * 发送 GET 请求（带参数）
     *
     * @param url    请求 URL
     * @param params URL 参数
     * @return 响应体字符串
     */
    public static String get(String url, Map<String, String> params) {
        HttpResponse response = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.GET)
                .params(params)
                .config(defaultConfig)
                .build()
                .execute();
        return response.asString();
    }

    /**
     * 发送 GET 请求（带请求头）
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @return 响应体字符串
     */
    public static String get(String url, Map<String, String> params, Map<String, String> headers) {
        HttpResponse response = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.GET)
                .params(params)
                .headers(headers)
                .config(defaultConfig)
                .build()
                .execute();
        return response.asString();
    }

    /**
     * 发送 GET 请求并返回完整响应
     *
     * @param url 请求 URL
     * @return HTTP 响应对象
     */
    public static HttpResponse getResponse(String url) {
        return HttpRequest.builder()
                .url(url)
                .method(HttpMethod.GET)
                .config(defaultConfig)
                .build()
                .execute();
    }

    // ==================== POST 请求 ====================

    /**
     * 发送 POST 请求（JSON 格式）
     *
     * @param url  请求 URL
     * @param body 请求体对象
     * @return 响应体字符串
     */
    public static String postJson(String url, Object body) {
        HttpResponse response = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.POST)
                .header("Content-Type", "application/json")
                .body(body)
                .config(defaultConfig)
                .build()
                .execute();
        return response.asString();
    }

    /**
     * 发送 POST 请求（表单格式）
     *
     * @param url        请求 URL
     * @param formParams 表单参数
     * @return 响应体字符串
     */
    public static String postForm(String url, Map<String, String> formParams) {
        HttpResponse response = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.POST)
                .formParams(formParams)
                .config(defaultConfig)
                .build()
                .execute();
        return response.asString();
    }

    /**
     * 发送 POST 请求并返回完整响应
     *
     * @param url  请求 URL
     * @param body 请求体对象
     * @return HTTP 响应对象
     */
    public static HttpResponse postResponse(String url, Object body) {
        return HttpRequest.builder()
                .url(url)
                .method(HttpMethod.POST)
                .header("Content-Type", "application/json")
                .body(body)
                .config(defaultConfig)
                .build()
                .execute();
    }

    // ==================== PUT 请求 ====================

    /**
     * 发送 PUT 请求（JSON 格式）
     *
     * @param url  请求 URL
     * @param body 请求体对象
     * @return 响应体字符串
     */
    public static String putJson(String url, Object body) {
        HttpResponse response = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.PUT)
                .header("Content-Type", "application/json")
                .body(body)
                .config(defaultConfig)
                .build()
                .execute();
        return response.asString();
    }

    /**
     * 发送 PUT 请求并返回完整响应
     *
     * @param url  请求 URL
     * @param body 请求体对象
     * @return HTTP 响应对象
     */
    public static HttpResponse putResponse(String url, Object body) {
        return HttpRequest.builder()
                .url(url)
                .method(HttpMethod.PUT)
                .header("Content-Type", "application/json")
                .body(body)
                .config(defaultConfig)
                .build()
                .execute();
    }

    // ==================== DELETE 请求 ====================

    /**
     * 发送 DELETE 请求
     *
     * @param url 请求 URL
     * @return 响应体字符串
     */
    public static String delete(String url) {
        HttpResponse response = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.DELETE)
                .config(defaultConfig)
                .build()
                .execute();
        return response.asString();
    }

    /**
     * 发送 DELETE 请求并返回完整响应
     *
     * @param url 请求 URL
     * @return HTTP 响应对象
     */
    public static HttpResponse deleteResponse(String url) {
        return HttpRequest.builder()
                .url(url)
                .method(HttpMethod.DELETE)
                .config(defaultConfig)
                .build()
                .execute();
    }

    // ==================== 文件操作 ====================

    /**
     * 上传文件
     *
     * @param url  请求 URL
     * @param name 文件参数名称
     * @param file 文件对象
     * @return 响应体字符串
     */
    public static String upload(String url, String name, File file) {
        HttpResponse response = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.POST)
                .file(name, file)
                .config(defaultConfig)
                .build()
                .execute();
        return response.asString();
    }

    /**
     * 上传文件（带额外参数）
     *
     * @param url        请求 URL
     * @param name       文件参数名称
     * @param file       文件对象
     * @param formParams 额外的表单参数
     * @return 响应体字符串
     */
    public static String upload(String url, String name, File file, Map<String, String> formParams) {
        HttpResponse response = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.POST)
                .file(name, file)
                .formParams(formParams)
                .config(defaultConfig)
                .build()
                .execute();
        return response.asString();
    }

    /**
     * 下载文件
     *
     * @param url        请求 URL
     * @param targetFile 目标文件
     */
    public static void download(String url, File targetFile) {
        HttpRequest.builder()
                .url(url)
                .method(HttpMethod.GET)
                .config(defaultConfig)
                .build()
                .downloadToFile(targetFile);
    }

    // ==================== URL 工具方法 ====================

    /**
     * URL 编码
     *
     * @param value 待编码的字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("URL encode failed", e);
        }
    }

    /**
     * URL 解码
     *
     * @param value 待解码的字符串
     * @return 解码后的字符串
     */
    public static String urlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("URL decode failed", e);
        }
    }
}
