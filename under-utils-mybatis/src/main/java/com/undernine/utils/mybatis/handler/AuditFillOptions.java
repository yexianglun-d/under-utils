package com.undernine.utils.mybatis.handler;

import java.util.Objects;

/**
 * MyBatis-Plus 审计字段填充配置。
 * <p>
 * 默认字段名与 {@code BaseEntity} 保持一致；字段命名不同的项目可以在注册
 * {@link DefaultMetaObjectHandler} 时显式覆盖。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.3
 */
public final class AuditFillOptions {

    private static final String DEFAULT_CREATE_TIME_FIELD = "createTime";
    private static final String DEFAULT_UPDATE_TIME_FIELD = "updateTime";
    private static final String DEFAULT_CREATE_BY_FIELD = "createBy";
    private static final String DEFAULT_UPDATE_BY_FIELD = "updateBy";
    private static final String DEFAULT_DELETED_FIELD = "deleted";
    private static final Object DEFAULT_NOT_DELETED_VALUE = 0;

    private static final AuditFillOptions DEFAULT_OPTIONS = builder().build();

    private final String createTimeField;
    private final String updateTimeField;
    private final String createByField;
    private final String updateByField;
    private final String deletedField;
    private final Object notDeletedValue;
    private final boolean fillDeleted;

    private AuditFillOptions(Builder builder) {
        this.createTimeField = requireText(builder.createTimeField, "createTimeField");
        this.updateTimeField = requireText(builder.updateTimeField, "updateTimeField");
        this.createByField = requireText(builder.createByField, "createByField");
        this.updateByField = requireText(builder.updateByField, "updateByField");
        this.deletedField = requireText(builder.deletedField, "deletedField");
        this.notDeletedValue = Objects.requireNonNull(builder.notDeletedValue, "notDeletedValue must not be null");
        this.fillDeleted = builder.fillDeleted;
    }

    /**
     * 默认填充配置。
     *
     * @return 默认配置
     */
    public static AuditFillOptions defaults() {
        return DEFAULT_OPTIONS;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 基于当前配置创建构建器。
     *
     * @return 构建器
     */
    public Builder toBuilder() {
        return builder()
                .createTimeField(createTimeField)
                .updateTimeField(updateTimeField)
                .createByField(createByField)
                .updateByField(updateByField)
                .deletedField(deletedField)
                .notDeletedValue(notDeletedValue)
                .fillDeleted(fillDeleted);
    }

    public String getCreateTimeField() {
        return createTimeField;
    }

    public String getUpdateTimeField() {
        return updateTimeField;
    }

    public String getCreateByField() {
        return createByField;
    }

    public String getUpdateByField() {
        return updateByField;
    }

    public String getDeletedField() {
        return deletedField;
    }

    public Object getNotDeletedValue() {
        return notDeletedValue;
    }

    public boolean isFillDeleted() {
        return fillDeleted;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    /**
     * 审计字段填充配置构建器。
     */
    public static final class Builder {

        private String createTimeField = DEFAULT_CREATE_TIME_FIELD;
        private String updateTimeField = DEFAULT_UPDATE_TIME_FIELD;
        private String createByField = DEFAULT_CREATE_BY_FIELD;
        private String updateByField = DEFAULT_UPDATE_BY_FIELD;
        private String deletedField = DEFAULT_DELETED_FIELD;
        private Object notDeletedValue = DEFAULT_NOT_DELETED_VALUE;
        private boolean fillDeleted = true;

        private Builder() {
        }

        public Builder createTimeField(String createTimeField) {
            this.createTimeField = createTimeField;
            return this;
        }

        public Builder updateTimeField(String updateTimeField) {
            this.updateTimeField = updateTimeField;
            return this;
        }

        public Builder createByField(String createByField) {
            this.createByField = createByField;
            return this;
        }

        public Builder updateByField(String updateByField) {
            this.updateByField = updateByField;
            return this;
        }

        public Builder deletedField(String deletedField) {
            this.deletedField = deletedField;
            return this;
        }

        public Builder notDeletedValue(Object notDeletedValue) {
            this.notDeletedValue = notDeletedValue;
            return this;
        }

        public Builder fillDeleted(boolean fillDeleted) {
            this.fillDeleted = fillDeleted;
            return this;
        }

        public AuditFillOptions build() {
            return new AuditFillOptions(this);
        }
    }
}
