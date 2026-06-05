package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.security.crypto.AesGcmFieldEncryptor;
import com.undernine.utils.security.crypto.FieldEncryptor;
import com.undernine.utils.security.crypto.StaticKeyProvider;
import com.undernine.utils.security.mybatis.EncryptedStringTypeHandler;
import com.undernine.utils.starter.properties.UnderUtilsSecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Under-Utils Security 自动配置入口。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
@AutoConfiguration
@EnableConfigurationProperties(UnderUtilsSecurityProperties.class)
@ConditionalOnClass(FieldEncryptor.class)
@ConditionalOnProperty(prefix = "under.utils.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UnderUtilsSecurityAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "under.utils.security.field-encryption",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    static class FieldEncryptionConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "under.utils.security.field-encryption", name = "key")
        public FieldEncryptor fieldEncryptor(UnderUtilsSecurityProperties properties) {
            UnderUtilsSecurityProperties.FieldEncryption fieldEncryption = properties.getFieldEncryption();
            return new AesGcmFieldEncryptor(
                    StaticKeyProvider.ofBase64(fieldEncryption.getKeyId(), fieldEncryption.getKey())
            );
        }

        @Bean
        @ConditionalOnBean(FieldEncryptor.class)
        @ConditionalOnClass(EncryptedStringTypeHandler.class)
        @ConditionalOnProperty(prefix = "under.utils.security.field-encryption",
                name = "register-mybatis-type-handler",
                havingValue = "true",
                matchIfMissing = true)
        public EncryptedStringTypeHandlerRegistration encryptedStringTypeHandlerRegistration(
                FieldEncryptor fieldEncryptor) {
            return new EncryptedStringTypeHandlerRegistration(fieldEncryptor);
        }
    }

    /**
     * 注册 MyBatis 加密 TypeHandler 默认加密器。
     */
    public static final class EncryptedStringTypeHandlerRegistration implements AutoCloseable {

        private final FieldEncryptor fieldEncryptor;
        private final Object ownerToken;

        private EncryptedStringTypeHandlerRegistration(FieldEncryptor fieldEncryptor) {
            this.fieldEncryptor = fieldEncryptor;
            this.ownerToken = EncryptedStringTypeHandler.registerDefaultFieldEncryptor(fieldEncryptor);
        }

        public FieldEncryptor getFieldEncryptor() {
            return fieldEncryptor;
        }

        @Override
        public void close() {
            EncryptedStringTypeHandler.clearDefaultFieldEncryptor(ownerToken);
        }
    }
}
