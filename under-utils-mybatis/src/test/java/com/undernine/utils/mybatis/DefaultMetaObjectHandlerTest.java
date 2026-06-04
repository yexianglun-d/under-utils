package com.undernine.utils.mybatis;

import com.undernine.utils.mybatis.handler.DefaultMetaObjectHandler;
import com.undernine.utils.mybatis.handler.AuditFillOptions;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultMetaObjectHandler 测试
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("DefaultMetaObjectHandler 测试")
class DefaultMetaObjectHandlerTest {

    private DefaultMetaObjectHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DefaultMetaObjectHandler();
    }

    @Test
    @DisplayName("测试插入时自动填充创建时间和修改时间")
    void testInsertFillTime() {
        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        handler.insertFill(metaObject);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        LocalDateTime createTime = (LocalDateTime) entity.get("createTime");
        LocalDateTime updateTime = (LocalDateTime) entity.get("updateTime");

        assertThat(createTime).isNotNull();
        assertThat(updateTime).isNotNull();
        assertThat(createTime).isBetween(before, after);
        assertThat(updateTime).isBetween(before, after);
    }

    @Test
    @DisplayName("测试插入时填充逻辑删除标记")
    void testInsertFillDeleted() {
        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        handler.insertFill(metaObject);

        Integer deleted = (Integer) entity.get("deleted");
        assertThat(deleted).isEqualTo(0);
    }

    @Test
    @DisplayName("测试更新时自动填充修改时间")
    void testUpdateFillTime() {
        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        handler.updateFill(metaObject);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        LocalDateTime updateTime = (LocalDateTime) entity.get("updateTime");

        assertThat(updateTime).isNotNull();
        assertThat(updateTime).isBetween(before, after);
    }

    @Test
    @DisplayName("测试审计 Provider 注入")
    void testAuditorProviderInjection() {
        DefaultMetaObjectHandler providerHandler = new DefaultMetaObjectHandler(() -> 777L);

        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        providerHandler.insertFill(metaObject);

        assertThat(entity.get("createBy")).isEqualTo(777L);
        assertThat(entity.get("updateBy")).isEqualTo(777L);
    }

    @Test
    @DisplayName("测试自定义审计字段名和未删除值")
    void testCustomAuditFillOptions() {
        AuditFillOptions options = AuditFillOptions.builder()
                .createTimeField("createdAt")
                .updateTimeField("updatedAt")
                .createByField("creatorId")
                .updateByField("updaterId")
                .deletedField("isDeleted")
                .notDeletedValue(false)
                .build();
        DefaultMetaObjectHandler customHandler = new DefaultMetaObjectHandler(() -> 1001L, options);

        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        customHandler.insertFill(metaObject);

        assertThat(entity.get("createdAt")).isInstanceOf(LocalDateTime.class);
        assertThat(entity.get("updatedAt")).isInstanceOf(LocalDateTime.class);
        assertThat(entity.get("creatorId")).isEqualTo(1001L);
        assertThat(entity.get("updaterId")).isEqualTo(1001L);
        assertThat(entity.get("isDeleted")).isEqualTo(false);
        assertThat(entity.get("createTime")).isNull();
        assertThat(entity.get("deleted")).isNull();
    }

    @Test
    @DisplayName("测试可以关闭逻辑删除默认填充")
    void testDisableDeletedFill() {
        DefaultMetaObjectHandler customHandler = new DefaultMetaObjectHandler(
                null, AuditFillOptions.builder().fillDeleted(false).build());

        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        customHandler.insertFill(metaObject);

        assertThat(entity.get("createTime")).isNotNull();
        assertThat(entity.get("deleted")).isNull();
    }

    @Test
    @DisplayName("测试审计字段配置拒绝空字段名")
    void testRejectBlankAuditFieldName() {
        assertThatThrownBy(() -> AuditFillOptions.builder()
                .createTimeField(" ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createTimeField must not be blank");
    }

    @Test
    @DisplayName("测试审计 Provider 更新填充")
    void testAuditorProviderUpdateFill() {
        DefaultMetaObjectHandler providerHandler = new DefaultMetaObjectHandler(() -> 778L);

        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        providerHandler.updateFill(metaObject);

        assertThat(entity.get("updateBy")).isEqualTo(778L);
    }

    @Test
    @DisplayName("测试自定义用户 ID 获取逻辑")
    void testCustomUserIdHandler() {
        // 创建自定义处理器，返回固定用户 ID
        DefaultMetaObjectHandler customHandler = new DefaultMetaObjectHandler() {
            @Override
            protected Long getUserId() {
                return 999L;
            }
        };

        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        customHandler.insertFill(metaObject);

        Long createBy = (Long) entity.get("createBy");
        Long updateBy = (Long) entity.get("updateBy");

        assertThat(createBy).isEqualTo(999L);
        assertThat(updateBy).isEqualTo(999L);
    }

    @Test
    @DisplayName("测试默认用户 ID 为 null 时不填充")
    void testDefaultUserIdIsNull() {
        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        handler.insertFill(metaObject);

        // 默认实现返回 null，不应该填充用户 ID
        assertThat(entity.get("createBy")).isNull();
        assertThat(entity.get("updateBy")).isNull();
    }

    @Test
    @DisplayName("测试更新时填充修改人")
    void testUpdateFillUpdateBy() {
        // 创建自定义处理器，返回固定用户 ID
        DefaultMetaObjectHandler customHandler = new DefaultMetaObjectHandler() {
            @Override
            protected Long getUserId() {
                return 888L;
            }
        };

        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        customHandler.updateFill(metaObject);

        Long updateBy = (Long) entity.get("updateBy");
        assertThat(updateBy).isEqualTo(888L);
    }

    @Test
    @DisplayName("测试插入和更新填充的完整流程")
    void testFullFillProcess() {
        // 创建自定义处理器
        DefaultMetaObjectHandler customHandler = new DefaultMetaObjectHandler() {
            @Override
            protected Long getUserId() {
                return 100L;
            }
        };

        // 模拟插入
        Map<String, Object> entity = new HashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);
        customHandler.insertFill(metaObject);

        assertThat(entity.get("createTime")).isNotNull();
        assertThat(entity.get("updateTime")).isNotNull();
        assertThat(entity.get("createBy")).isEqualTo(100L);
        assertThat(entity.get("updateBy")).isEqualTo(100L);
        assertThat(entity.get("deleted")).isEqualTo(0);

        // 模拟更新（修改用户 ID）
        DefaultMetaObjectHandler updateHandler = new DefaultMetaObjectHandler() {
            @Override
            protected Long getUserId() {
                return 200L;
            }
        };

        updateHandler.updateFill(metaObject);

        // 验证更新后的字段
        assertThat(entity.get("updateTime")).isNotNull();
        assertThat(entity.get("updateBy")).isEqualTo(200L);
        // 创建时间和创建人不应该被修改
        assertThat(entity.get("createTime")).isNotNull();
        assertThat(entity.get("createBy")).isEqualTo(100L);
    }
}
