package com.undernine.utils.core.json;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * JsonUtils 测试类
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
class JsonUtilsTest {

    // ==================== 测试实体类 ====================

    static class User {
        private String name;
        private Integer age;
        private LocalDateTime createTime;

        public User() {
        }

        public User(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public User(String name, Integer age, LocalDateTime createTime) {
            this.name = name;
            this.age = age;
            this.createTime = createTime;
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
    }

    // ==================== toJson() 测试 ====================

    @Test
    void testToJson_simple() {
        User user = new User("John", 25);
        String json = JsonUtils.toJson(user);
        
        assertThat(json).isNotNull();
        assertThat(json).contains("\"name\":\"John\"");
        assertThat(json).contains("\"age\":25");
    }

    @Test
    void testToJson_withLocalDateTime() {
        LocalDateTime now = LocalDateTime.of(2024, 12, 6, 10, 30, 0);
        User user = new User("John", 25, now);
        String json = JsonUtils.toJson(user);
        
        assertThat(json).contains("\"createTime\":");
        assertThat(json).contains("2024-12-06");
    }

    @Test
    void testToJson_null() {
        String json = JsonUtils.toJson(null);
        assertThat(json).isEqualTo("null");
    }

    @Test
    void testToJson_list() {
        List<User> users = Arrays.asList(
            new User("John", 25),
            new User("Jane", 23)
        );
        String json = JsonUtils.toJson(users);
        
        assertThat(json).startsWith("[");
        assertThat(json).endsWith("]");
        assertThat(json).contains("John");
        assertThat(json).contains("Jane");
    }

    @Test
    void testToJson_map() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 25);
        map.put("active", true);
        
        String json = JsonUtils.toJson(map);
        
        assertThat(json).contains("\"name\":\"John\"");
        assertThat(json).contains("\"age\":25");
        assertThat(json).contains("\"active\":true");
    }

    // ==================== toPrettyJson() 测试 ====================

    @Test
    void testToPrettyJson() {
        User user = new User("John", 25);
        String json = JsonUtils.toPrettyJson(user);
        
        assertThat(json).contains("\n");
        assertThat(json).contains("\"name\" : \"John\"");
    }

    @Test
    void testToPrettyJson_null() {
        String json = JsonUtils.toPrettyJson(null);
        assertThat(json).isEqualTo("null");
    }

    // ==================== fromJson(String, Class) 测试 ====================

    @Test
    void testFromJson_simple() {
        String json = "{\"name\":\"John\",\"age\":25}";
        User user = JsonUtils.fromJson(json, User.class);
        
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("John");
        assertThat(user.getAge()).isEqualTo(25);
    }

    @Test
    void testFromJson_withLocalDateTime() {
        String json = "{\"name\":\"John\",\"age\":25,\"createTime\":\"2024-12-06T10:30:00\"}";
        User user = JsonUtils.fromJson(json, User.class);
        
        assertThat(user).isNotNull();
        assertThat(user.getCreateTime()).isNotNull();
        assertThat(user.getCreateTime().getYear()).isEqualTo(2024);
    }

    @Test
    void testFromJson_null() {
        User user = JsonUtils.fromJson(null, User.class);
        assertThat(user).isNull();
    }

    @Test
    void testFromJson_emptyString() {
        User user = JsonUtils.fromJson("", User.class);
        assertThat(user).isNull();
    }

    @Test
    void testFromJson_blankString() {
        User user = JsonUtils.fromJson("   ", User.class);
        assertThat(user).isNull();
    }

    @Test
    void testFromJson_ignoreUnknownProperty() {
        // JSON 中包含未知属性 "email"，应该被忽略
        String json = "{\"name\":\"John\",\"age\":25,\"email\":\"john@example.com\"}";
        User user = JsonUtils.fromJson(json, User.class);
        
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("John");
        assertThat(user.getAge()).isEqualTo(25);
    }

    @Test
    void testFromJson_invalidJson() {
        String invalidJson = "{\"name\":\"John\",\"age\":}";
        
        assertThatThrownBy(() -> JsonUtils.fromJson(invalidJson, User.class))
            .isInstanceOf(JsonException.class)
            .hasMessageContaining("Failed to deserialize JSON");
    }

    @Test
    void testFromJson_invalidJsonDoesNotExposePayload() {
        String invalidJson = "{\"password\":\"secret-token\",\"age\":}";

        Throwable throwable = catchThrowable(() -> JsonUtils.fromJson(invalidJson, User.class));

        assertThat(throwable)
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("inputLength=")
                .hasMessageNotContaining("secret-token")
                .hasMessageNotContaining(invalidJson);
    }

    // ==================== fromJson(String, TypeReference) 测试 ====================

    @Test
    void testFromJson_list() {
        String json = "[{\"name\":\"John\",\"age\":25},{\"name\":\"Jane\",\"age\":23}]";
        List<User> users = JsonUtils.fromJson(json, new TypeReference<List<User>>() {});
        
        assertThat(users).isNotNull();
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getName()).isEqualTo("John");
        assertThat(users.get(1).getName()).isEqualTo("Jane");
    }

    @Test
    void testFromJson_map() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        Map<String, String> map = JsonUtils.fromJson(json, new TypeReference<Map<String, String>>() {});
        
        assertThat(map).isNotNull();
        assertThat(map).hasSize(2);
        assertThat(map.get("key1")).isEqualTo("value1");
        assertThat(map.get("key2")).isEqualTo("value2");
    }

    @Test
    void testFromJson_typeReference_null() {
        List<User> users = JsonUtils.fromJson(null, new TypeReference<List<User>>() {});
        assertThat(users).isNull();
    }

    @Test
    void testFromJson_typeReferenceInvalidJsonDoesNotExposePayload() {
        String invalidJson = "[{\"password\":\"secret-token\",\"age\":}]";

        Throwable throwable = catchThrowable(() ->
                JsonUtils.fromJson(invalidJson, new TypeReference<List<User>>() {}));

        assertThat(throwable)
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("inputLength=")
                .hasMessageNotContaining("secret-token")
                .hasMessageNotContaining(invalidJson);
    }

    // ==================== tryToJson() 测试 ====================

    @Test
    void testTryToJson_success() {
        User user = new User("John", 25);
        String json = JsonUtils.tryToJson(user);
        
        assertThat(json).isNotNull();
        assertThat(json).contains("John");
    }

    @Test
    void testTryToJson_null() {
        String json = JsonUtils.tryToJson(null);
        assertThat(json).isEqualTo("null");
    }

    // ==================== tryFromJson() 测试 ====================

    @Test
    void testTryFromJson_success() {
        String json = "{\"name\":\"John\",\"age\":25}";
        User user = JsonUtils.tryFromJson(json, User.class);
        
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("John");
    }

    @Test
    void testTryFromJson_invalidJson() {
        String invalidJson = "{\"name\":\"John\",\"age\":}";
        User user = JsonUtils.tryFromJson(invalidJson, User.class);
        
        assertThat(user).isNull();
    }

    @Test
    void testTryFromJson_null() {
        User user = JsonUtils.tryFromJson(null, User.class);
        assertThat(user).isNull();
    }

    @Test
    void testTryFromJson_typeReference_success() {
        String json = "[{\"name\":\"John\",\"age\":25}]";
        List<User> users = JsonUtils.tryFromJson(json, new TypeReference<List<User>>() {});
        
        assertThat(users).isNotNull();
        assertThat(users).hasSize(1);
    }

    @Test
    void testTryFromJson_typeReference_invalidJson() {
        String invalidJson = "[{\"name\":\"John\",\"age\":}]";
        List<User> users = JsonUtils.tryFromJson(invalidJson, new TypeReference<List<User>>() {});
        
        assertThat(users).isNull();
    }

    // ==================== isValidJson() 测试 ====================

    @Test
    void testIsValidJson_validObject() {
        String json = "{\"name\":\"John\",\"age\":25}";
        assertThat(JsonUtils.isValidJson(json)).isTrue();
    }

    @Test
    void testIsValidJson_validArray() {
        String json = "[{\"name\":\"John\"},{\"name\":\"Jane\"}]";
        assertThat(JsonUtils.isValidJson(json)).isTrue();
    }

    @Test
    void testIsValidJson_validString() {
        String json = "\"hello\"";
        assertThat(JsonUtils.isValidJson(json)).isTrue();
    }

    @Test
    void testIsValidJson_validNumber() {
        String json = "123";
        assertThat(JsonUtils.isValidJson(json)).isTrue();
    }

    @Test
    void testIsValidJson_validBoolean() {
        String json = "true";
        assertThat(JsonUtils.isValidJson(json)).isTrue();
    }

    @Test
    void testIsValidJson_validNull() {
        String json = "null";
        assertThat(JsonUtils.isValidJson(json)).isTrue();
    }

    @Test
    void testIsValidJson_invalid() {
        String json = "{\"name\":\"John\",\"age\":}";
        assertThat(JsonUtils.isValidJson(json)).isFalse();
    }

    @Test
    void testIsValidJson_null() {
        assertThat(JsonUtils.isValidJson(null)).isFalse();
    }

    @Test
    void testIsValidJson_empty() {
        assertThat(JsonUtils.isValidJson("")).isFalse();
    }

    @Test
    void testIsValidJson_blank() {
        assertThat(JsonUtils.isValidJson("   ")).isFalse();
    }

    // ==================== getObjectMapper() 测试 ====================

    @Test
    void testGetObjectMapper() {
        assertThat(JsonUtils.getObjectMapper()).isNotNull();
    }

    // ==================== 边界测试 ====================

    @Test
    void testToJson_emptyObject() {
        User user = new User();
        String json = JsonUtils.toJson(user);
        
        assertThat(json).isNotNull();
        assertThat(json).contains("null");
    }

    @Test
    void testFromJson_emptyObject() {
        String json = "{}";
        User user = JsonUtils.fromJson(json, User.class);
        
        assertThat(user).isNotNull();
        assertThat(user.getName()).isNull();
        assertThat(user.getAge()).isNull();
    }

    @Test
    void testRoundTrip() {
        // 序列化 -> 反序列化 -> 应该相等
        User original = new User("John", 25);
        String json = JsonUtils.toJson(original);
        User deserialized = JsonUtils.fromJson(json, User.class);
        
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getName()).isEqualTo(original.getName());
        assertThat(deserialized.getAge()).isEqualTo(original.getAge());
    }
}
