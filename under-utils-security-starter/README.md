# Under-Utils Security Starter

Spring Boot 自动装配模块，用于按配置创建字段级加密器，并把它注册为 `EncryptedStringTypeHandler` 的默认加密器。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-security-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

## 配置

```yaml
under:
  utils:
    security:
      enabled: true
      field-encryption:
        enabled: true
        key-id: k1
        key: ${UNDER_UTILS_FIELD_AES_KEY_BASE64}
        register-mybatis-type-handler: true
```

`key` 必须是 Base64 编码的 AES key，解码后长度必须为 16、24 或 32 字节。未配置 key 或设置 `field-encryption.enabled=false` 时不会创建默认 `FieldEncryptor`，也不会注册 `EncryptedStringTypeHandler` 默认加密器。

## MyBatis 用法

```java
@TableName(value = "sys_user", autoResultMap = true)
public class SysUser {

    @TableField(value = "mobile", typeHandler = EncryptedStringTypeHandler.class)
    private String mobile;
}
```

本 starter 不会自动扫描实体字段，也不会全局加密所有字符串字段。字段必须显式声明 TypeHandler。
