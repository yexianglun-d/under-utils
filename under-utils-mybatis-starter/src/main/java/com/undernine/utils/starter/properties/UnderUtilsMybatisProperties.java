package com.undernine.utils.starter.properties;

import com.baomidou.mybatisplus.annotation.DbType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Under-Utils MyBatis 自动配置属性。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
@ConfigurationProperties(prefix = "under.utils.mybatis")
public class UnderUtilsMybatisProperties {

    private boolean enabled = true;
    private Interceptor interceptor = new Interceptor();
    private Audit audit = new Audit();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Interceptor getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    /**
     * MyBatis-Plus interceptor 自动配置。
     */
    public static class Interceptor {
        private boolean enabled = true;
        private DbType dbType = DbType.MYSQL;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public DbType getDbType() {
            return dbType;
        }

        public void setDbType(DbType dbType) {
            this.dbType = dbType;
        }
    }

    /**
     * 审计字段自动填充配置。
     */
    public static class Audit {
        private boolean enabled = true;
        private String createTimeField = "createTime";
        private String updateTimeField = "updateTime";
        private String createByField = "createBy";
        private String updateByField = "updateBy";
        private String deletedField = "deleted";
        private Object notDeletedValue = 0;
        private boolean fillDeleted = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCreateTimeField() {
            return createTimeField;
        }

        public void setCreateTimeField(String createTimeField) {
            this.createTimeField = createTimeField;
        }

        public String getUpdateTimeField() {
            return updateTimeField;
        }

        public void setUpdateTimeField(String updateTimeField) {
            this.updateTimeField = updateTimeField;
        }

        public String getCreateByField() {
            return createByField;
        }

        public void setCreateByField(String createByField) {
            this.createByField = createByField;
        }

        public String getUpdateByField() {
            return updateByField;
        }

        public void setUpdateByField(String updateByField) {
            this.updateByField = updateByField;
        }

        public String getDeletedField() {
            return deletedField;
        }

        public void setDeletedField(String deletedField) {
            this.deletedField = deletedField;
        }

        public Object getNotDeletedValue() {
            return notDeletedValue;
        }

        public void setNotDeletedValue(Object notDeletedValue) {
            this.notDeletedValue = notDeletedValue;
        }

        public boolean isFillDeleted() {
            return fillDeleted;
        }

        public void setFillDeleted(boolean fillDeleted) {
            this.fillDeleted = fillDeleted;
        }
    }
}
