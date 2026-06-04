package com.undernine.utils.test.examples;

import com.undernine.utils.core.collection.CollectionUtils;

import java.util.*;

/**
 * CollectionUtils 使用示例
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class CollectionUtilsExample {

    public static void main(String[] args) {
        System.out.println("========== CollectionUtils 使用示例 ==========\n");

        // 1. 集合判空
        collectionEmptyCheck();

        // 2. Map 判空
        mapEmptyCheck();

        // 3. 安全获取元素
        safeElementAccess();

        // 4. 列表分批处理
        listPartition();

        // 5. 集合去重
        collectionDistinct();

        // 6. 集合转换
        collectionMapping();

        // 7. 集合运算
        collectionOperations();

        // 8. 实际应用场景
        practicalUseCases();
    }

    /**
     * 1. 集合判空
     */
    private static void collectionEmptyCheck() {
        System.out.println("1. 集合判空");

        List<String> list1 = null;
        List<String> list2 = new ArrayList<>();
        List<String> list3 = Arrays.asList("a", "b", "c");

        System.out.println("null 集合为空: " + CollectionUtils.isEmpty(list1));
        System.out.println("空集合为空: " + CollectionUtils.isEmpty(list2));
        System.out.println("非空集合为空: " + CollectionUtils.isEmpty(list3));
        System.out.println("非空集合不为空: " + CollectionUtils.isNotEmpty(list3));
        System.out.println("集合大小: " + CollectionUtils.size(list3));
        System.out.println();
    }

    /**
     * 2. Map 判空
     */
    private static void mapEmptyCheck() {
        System.out.println("2. Map 判空");

        Map<String, Object> map1 = null;
        Map<String, Object> map2 = new HashMap<>();
        Map<String, Object> map3 = new HashMap<>();
        map3.put("key", "value");

        System.out.println("null Map 为空: " + CollectionUtils.isEmpty(map1));
        System.out.println("空 Map 为空: " + CollectionUtils.isEmpty(map2));
        System.out.println("非空 Map 为空: " + CollectionUtils.isEmpty(map3));
        System.out.println("Map 大小: " + CollectionUtils.size(map3));
        System.out.println();
    }

    /**
     * 3. 安全获取元素
     */
    private static void safeElementAccess() {
        System.out.println("3. 安全获取元素");

        List<String> list = Arrays.asList("Apple", "Banana", "Cherry", "Date");

        System.out.println("第一个元素: " + CollectionUtils.getFirst(list));
        System.out.println("最后一个元素: " + CollectionUtils.getLast(list));
        System.out.println("索引 2 的元素: " + CollectionUtils.get(list, 2));
        System.out.println("索引 10 的元素（越界）: " + CollectionUtils.get(list, 10));
        System.out.println("索引 10 的元素（带默认值）: " + CollectionUtils.get(list, 10, "默认值"));

        // 空列表或 null 列表
        List<String> emptyList = new ArrayList<>();
        System.out.println("空列表第一个元素: " + CollectionUtils.getFirst(emptyList));
        System.out.println("null 列表第一个元素: " + CollectionUtils.getFirst(null));
        System.out.println();
    }

    /**
     * 4. 列表分批处理（分页）
     */
    private static void listPartition() {
        System.out.println("4. 列表分批处理");

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<List<Integer>> batches = CollectionUtils.partition(numbers, 3);

        System.out.println("原始列表: " + numbers);
        System.out.println("分批大小: 3");
        System.out.println("分批结果:");
        for (int i = 0; i < batches.size(); i++) {
            System.out.println("  批次 " + (i + 1) + ": " + batches.get(i));
        }
        System.out.println();
    }

    /**
     * 5. 集合去重
     */
    private static void collectionDistinct() {
        System.out.println("5. 集合去重");

        List<String> withDuplicates = Arrays.asList("apple", "banana", "apple", "cherry", "banana", "date");
        List<String> distinct = CollectionUtils.distinct(withDuplicates);

        System.out.println("原始列表: " + withDuplicates);
        System.out.println("去重后: " + distinct);
        System.out.println("原始大小: " + withDuplicates.size() + ", 去重后大小: " + distinct.size());
        System.out.println();
    }

    /**
     * 6. 集合转换（映射）
     */
    private static void collectionMapping() {
        System.out.println("6. 集合转换");

        // 字符串长度转换
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David");
        List<Integer> lengths = CollectionUtils.map(names, String::length);
        System.out.println("名字列表: " + names);
        System.out.println("长度列表: " + lengths);

        // 转大写
        List<String> upperCase = CollectionUtils.map(names, String::toUpperCase);
        System.out.println("大写列表: " + upperCase);

        // 数组转 List
        String[] array = {"a", "b", "c"};
        List<String> list = CollectionUtils.toList(array);
        System.out.println("数组转 List: " + list);
        System.out.println();
    }

    /**
     * 7. 集合运算
     */
    private static void collectionOperations() {
        System.out.println("7. 集合运算");

        List<String> team1 = Arrays.asList("Alice", "Bob", "Charlie");
        List<String> team2 = Arrays.asList("Bob", "Charlie", "David", "Eve");

        System.out.println("团队 1: " + team1);
        System.out.println("团队 2: " + team2);

        // 并集
        List<String> union = CollectionUtils.union(team1, team2);
        System.out.println("并集（所有成员）: " + union);

        // 交集
        List<String> intersection = CollectionUtils.intersection(team1, team2);
        System.out.println("交集（共同成员）: " + intersection);

        // 差集
        List<String> subtract = CollectionUtils.subtract(team1, team2);
        System.out.println("差集（团队1独有）: " + subtract);

        // 包含检查
        boolean containsBob = CollectionUtils.contains(team1, "Bob");
        System.out.println("团队1 包含 Bob: " + containsBob);
        System.out.println();
    }

    /**
     * 8. 实际应用场景
     */
    private static void practicalUseCases() {
        System.out.println("8. 实际应用场景");

        // 场景 1: 批量处理数据（如批量插入数据库）
        System.out.println("场景 1: 批量插入数据库");
        List<String> allData = Arrays.asList("data1", "data2", "data3", "data4", "data5",
                "data6", "data7", "data8", "data9", "data10");
        List<List<String>> batches = CollectionUtils.partition(allData, 3);
        System.out.println("总数据量: " + allData.size());
        System.out.println("分批数量: " + batches.size());
        for (int i = 0; i < batches.size(); i++) {
            System.out.println("  批次 " + (i + 1) + " 插入 " + batches.get(i).size() + " 条数据");
        }

        System.out.println();

        // 场景 2: 查询结果去重
        System.out.println("场景 2: 查询结果去重");
        List<Long> userIds = Arrays.asList(1001L, 1002L, 1001L, 1003L, 1002L);
        List<Long> distinctUserIds = CollectionUtils.distinct(userIds);
        System.out.println("原始用户ID: " + userIds);
        System.out.println("去重后: " + distinctUserIds);

        System.out.println();

        // 场景 3: 提取对象属性
        System.out.println("场景 3: 提取对象属性");
        List<User> users = Arrays.asList(
                new User(1L, "Alice", 25),
                new User(2L, "Bob", 30),
                new User(3L, "Charlie", 28)
        );
        List<String> userNames = CollectionUtils.map(users, User::getName);
        List<Integer> userAges = CollectionUtils.map(users, User::getAge);
        System.out.println("用户名列表: " + userNames);
        System.out.println("年龄列表: " + userAges);

        System.out.println();

        // 场景 4: 权限合并
        System.out.println("场景 4: 权限合并");
        List<String> rolePermissions = Arrays.asList("READ", "WRITE");
        List<String> userPermissions = Arrays.asList("WRITE", "DELETE", "ADMIN");
        List<String> allPermissions = CollectionUtils.union(rolePermissions, userPermissions);
        System.out.println("角色权限: " + rolePermissions);
        System.out.println("用户权限: " + userPermissions);
        System.out.println("合并后权限: " + allPermissions);
    }

    // ==================== 测试实体类 ====================

    static class User {
        private Long id;
        private String name;
        private Integer age;

        public User(Long id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "', age=" + age + "}";
        }
    }
}
