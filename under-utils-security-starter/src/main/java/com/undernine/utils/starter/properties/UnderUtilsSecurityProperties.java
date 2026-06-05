package com.undernine.utils.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Under-Utils Security 自动配置属性。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
@ConfigurationProperties(prefix = "under.utils.security")
public class UnderUtilsSecurityProperties {

    private boolean enabled = true;
    private FieldEncryption fieldEncryption = new FieldEncryption();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public FieldEncryption getFieldEncryption() {
        return fieldEncryption;
    }

    public void setFieldEncryption(FieldEncryption fieldEncryption) {
        this.fieldEncryption = fieldEncryption;
    }

    /**
     * 字段级加密配置。
     */
    public static class FieldEncryption {
        private boolean enabled = true;
        private String keyId = "default";
        private String key;
        private boolean registerMybatisTypeHandler = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public boolean isRegisterMybatisTypeHandler() {
            return registerMybatisTypeHandler;
        }

        public void setRegisterMybatisTypeHandler(boolean registerMybatisTypeHandler) {
            this.registerMybatisTypeHandler = registerMybatisTypeHandler;
        }
    }
}
