package com.undernine.utils.starter.autoconfigure;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.undernine.utils.mybatis.config.MybatisPlusConfig;
import com.undernine.utils.mybatis.handler.AuditFillOptions;
import com.undernine.utils.mybatis.handler.AuditorProvider;
import com.undernine.utils.mybatis.handler.DefaultMetaObjectHandler;
import com.undernine.utils.starter.properties.UnderUtilsMybatisProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Under-Utils MyBatis 自动配置入口。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
@AutoConfiguration
@EnableConfigurationProperties(UnderUtilsMybatisProperties.class)
@ConditionalOnClass({MybatisPlusInterceptor.class, MetaObjectHandler.class})
@ConditionalOnProperty(prefix = "under.utils.mybatis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UnderUtilsMybatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "under.utils.mybatis.interceptor",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public MybatisPlusInterceptor mybatisPlusInterceptor(UnderUtilsMybatisProperties properties) {
        return MybatisPlusConfig.mybatisPlusInterceptor(properties.getInterceptor().getDbType());
    }

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    @ConditionalOnProperty(prefix = "under.utils.mybatis.audit",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public DefaultMetaObjectHandler defaultMetaObjectHandler(
            UnderUtilsMybatisProperties properties,
            ObjectProvider<AuditorProvider> auditorProvider) {
        return new DefaultMetaObjectHandler(auditorProvider.getIfAvailable(), auditFillOptions(properties));
    }

    private AuditFillOptions auditFillOptions(UnderUtilsMybatisProperties properties) {
        UnderUtilsMybatisProperties.Audit audit = properties.getAudit();
        Object notDeletedValue = audit.getNotDeletedValue() == null ? 0 : audit.getNotDeletedValue();
        return AuditFillOptions.builder()
                .createTimeField(audit.getCreateTimeField())
                .updateTimeField(audit.getUpdateTimeField())
                .createByField(audit.getCreateByField())
                .updateByField(audit.getUpdateByField())
                .deletedField(audit.getDeletedField())
                .notDeletedValue(notDeletedValue)
                .fillDeleted(audit.isFillDeleted())
                .build();
    }
}
