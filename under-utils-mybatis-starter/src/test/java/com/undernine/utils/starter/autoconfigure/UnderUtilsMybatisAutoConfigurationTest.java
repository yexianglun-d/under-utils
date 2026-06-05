package com.undernine.utils.starter.autoconfigure;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.undernine.utils.mybatis.handler.AuditorProvider;
import com.undernine.utils.mybatis.handler.DefaultMetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UnderUtilsMybatisAutoConfiguration 测试。
 *
 * @author Under-Utils Team
 */
class UnderUtilsMybatisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UnderUtilsMybatisAutoConfiguration.class));

    @Test
    void shouldAutoConfigureDefaultMybatisComponents() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
            assertThat(context).hasSingleBean(MetaObjectHandler.class);
            assertThat(context.getBean(MetaObjectHandler.class)).isInstanceOf(DefaultMetaObjectHandler.class);
        });
    }

    @Test
    void shouldBindAuditPropertiesAndAuditorProvider() {
        contextRunner
                .withBean(AuditorProvider.class, () -> () -> 1001L)
                .withPropertyValues(
                        "under.utils.mybatis.audit.create-time-field=createdAt",
                        "under.utils.mybatis.audit.update-time-field=updatedAt",
                        "under.utils.mybatis.audit.create-by-field=creatorId",
                        "under.utils.mybatis.audit.update-by-field=updaterId",
                        "under.utils.mybatis.audit.deleted-field=isDeleted",
                        "under.utils.mybatis.audit.not-deleted-value=false"
                )
                .run(context -> {
                    Map<String, Object> entity = new HashMap<>();
                    MetaObject metaObject = SystemMetaObject.forObject(entity);

                    context.getBean(MetaObjectHandler.class).insertFill(metaObject);

                    assertThat(entity.get("createdAt")).isNotNull();
                    assertThat(entity.get("updatedAt")).isNotNull();
                    assertThat(entity.get("creatorId")).isEqualTo(1001L);
                    assertThat(entity.get("updaterId")).isEqualTo(1001L);
                    assertThat(String.valueOf(entity.get("isDeleted"))).isEqualTo("false");
                    assertThat(entity).doesNotContainKey("createTime");
                });
    }

    @Test
    void shouldDisableAuditFillWhenConfigured() {
        contextRunner
                .withPropertyValues("under.utils.mybatis.audit.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context).doesNotHaveBean(MetaObjectHandler.class);
                });
    }

    @Test
    void shouldDisableInterceptorWhenConfigured() {
        contextRunner
                .withPropertyValues("under.utils.mybatis.interceptor.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MybatisPlusInterceptor.class);
                    assertThat(context).hasSingleBean(MetaObjectHandler.class);
                });
    }

    @Test
    void shouldDisableAllWhenConfigured() {
        contextRunner
                .withPropertyValues("under.utils.mybatis.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MybatisPlusInterceptor.class);
                    assertThat(context).doesNotHaveBean(MetaObjectHandler.class);
                });
    }

    @Test
    void shouldBackOffWhenUserBeansExist() {
        MybatisPlusInterceptor customInterceptor = new MybatisPlusInterceptor();
        MetaObjectHandler customHandler = new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
            }

            @Override
            public void updateFill(MetaObject metaObject) {
            }
        };

        contextRunner
                .withBean(MybatisPlusInterceptor.class, () -> customInterceptor)
                .withBean(MetaObjectHandler.class, () -> customHandler)
                .run(context -> {
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context).hasSingleBean(MetaObjectHandler.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class)).isSameAs(customInterceptor);
                    assertThat(context.getBean(MetaObjectHandler.class)).isSameAs(customHandler);
                });
    }
}
