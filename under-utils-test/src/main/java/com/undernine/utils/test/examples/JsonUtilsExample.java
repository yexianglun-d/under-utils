package com.undernine.utils.test.examples;

import com.fasterxml.jackson.core.type.TypeReference;
import com.undernine.utils.core.json.JsonUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JsonUtils 使用示例
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class JsonUtilsExample {

    public static void main(String[] args) {
        System.out.println("========== JsonUtils 使用示例 ==========\n");

        // 1. 对象转 JSON
        objectToJson();

        // 2. JSON 转对象
        jsonToObject();

        // 3. 集合操作
        collectionOperations();

        // 4. Map 操作
        mapOperations();

        // 5. 安全方法
        safeOperations();

        // 6. JSON 验证
        jsonValidation();

        // 7. 格式化 JSON
        prettyJson();

        // 8. Java 8 时间类型
        javaTimeTypes();
    }

    /**
     * 1. 对象转 JSON
     */
    private static void objectToJson() {
        System.out.println("1. 对象转 JSON");
        
        User user = new User("John Doe", 25, "john@example.com");
        String json = JsonUtils.toJson(user);
        
        System.out.println("User 对象: " + user);
        System.out.println("JSON 字符串: " + json);
        System.out.println();
    }

    /**
     * 2. JSON 转对象
     */
    private static void jsonToObject() {
        System.out.println("2. JSON 转对象");
        
        String json = "{\"name\":\"Jane Smith\",\"age\":23,\"email\":\"jane@example.com\"}";
        User user = JsonUtils.fromJson(json, User.class);
        
        System.out.println("JSON 字符串: " + json);
        System.out.println("User 对象: " + user);
        System.out.println();
    }

    /**
     * 3. 集合操作
     */
    private static void collectionOperations() {
        System.out.println("3. 集合操作");
        
        // List 转 JSON
        List<User> users = Arrays.asList(
            new User("John", 25, "john@example.com"),
            new User("Jane", 23, "jane@example.com"),
            new User("Bob", 30, "bob@example.com")
        );
        String json = JsonUtils.toJson(users);
        System.out.println("List 转 JSON: " + json);
        
        // JSON 转 List
        List<User> deserializedUsers = JsonUtils.fromJson(json, new TypeReference<List<User>>() {});
        System.out.println("JSON 转 List: " + deserializedUsers.size() + " 个用户");
        System.out.println();
    }

    /**
     * 4. Map 操作
     */
    private static void mapOperations() {
        System.out.println("4. Map 操作");
        
        // Map 转 JSON
        Map<String, Object> data = new HashMap<>();
        data.put("status", "success");
        data.put("code", 200);
        data.put("message", "操作成功");
        data.put("timestamp", System.currentTimeMillis());
        
        String json = JsonUtils.toJson(data);
        System.out.println("Map 转 JSON: " + json);
        
        // JSON 转 Map
        Map<String, Object> deserializedMap = JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});
        System.out.println("JSON 转 Map: " + deserializedMap);
        System.out.println();
    }

    /**
     * 5. 安全方法（不抛异常）
     */
    private static void safeOperations() {
        System.out.println("5. 安全方法（不抛异常）");
        
        // 安全的序列化
        User user = new User("Test", 20, "test@example.com");
        String json = JsonUtils.tryToJson(user);
        System.out.println("tryToJson 成功: " + (json != null));
        
        // 安全的反序列化 - 正常情况
        String validJson = "{\"name\":\"Valid\",\"age\":25,\"email\":\"valid@example.com\"}";
        User validUser = JsonUtils.tryFromJson(validJson, User.class);
        System.out.println("tryFromJson 成功: " + (validUser != null));
        
        // 安全的反序列化 - 异常情况
        String invalidJson = "{\"name\":\"Invalid\",\"age\":}";
        User invalidUser = JsonUtils.tryFromJson(invalidJson, User.class);
        System.out.println("tryFromJson 失败（返回 null）: " + (invalidUser == null));
        System.out.println();
    }

    /**
     * 6. JSON 验证
     */
    private static void jsonValidation() {
        System.out.println("6. JSON 验证");
        
        System.out.println("有效的 JSON 对象: " + JsonUtils.isValidJson("{\"name\":\"John\"}"));
        System.out.println("有效的 JSON 数组: " + JsonUtils.isValidJson("[1,2,3]"));
        System.out.println("有效的 JSON 字符串: " + JsonUtils.isValidJson("\"hello\""));
        System.out.println("有效的 JSON 数字: " + JsonUtils.isValidJson("123"));
        System.out.println("无效的 JSON: " + JsonUtils.isValidJson("{\"name\":}"));
        System.out.println("空字符串: " + JsonUtils.isValidJson(""));
        System.out.println();
    }

    /**
     * 7. 格式化 JSON
     */
    private static void prettyJson() {
        System.out.println("7. 格式化 JSON");
        
        User user = new User("John", 25, "john@example.com");
        String prettyJson = JsonUtils.toPrettyJson(user);
        
        System.out.println("格式化的 JSON:");
        System.out.println(prettyJson);
        System.out.println();
    }

    /**
     * 8. Java 8 时间类型支持
     */
    private static void javaTimeTypes() {
        System.out.println("8. Java 8 时间类型支持");
        
        UserWithTime user = new UserWithTime(
            "John",
            25,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30)
        );
        
        String json = JsonUtils.toJson(user);
        System.out.println("包含时间类型的对象: " + json);
        
        UserWithTime deserializedUser = JsonUtils.fromJson(json, UserWithTime.class);
        System.out.println("反序列化成功: " + (deserializedUser != null));
        System.out.println("创建时间: " + deserializedUser.getCreateTime());
        System.out.println();
    }

    // ==================== 测试实体类 ====================

    static class User {
        private String name;
        private Integer age;
        private String email;

        public User() {
        }

        public User(String name, Integer age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "', age=" + age + ", email='" + email + "'}";
        }
    }

    static class UserWithTime {
        private String name;
        private Integer age;
        private LocalDateTime createTime;
        private LocalDateTime expireTime;

        public UserWithTime() {
        }

        public UserWithTime(String name, Integer age, LocalDateTime createTime, LocalDateTime expireTime) {
            this.name = name;
            this.age = age;
            this.createTime = createTime;
            this.expireTime = expireTime;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(LocalDateTime expireTime) {
            this.expireTime = expireTime;
        }
    }
}
