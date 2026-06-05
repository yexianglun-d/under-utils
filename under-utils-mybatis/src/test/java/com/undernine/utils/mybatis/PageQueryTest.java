package com.undernine.utils.mybatis;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.undernine.utils.mybatis.page.PageQuery;
import com.undernine.utils.mybatis.page.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页查询测试
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("分页查询测试")
@SuppressWarnings("deprecation")
class PageQueryTest {

    @Test
    @DisplayName("测试创建默认分页查询参数")
    void testDefaultPageQuery() {
        PageQuery pageQuery = PageQuery.of();

        assertThat(pageQuery.getCurrent()).isEqualTo(1L);
        assertThat(pageQuery.getSize()).isEqualTo(10L);
    }

    @Test
    @DisplayName("测试创建自定义分页查询参数")
    void testCustomPageQuery() {
        PageQuery pageQuery = PageQuery.of(2L, 20L);

        assertThat(pageQuery.getCurrent()).isEqualTo(2L);
        assertThat(pageQuery.getSize()).isEqualTo(20L);
    }

    @Test
    @DisplayName("测试添加排序字段")
    void testOrderBy() {
        PageQuery pageQuery = PageQuery.of()
                .orderByDesc("create_time")
                .orderByAsc("id");

        assertThat(pageQuery.getOrders()).hasSize(2);
        assertThat(pageQuery.getOrders().get(0).getColumn()).isEqualTo("create_time");
        assertThat(pageQuery.getOrders().get(0).isAsc()).isFalse();
        assertThat(pageQuery.getOrders().get(1).getColumn()).isEqualTo("id");
        assertThat(pageQuery.getOrders().get(1).isAsc()).isTrue();
    }

    @Test
    @DisplayName("测试构建 MyBatis-Plus Page 对象")
    void testBuildPage() {
        PageQuery pageQuery = PageQuery.of(3L, 15L)
                .orderByDesc("create_time");

        Page<Object> page = pageQuery.buildPage();

        assertThat(page.getCurrent()).isEqualTo(3L);
        assertThat(page.getSize()).isEqualTo(15L);
        assertThat(page.orders()).hasSize(1);
    }

    @Test
    @DisplayName("测试构建 Page 对象时使用默认排序")
    void testBuildPageWithDefaultOrders() {
        PageQuery pageQuery = PageQuery.of(1L, 10L);
        List<OrderItem> defaultOrders = List.of(OrderItem.desc("id"));

        Page<Object> page = pageQuery.buildPage(defaultOrders);

        assertThat(page.orders()).hasSize(1);
        assertThat(page.orders().get(0).getColumn()).isEqualTo("id");
    }

    @Test
    @DisplayName("测试分页大小超过最大值时自动限制")
    void testMaxSize() {
        PageQuery pageQuery = PageQuery.of(1L, 2000L);

        Page<Object> page = pageQuery.buildPage();

        assertThat(page.getSize()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("测试创建空分页结果")
    void testEmptyPageResult() {
        PageResult<String> emptyResult = PageResult.empty();

        assertThat(emptyResult.getRecords()).isEmpty();
        assertThat(emptyResult.getTotal()).isZero();
        assertThat(emptyResult.getCurrent()).isEqualTo(1L);
        assertThat(emptyResult.getSize()).isEqualTo(10L);
        assertThat(emptyResult.getPages()).isZero();
    }

    @Test
    @DisplayName("测试创建空分页结果（指定参数）")
    void testEmptyPageResultWithParams() {
        PageResult<String> emptyResult = PageResult.empty(2L, 20L);

        assertThat(emptyResult.getRecords()).isEmpty();
        assertThat(emptyResult.getTotal()).isZero();
        assertThat(emptyResult.getCurrent()).isEqualTo(2L);
        assertThat(emptyResult.getSize()).isEqualTo(20L);
    }

    @Test
    @DisplayName("测试 PageResult 转换")
    void testPageResultOf() {
        Page<String> page = new Page<>(1L, 10L);
        page.setRecords(List.of("item1", "item2", "item3"));
        page.setTotal(3L);

        PageResult<String> result = PageResult.of(page);

        assertThat(result.getRecords()).hasSize(3);
        assertThat(result.getTotal()).isEqualTo(3L);
        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(10L);
    }
}
