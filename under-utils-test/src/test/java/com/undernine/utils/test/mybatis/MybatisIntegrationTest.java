package com.undernine.utils.test.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.undernine.utils.mybatis.page.PageResult;
import com.undernine.utils.mybatis.page.SafePageQuery;
import com.undernine.utils.mybatis.page.SortFieldMapping;
import com.undernine.utils.test.mybatis.entity.User;
import com.undernine.utils.test.mybatis.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MyBatis 集成测试
 * 测试 BaseEntity 自动填充、分页、逻辑删除等功能
 *
 * @author undernine
 * @since 2024-12-03
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MybatisIntegrationTest {

    private static final Duration MYSQL_DATETIME_PRECISION = Duration.ofMillis(1);

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("under_utils_test")
            .withUsername("under_utils")
            .withPassword("under_utils");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE t_user");
    }

    @Test
    @DisplayName("测试基础实体类自动填充")
    void testAutoFill() {
        // 1. 创建用户
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setAge(25);
        user.setStatus(1);

        // 2. 插入数据
        int result = userMapper.insert(user);
        assertThat(result).isEqualTo(1);

        // 3. 验证自动填充字段
        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreateTime()).isNotNull();
        assertThat(user.getUpdateTime()).isNotNull();
        assertThat(user.getCreateBy()).isEqualTo(1001L); // 测试环境固定用户ID
        assertThat(user.getUpdateBy()).isEqualTo(1001L);
        assertThat(user.getDeleted()).isEqualTo(0);

        System.out.println("✅ 自动填充测试通过");
        System.out.println("   用户ID: " + user.getId());
        System.out.println("   创建时间: " + user.getCreateTime());
        System.out.println("   创建人: " + user.getCreateBy());
    }

    @Test
    @DisplayName("测试分页查询")
    void testPagination() {
        // 1. 插入测试数据
        for (int i = 1; i <= 25; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setAge(20 + i);
            user.setStatus(1);
            userMapper.insert(user);
        }

        // 2. 构建分页查询
        SortFieldMapping sortFieldMapping = SortFieldMapping.builder()
                .add("createdAt", "create_time")
                .build();
        SafePageQuery pageQuery = SafePageQuery.of(1L, 10L)
                .orderByDesc("createdAt");
        Page<User> page = pageQuery.buildPage(sortFieldMapping);

        // 3. 执行查询
        IPage<User> result = userMapper.selectPage(page, null);

        // 4. 转换为统一分页结果
        PageResult<User> pageResult = PageResult.of(result);

        // 5. 验证结果
        assertThat(pageResult.getRecords()).hasSize(10);
        assertThat(pageResult.getTotal()).isGreaterThanOrEqualTo(25L);
        assertThat(pageResult.getCurrent()).isEqualTo(1L);
        assertThat(pageResult.getSize()).isEqualTo(10L);
        assertThat(pageResult.getPages()).isGreaterThanOrEqualTo(3L);

        System.out.println("✅ 分页查询测试通过");
        System.out.println("   总记录数: " + pageResult.getTotal());
        System.out.println("   当前页: " + pageResult.getCurrent());
        System.out.println("   每页大小: " + pageResult.getSize());
        System.out.println("   总页数: " + pageResult.getPages());
    }

    @Test
    @DisplayName("测试逻辑删除")
    void testLogicDelete() {
        // 1. 创建用户
        User user = new User();
        user.setUsername("deletetest");
        user.setEmail("delete@example.com");
        user.setAge(30);
        user.setStatus(1);
        userMapper.insert(user);

        Long userId = user.getId();

        // 2. 逻辑删除
        int deleteResult = userMapper.deleteById(userId);
        assertThat(deleteResult).isEqualTo(1);

        // 3. 验证已被逻辑删除（正常查询查不到）
        User deletedUser = userMapper.selectById(userId);
        assertThat(deletedUser).isNull();

        // 4. 验证逻辑删除确实生效（其他未删除的记录仍能查到）
        List<User> activeUsers = userMapper.selectList(new LambdaQueryWrapper<User>());
        // 确保返回的列表中不包含已删除的记录
        assertThat(activeUsers.stream().noneMatch(u -> u.getId().equals(userId))).isTrue();

        System.out.println("✅ 逻辑删除测试通过");
        System.out.println("   删除的用户ID: " + userId);
        System.out.println("   正常查询结果: null（已被逻辑删除过滤）");
        System.out.println("   当前活跃用户数: " + activeUsers.size());
    }

    @Test
    @DisplayName("测试更新时自动填充")
    void testUpdateAutoFill() throws InterruptedException {
        // 1. 创建用户
        User user = new User();
        user.setUsername("updatetest");
        user.setEmail("update@example.com");
        user.setAge(28);
        user.setStatus(1);
        userMapper.insert(user);

        LocalDateTime createTime = user.getCreateTime();
        LocalDateTime updateTime = user.getUpdateTime();
        Long createBy = user.getCreateBy();

        // 等待 1 秒确保时间有差异
        Thread.sleep(1000);

        // 2. 更新用户
        user.setAge(30);
        user.setEmail("updated@example.com");
        int updateResult = userMapper.updateById(user);
        assertThat(updateResult).isEqualTo(1);

        // 3. 查询验证
        User updatedUser = userMapper.selectById(user.getId());

        // 4. 验证更新时间已变化，创建时间和创建人未变
        assertThat(Duration.between(createTime, updatedUser.getCreateTime()).abs())
                .isLessThan(MYSQL_DATETIME_PRECISION);
        assertThat(updatedUser.getCreateBy()).isEqualTo(createBy);
        assertThat(updatedUser.getUpdateTime()).isAfter(updateTime);
        assertThat(updatedUser.getUpdateBy()).isEqualTo(1001L);
        assertThat(updatedUser.getAge()).isEqualTo(30);
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");

        System.out.println("✅ 更新自动填充测试通过");
        System.out.println("   创建时间: " + updatedUser.getCreateTime() + " (未变化)");
        System.out.println("   更新时间: " + updatedUser.getUpdateTime() + " (已更新)");
        System.out.println("   创建人: " + updatedUser.getCreateBy() + " (未变化)");
        System.out.println("   更新人: " + updatedUser.getUpdateBy());
    }

    @Test
    @DisplayName("测试条件查询")
    void testConditionalQuery() {
        // 1. 插入测试数据
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.setUsername("condtest" + i);
            user.setEmail("cond" + i + "@example.com");
            user.setAge(20 + i);
            user.setStatus(i % 2); // 奇数启用，偶数禁用
            userMapper.insert(user);
        }

        // 2. 条件查询：状态为启用且年龄大于25
        List<User> activeUsers = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .gt(User::getAge, 25)
                .orderByDesc(User::getAge)
        );

        // 3. 验证结果
        assertThat(activeUsers).isNotEmpty();
        activeUsers.forEach(u -> {
            assertThat(u.getStatus()).isEqualTo(1);
            assertThat(u.getAge()).isGreaterThan(25);
        });

        System.out.println("✅ 条件查询测试通过");
        System.out.println("   符合条件的用户数: " + activeUsers.size());
        activeUsers.forEach(u -> 
            System.out.println("   - " + u.getUsername() + ", 年龄: " + u.getAge() + ", 状态: " + u.getStatus())
        );
    }
}
