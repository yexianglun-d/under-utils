# Under-Utils Security

字段级加密与响应脱敏模块。

本模块不替代完整权限系统、密钥管理系统或数据库透明加密。它只提供业务系统里常见的显式字段加密、MyBatis TypeHandler 接入和响应字段脱敏。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-security</artifactId>
    <version>1.0.4</version>
</dependency>
```

Spring Boot 项目需要按配置创建加密器时使用：

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-security-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

## 字段加密

核心入口：

- `FieldEncryptor`
- `AesGcmFieldEncryptor`
- `KeyProvider`
- `StaticKeyProvider`

密文格式为：

```text
ENCv1:<keyId>:<base64url iv>:<base64url ciphertext>
```

示例：

```java
FieldEncryptor encryptor = new AesGcmFieldEncryptor(
        StaticKeyProvider.ofBase64("k1", base64Key)
);

String ciphertext = encryptor.encrypt("13812345678");
String plaintext = encryptor.decrypt(ciphertext);
```

AES key 必须是 16、24 或 32 字节，starter 配置使用 Base64 字符串。

## MyBatis 字段加密

MyBatis 加密通过显式 TypeHandler 生效，不做全局隐式加密。

```java
@TableName(value = "sys_user", autoResultMap = true)
public class SysUser {

    @TableField(value = "mobile", typeHandler = EncryptedStringTypeHandler.class)
    private String mobile;
}
```

Spring Boot starter 配置：

```yaml
under:
  utils:
    security:
      field-encryption:
        key-id: k1
        key: ${UNDER_UTILS_FIELD_AES_KEY_BASE64}
        enabled: true
        register-mybatis-type-handler: true
```

未配置 `key` 或显式设置 `enabled: false` 时 starter 不会创建默认 `FieldEncryptor`，`EncryptedStringTypeHandler` 也不会被注册默认加密器。

## 响应脱敏

```java
public class UserResponse {

    @Mask(type = MaskType.MOBILE_PHONE)
    private String phone;

    @Mask(type = MaskType.EMAIL)
    private String email;
}
```

`@Mask` 是新增 security API；`under-utils-spring` 中已有的 `@Sensitive` 继续保留兼容，不迁移包名。

## 边界

- 不在日志、异常或 `toString()` 中输出密钥。
- 不提供 KMS、轮换调度或密钥托管。
- 不承诺自动加密所有 MyBatis 字段，字段必须显式声明 TypeHandler。
- 非 `ENCv1:` 前缀的值解密时会原样返回，便于存量明文渐进迁移。
