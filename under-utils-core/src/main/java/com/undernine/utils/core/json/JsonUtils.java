package com.undernine.utils.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON 工具类。
 * <p>
 * 该类仅保留为兼容维护 API。它内置单例 {@link ObjectMapper}，适合作为轻量历史调用入口；
 * 新代码应优先使用应用自身配置的 {@link ObjectMapper}、消息 codec 或边界更明确的序列化组件。
 * </p>
 * <p>
 * 特性：
 * <ul>
 *   <li>支持 Java 8 时间类型（LocalDateTime、LocalDate 等）</li>
 *   <li>忽略未知属性，避免反序列化失败</li>
 *   <li>线程安全，ObjectMapper 使用单例模式</li>
 *   <li>提供安全方法（try 开头），失败返回 null 而不抛异常</li>
 * </ul>
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 * @deprecated 历史 JSON 工具保留为兼容 API，不作为 Under-Utils 后续工程模式主线能力演进。
 */
@Deprecated(since = "1.0.0")
public final class JsonUtils {

    /**
     * ObjectMapper 单例（线程安全）
     */
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    /**
     * 私有构造方法，防止实例化
     *
     * @throws UnsupportedOperationException 如果尝试实例化此类
     */
    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 创建并配置 ObjectMapper
     *
     * @return 配置好的 ObjectMapper 实例
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 禁用将日期序列化为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略未知属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        // 忽略空对象
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        return mapper;
    }

    /**
     * 将对象序列化为 JSON 字符串。
     * <p>
     * 使用示例：
     * <pre>{@code
     * User user = new User("John", 25);
     * String json = JsonUtils.toJson(user);
     * // 结果：{"name":"John","age":25}
     * }</pre>
     * </p>
     *
     * @param obj 待序列化的对象
     * @return JSON 字符串，如果输入为 null 则返回 "null"
     * @throws JsonException 如果序列化失败
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to serialize object to JSON: " + obj.getClass().getName(), e);
        }
    }

    /**
     * 将对象序列化为格式化的 JSON 字符串（带缩进）。
     * <p>
     * 使用示例：
     * <pre>{@code
     * User user = new User("John", 25);
     * String json = JsonUtils.toPrettyJson(user);
     * // 结果：
     * // {
     * //   "name" : "John",
     * //   "age" : 25
     * // }
     * }</pre>
     * </p>
     *
     * @param obj 待序列化的对象
     * @return 格式化的 JSON 字符串，如果输入为 null 则返回 "null"
     * @throws JsonException 如果序列化失败
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to serialize object to pretty JSON: " + obj.getClass().getName(), e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象。
     * <p>
     * 使用示例：
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":25}";
     * User user = JsonUtils.fromJson(json, User.class);
     * }</pre>
     * </p>
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的对象，如果输入为 null 或空字符串则返回 null
     * @throws JsonException 如果反序列化失败
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to "
                    + clazz.getName() + " (" + inputSummary(json) + ")", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象（支持泛型）。
     * <p>
     * 使用示例：
     * <pre>{@code
     * String json = "[{\"name\":\"John\",\"age\":25},{\"name\":\"Jane\",\"age\":23}]";
     * List<User> users = JsonUtils.fromJson(json, new TypeReference<List<User>>() {});
     * }</pre>
     * </p>
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用（用于泛型）
     * @param <T>           泛型类型
     * @return 反序列化后的对象，如果输入为 null 或空字符串则返回 null
     * @throws JsonException 如果反序列化失败
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to "
                    + typeReference.getType() + " (" + inputSummary(json) + ")", e);
        }
    }

    private static String inputSummary(String json) {
        return "inputLength=" + json.length();
    }

    /**
     * 安全地将对象序列化为 JSON 字符串，失败时返回 null。
     * <p>
     * 使用示例：
     * <pre>{@code
     * User user = new User("John", 25);
     * String json = JsonUtils.tryToJson(user);
     * if (json == null) {
     *     System.out.println("序列化失败");
     * }
     * }</pre>
     * </p>
     *
     * @param obj 待序列化的对象
     * @return JSON 字符串，失败时返回 null
     */
    public static String tryToJson(Object obj) {
        try {
            return toJson(obj);
        } catch (JsonException e) {
            return null;
        }
    }

    /**
     * 安全地将 JSON 字符串反序列化为指定类型的对象，失败时返回 null。
     * <p>
     * 使用示例：
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":25}";
     * User user = JsonUtils.tryFromJson(json, User.class);
     * if (user == null) {
     *     System.out.println("反序列化失败");
     * }
     * }</pre>
     * </p>
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的对象，失败时返回 null
     */
    public static <T> T tryFromJson(String json, Class<T> clazz) {
        try {
            return fromJson(json, clazz);
        } catch (JsonException e) {
            return null;
        }
    }

    /**
     * 安全地将 JSON 字符串反序列化为指定类型的对象（支持泛型），失败时返回 null。
     * <p>
     * 使用示例：
     * <pre>{@code
     * String json = "[{\"name\":\"John\",\"age\":25}]";
     * List<User> users = JsonUtils.tryFromJson(json, new TypeReference<List<User>>() {});
     * if (users == null) {
     *     System.out.println("反序列化失败");
     * }
     * }</pre>
     * </p>
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用（用于泛型）
     * @param <T>           泛型类型
     * @return 反序列化后的对象，失败时返回 null
     */
    public static <T> T tryFromJson(String json, TypeReference<T> typeReference) {
        try {
            return fromJson(json, typeReference);
        } catch (JsonException e) {
            return null;
        }
    }

    /**
     * 判断字符串是否为有效的 JSON 格式。
     * <p>
     * 使用示例：
     * <pre>{@code
     * boolean valid = JsonUtils.isValidJson("{\"name\":\"John\"}");
     * // 结果：true
     * }</pre>
     * </p>
     *
     * @param json 待判断的字符串
     * @return 如果是有效的 JSON 格式返回 true，否则返回 false
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 获取 ObjectMapper 实例（用于高级定制）。
     * <p>
     * 注意：请勿修改返回的 ObjectMapper 配置，以免影响全局行为。
     * </p>
     *
     * @return ObjectMapper 实例
     * @deprecated 返回的是全局共享实例，修改配置会影响所有调用；新代码应注入业务自己的 ObjectMapper。
     */
    @Deprecated(since = "1.0.0")
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
