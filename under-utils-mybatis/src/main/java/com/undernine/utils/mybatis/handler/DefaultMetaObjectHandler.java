package com.undernine.utils.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 元数据自动填充处理器
 * <p>
 * 自动填充创建时间、修改时间、创建人、修改人等字段
 * </p>
 * <p>
 * 使用说明：
 * 1. 在 Spring Boot 项目中，将此类注册为 Bean 即可生效
 * 2. 如需自定义用户 ID 获取逻辑，可注入 {@link AuditorProvider}，或继承此类并重写 getUserId() 方法
 * 3. 如需自定义字段名或逻辑未删除值，可注入 {@link AuditFillOptions}
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DefaultMetaObjectHandler implements MetaObjectHandler {

    private final AuditorProvider auditorProvider;
    private final AuditFillOptions fillOptions;

    public DefaultMetaObjectHandler() {
        this(null, AuditFillOptions.defaults());
    }

    public DefaultMetaObjectHandler(AuditorProvider auditorProvider) {
        this(auditorProvider, AuditFillOptions.defaults());
    }

    public DefaultMetaObjectHandler(AuditorProvider auditorProvider, AuditFillOptions fillOptions) {
        this.auditorProvider = auditorProvider;
        this.fillOptions = fillOptions == null ? AuditFillOptions.defaults() : fillOptions;
    }

    /**
     * 插入时自动填充
     *
     * @param metaObject 元数据对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");

        LocalDateTime now = LocalDateTime.now();
        this.fillStrategy(metaObject, fillOptions.getCreateTimeField(), now);
        this.fillStrategy(metaObject, fillOptions.getUpdateTimeField(), now);

        Long userId = getUserId();
        if (userId != null) {
            this.fillStrategy(metaObject, fillOptions.getCreateByField(), userId);
            this.fillStrategy(metaObject, fillOptions.getUpdateByField(), userId);
        }

        if (fillOptions.isFillDeleted()) {
            this.fillStrategy(metaObject, fillOptions.getDeletedField(), fillOptions.getNotDeletedValue());
        }
    }

    /**
     * 更新时自动填充
     *
     * @param metaObject 元数据对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");

        this.setFieldValByName(fillOptions.getUpdateTimeField(), LocalDateTime.now(), metaObject);

        Long userId = getUserId();
        if (userId != null) {
            this.setFieldValByName(fillOptions.getUpdateByField(), userId, metaObject);
        }
    }

    /**
     * 获取当前用户 ID
     * <p>
     * 默认实现返回 null，子类可以重写此方法，从 ThreadLocal、Spring Security 等获取当前用户 ID
     * </p>
     *
     * @return 当前用户 ID，如果无法获取则返回 null
     */
    protected Long getUserId() {
        return auditorProvider == null ? null : auditorProvider.getCurrentAuditor();
    }
}
