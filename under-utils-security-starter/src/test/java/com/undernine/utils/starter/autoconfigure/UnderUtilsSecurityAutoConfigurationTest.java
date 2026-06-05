package com.undernine.utils.starter.autoconfigure;

import com.undernine.utils.security.crypto.AesGcmFieldEncryptor;
import com.undernine.utils.security.crypto.FieldEncryptor;
import com.undernine.utils.security.mybatis.EncryptedStringTypeHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class UnderUtilsSecurityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UnderUtilsSecurityAutoConfiguration.class));

    @AfterEach
    void tearDown() {
        EncryptedStringTypeHandler.clearDefaultFieldEncryptor();
    }

    @Test
    void shouldNotCreateFieldEncryptorWhenKeyIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(FieldEncryptor.class);
            assertThat(context).doesNotHaveBean(
                    UnderUtilsSecurityAutoConfiguration.EncryptedStringTypeHandlerRegistration.class);
        });
    }

    @Test
    void shouldCreateAesGcmFieldEncryptorWhenKeyIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "under.utils.security.field-encryption.key-id=k1",
                        "under.utils.security.field-encryption.key=" + base64Key()
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(FieldEncryptor.class);
                    assertThat(context.getBean(FieldEncryptor.class)).isInstanceOf(AesGcmFieldEncryptor.class);
                    assertThat(context).hasSingleBean(
                            UnderUtilsSecurityAutoConfiguration.EncryptedStringTypeHandlerRegistration.class);
                });
    }

    @Test
    void shouldBackOffWhenUserFieldEncryptorExists() {
        FieldEncryptor customEncryptor = new FieldEncryptor() {
            @Override
            public String encrypt(String plaintext) {
                return "encrypted:" + plaintext;
            }

            @Override
            public String decrypt(String ciphertext) {
                return ciphertext.replace("encrypted:", "");
            }
        };

        contextRunner
                .withBean(FieldEncryptor.class, () -> customEncryptor)
                .withPropertyValues("under.utils.security.field-encryption.key=" + base64Key())
                .run(context -> {
                    assertThat(context).hasSingleBean(FieldEncryptor.class);
                    assertThat(context.getBean(FieldEncryptor.class)).isSameAs(customEncryptor);
                    assertThat(context).hasSingleBean(
                            UnderUtilsSecurityAutoConfiguration.EncryptedStringTypeHandlerRegistration.class);
                });
    }

    @Test
    void shouldDisableSecurityAutoConfigurationWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "under.utils.security.enabled=false",
                        "under.utils.security.field-encryption.key=" + base64Key()
                )
                .run(context -> assertThat(context).doesNotHaveBean(FieldEncryptor.class));
    }

    @Test
    void shouldDisableFieldEncryptionWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "under.utils.security.field-encryption.enabled=false",
                        "under.utils.security.field-encryption.key=" + base64Key()
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FieldEncryptor.class);
                    assertThat(context).doesNotHaveBean(
                            UnderUtilsSecurityAutoConfiguration.EncryptedStringTypeHandlerRegistration.class);
                });
    }

    @Test
    void shouldNotRegisterMybatisTypeHandlerWhenFieldEncryptionDisabled() {
        FieldEncryptor customEncryptor = new FieldEncryptor() {
            @Override
            public String encrypt(String plaintext) {
                return plaintext;
            }

            @Override
            public String decrypt(String ciphertext) {
                return ciphertext;
            }
        };

        contextRunner
                .withBean(FieldEncryptor.class, () -> customEncryptor)
                .withPropertyValues("under.utils.security.field-encryption.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(FieldEncryptor.class);
                    assertThat(context).doesNotHaveBean(
                            UnderUtilsSecurityAutoConfiguration.EncryptedStringTypeHandlerRegistration.class);
                });
    }

    @Test
    void shouldAllowMybatisTypeHandlerRegistrationToBeDisabled() {
        contextRunner
                .withPropertyValues(
                        "under.utils.security.field-encryption.key=" + base64Key(),
                        "under.utils.security.field-encryption.register-mybatis-type-handler=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(FieldEncryptor.class);
                    assertThat(context).doesNotHaveBean(
                            UnderUtilsSecurityAutoConfiguration.EncryptedStringTypeHandlerRegistration.class);
                });
    }

    private static String base64Key() {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(key);
    }
}
