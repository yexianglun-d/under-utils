/**
 * Spring 拦截器包
 * <p>
 * 提供 HTTP 请求拦截器，用于请求日志记录、性能监控等功能。
 * </p>
 *
 * <h2>核心拦截器</h2>
 * <ul>
 *     <li>{@link com.undernine.utils.spring.interceptor.RequestLogInterceptor} - HTTP 请求日志拦截器</li>
 * </ul>
 *
 * <h2>功能说明</h2>
 *
 * <h3>HTTP 请求日志拦截器</h3>
 * <p>自动记录所有 HTTP 请求的详细信息：</p>
 * <ul>
 *     <li>请求方法（GET、POST、PUT、DELETE 等）</li>
 *     <li>请求 URI</li>
 *     <li>客户端 IP 地址（默认使用 remote address；显式信任代理 Header 后支持代理）</li>
 *     <li>请求头信息（DEBUG 级别）</li>
 *     <li>响应状态码</li>
 *     <li>请求耗时</li>
 *     <li>异常信息（如果有）</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Configuration
 * public class WebMvcConfig implements WebMvcConfigurer {
 *
 *     @Override
 *     public void addInterceptors(InterceptorRegistry registry) {
 *         registry.addInterceptor(new RequestLogInterceptor())
 *                 .addPathPatterns("/**")
 *                 .excludePathPatterns("/static/**", "/error");
 *     }
 * }
 * }</pre>
 *
 * <h2>日志输出示例</h2>
 * <pre>
 * INFO  【HTTP请求】POST /api/users 来自IP: 192.168.1.100
 * DEBUG 【请求头】{Content-Type=application/json, Authorization=******}
 * INFO  【HTTP响应】POST /api/users - 状态码: 200, 耗时: 125ms
 * </pre>
 *
 * <h2>配置说明</h2>
 * <ul>
 *     <li>拦截器需要在 WebMvcConfigurer 中注册</li>
 *     <li>可通过 addPathPatterns 配置拦截路径</li>
 *     <li>可通过 excludePathPatterns 配置排除路径</li>
 *     <li>请求头信息仅在 DEBUG 级别输出，敏感 Header 会被遮蔽</li>
 * </ul>
 *
 * <h2>IP 获取策略</h2>
 * <p>默认只使用 {@code request.getRemoteAddr()}。如果服务部署在可信网关之后，且网关会清洗
 * {@code X-Forwarded-For} / {@code X-Real-IP}，可以使用 {@code new RequestLogInterceptor(true)}
 * 显式开启代理 Header 读取。开启后拦截器会按以下顺序获取客户端真实 IP：</p>
 * <ol>
 *     <li>X-Forwarded-For 请求头（代理服务器）</li>
 *     <li>X-Real-IP 请求头（Nginx 等）</li>
 *     <li>request.getRemoteAddr()（直连）</li>
 * </ol>
 *
 * <h2>注意事项</h2>
 * <ul>
 *     <li>拦截器会拦截所有匹配的请求，注意性能影响</li>
 *     <li>静态资源建议排除，避免产生大量日志</li>
 *     <li>生产环境建议关闭 DEBUG 级别日志</li>
 *     <li>如需记录请求体和响应体，需要使用 ContentCachingRequestWrapper</li>
 * </ul>
 *
 * <h2>扩展建议</h2>
 * <ul>
 *     <li>将日志持久化到数据库或日志系统</li>
 *     <li>添加请求体和响应体的记录（注意性能）</li>
 *     <li>集成链路追踪（TraceId）</li>
 *     <li>添加慢请求告警机制</li>
 * </ul>
 *
 * @author deng
 * @version 1.0.0
 * @since 1.0.0
 */
package com.undernine.utils.spring.interceptor;
