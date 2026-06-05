package com.undernine.utils.core.collection;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * CollectionUtils 测试类
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
class CollectionUtilsTest {

    // ==================== isEmpty(Collection) 测试 ====================

    @Test
    void testIsEmpty_collection_null() {
        assertThat(CollectionUtils.isEmpty((Collection<?>) null)).isTrue();
    }

    @Test
    void testIsEmpty_collection_empty() {
        assertThat(CollectionUtils.isEmpty(new ArrayList<>())).isTrue();
    }

    @Test
    void testIsEmpty_collection_notEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertThat(CollectionUtils.isEmpty(list)).isFalse();
    }

    // ==================== isNotEmpty(Collection) 测试 ====================

    @Test
    void testIsNotEmpty_collection_null() {
        assertThat(CollectionUtils.isNotEmpty((Collection<?>) null)).isFalse();
    }

    @Test
    void testIsNotEmpty_collection_empty() {
        assertThat(CollectionUtils.isNotEmpty(new ArrayList<>())).isFalse();
    }

    @Test
    void testIsNotEmpty_collection_notEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertThat(CollectionUtils.isNotEmpty(list)).isTrue();
    }

    // ==================== isEmpty(Map) 测试 ====================

    @Test
    void testIsEmpty_map_null() {
        assertThat(CollectionUtils.isEmpty((Map<?, ?>) null)).isTrue();
    }

    @Test
    void testIsEmpty_map_empty() {
        assertThat(CollectionUtils.isEmpty(new HashMap<>())).isTrue();
    }

    @Test
    void testIsEmpty_map_notEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        assertThat(CollectionUtils.isEmpty(map)).isFalse();
    }

    // ==================== isNotEmpty(Map) 测试 ====================

    @Test
    void testIsNotEmpty_map_null() {
        assertThat(CollectionUtils.isNotEmpty((Map<?, ?>) null)).isFalse();
    }

    @Test
    void testIsNotEmpty_map_empty() {
        assertThat(CollectionUtils.isNotEmpty(new HashMap<>())).isFalse();
    }

    @Test
    void testIsNotEmpty_map_notEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        assertThat(CollectionUtils.isNotEmpty(map)).isTrue();
    }

    // ==================== size() 测试 ====================

    @Test
    void testSize_collection_null() {
        assertThat(CollectionUtils.size((Collection<?>) null)).isEqualTo(0);
    }

    @Test
    void testSize_collection_empty() {
        assertThat(CollectionUtils.size(new ArrayList<>())).isEqualTo(0);
    }

    @Test
    void testSize_collection() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.size(list)).isEqualTo(3);
    }

    @Test
    void testSize_map_null() {
        assertThat(CollectionUtils.size((Map<?, ?>) null)).isEqualTo(0);
    }

    @Test
    void testSize_map_empty() {
        assertThat(CollectionUtils.size(new HashMap<>())).isEqualTo(0);
    }

    @Test
    void testSize_map() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        assertThat(CollectionUtils.size(map)).isEqualTo(2);
    }

    // ==================== getFirst() 测试 ====================

    @Test
    void testGetFirst_null() {
        assertThat(CollectionUtils.<String>getFirst(null)).isNull();
    }

    @Test
    void testGetFirst_empty() {
        assertThat(CollectionUtils.getFirst(new ArrayList<String>())).isNull();
    }

    @Test
    void testGetFirst() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.getFirst(list)).isEqualTo("a");
    }

    @Test
    void testGetFirst_singleElement() {
        List<String> list = Collections.singletonList("a");
        assertThat(CollectionUtils.getFirst(list)).isEqualTo("a");
    }

    // ==================== getLast() 测试 ====================

    @Test
    void testGetLast_null() {
        assertThat(CollectionUtils.<String>getLast(null)).isNull();
    }

    @Test
    void testGetLast_empty() {
        assertThat(CollectionUtils.getLast(new ArrayList<String>())).isNull();
    }

    @Test
    void testGetLast() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.getLast(list)).isEqualTo("c");
    }

    @Test
    void testGetLast_singleElement() {
        List<String> list = Collections.singletonList("a");
        assertThat(CollectionUtils.getLast(list)).isEqualTo("a");
    }

    // ==================== get(List, index) 测试 ====================

    @Test
    void testGet_null() {
        assertThat(CollectionUtils.<String>get(null, 0)).isNull();
    }

    @Test
    void testGet_empty() {
        assertThat(CollectionUtils.get(new ArrayList<String>(), 0)).isNull();
    }

    @Test
    void testGet_validIndex() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.get(list, 0)).isEqualTo("a");
        assertThat(CollectionUtils.get(list, 1)).isEqualTo("b");
        assertThat(CollectionUtils.get(list, 2)).isEqualTo("c");
    }

    @Test
    void testGet_negativeIndex() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.get(list, -1)).isNull();
    }

    @Test
    void testGet_outOfBounds() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.get(list, 10)).isNull();
    }

    // ==================== get(List, index, defaultValue) 测试 ====================

    @Test
    void testGetWithDefault_null() {
        assertThat(CollectionUtils.get(null, 0, "default")).isEqualTo("default");
    }

    @Test
    void testGetWithDefault_outOfBounds() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.get(list, 10, "default")).isEqualTo("default");
    }

    @Test
    void testGetWithDefault_validIndex() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.get(list, 1, "default")).isEqualTo("b");
    }

    // ==================== partition() 测试 ====================

    @Test
    void testPartition_null() {
        List<List<String>> result = CollectionUtils.partition(null, 3);
        assertThat(result).isEmpty();
    }

    @Test
    void testPartition_empty() {
        List<List<String>> result = CollectionUtils.partition(new ArrayList<>(), 3);
        assertThat(result).isEmpty();
    }

    @Test
    void testPartition_exactDivision() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<List<Integer>> result = CollectionUtils.partition(list, 2);
        
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).containsExactly(1, 2);
        assertThat(result.get(1)).containsExactly(3, 4);
        assertThat(result.get(2)).containsExactly(5, 6);
    }

    @Test
    void testPartition_notExactDivision() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> result = CollectionUtils.partition(list, 3);
        
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).containsExactly(1, 2, 3);
        assertThat(result.get(1)).containsExactly(4, 5, 6);
        assertThat(result.get(2)).containsExactly(7);
    }

    @Test
    void testPartition_invalidBatchSize() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        
        assertThatThrownBy(() -> CollectionUtils.partition(list, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Batch size must be greater than 0");
        
        assertThatThrownBy(() -> CollectionUtils.partition(list, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== distinct() 测试 ====================

    @Test
    void testDistinct_null() {
        List<String> result = CollectionUtils.distinct(null);
        assertThat(result).isEmpty();
    }

    @Test
    void testDistinct_empty() {
        List<String> result = CollectionUtils.distinct(new ArrayList<>());
        assertThat(result).isEmpty();
    }

    @Test
    void testDistinct_noDuplicates() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = CollectionUtils.distinct(list);
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void testDistinct_withDuplicates() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "d");
        List<String> result = CollectionUtils.distinct(list);
        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    @Test
    void testDistinct_preserveOrder() {
        List<String> list = Arrays.asList("c", "a", "b", "a", "c");
        List<String> result = CollectionUtils.distinct(list);
        assertThat(result).containsExactly("c", "a", "b");
    }

    // ==================== map() 测试 ====================

    @Test
    void testMap_null() {
        List<Integer> result = CollectionUtils.map(null, String::length);
        assertThat(result).isEmpty();
    }

    @Test
    void testMap_empty() {
        List<Integer> result = CollectionUtils.map(new ArrayList<>(), String::length);
        assertThat(result).isEmpty();
    }

    @Test
    void testMap() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        List<Integer> result = CollectionUtils.map(list, String::length);
        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    void testMap_toUpperCase() {
        List<String> list = Arrays.asList("hello", "world");
        List<String> result = CollectionUtils.map(list, String::toUpperCase);
        assertThat(result).containsExactly("HELLO", "WORLD");
    }

    // ==================== toList() 测试 ====================

    @Test
    void testToList_null() {
        List<String> result = CollectionUtils.toList((String[]) null);
        assertThat(result).isEmpty();
    }

    @Test
    void testToList_empty() {
        List<String> result = CollectionUtils.toList();
        assertThat(result).isEmpty();
    }

    @Test
    void testToList() {
        List<String> result = CollectionUtils.toList("a", "b", "c");
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void testToList_mutable() {
        List<String> result = CollectionUtils.toList("a", "b", "c");
        result.add("d"); // 应该可以修改
        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    // ==================== addAll() 测试 ====================

    @Test
    void testAddAll_nullCollection() {
        boolean result = CollectionUtils.addAll(null, "a", "b");
        assertThat(result).isFalse();
    }

    @Test
    void testAddAll_nullElements() {
        List<String> list = new ArrayList<>();
        boolean result = CollectionUtils.addAll(list, (String[]) null);
        assertThat(result).isFalse();
        assertThat(list).isEmpty();
    }

    @Test
    void testAddAll_emptyElements() {
        List<String> list = new ArrayList<>();
        boolean result = CollectionUtils.addAll(list);
        assertThat(result).isFalse();
        assertThat(list).isEmpty();
    }

    @Test
    void testAddAll() {
        List<String> list = new ArrayList<>();
        boolean result = CollectionUtils.addAll(list, "a", "b", "c");
        assertThat(result).isTrue();
        assertThat(list).containsExactly("a", "b", "c");
    }

    // ==================== union() 测试 ====================

    @Test
    void testUnion_bothNull() {
        List<String> result = CollectionUtils.union(null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void testUnion_firstNull() {
        List<String> list2 = Arrays.asList("a", "b");
        List<String> result = CollectionUtils.union(null, list2);
        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void testUnion_secondNull() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> result = CollectionUtils.union(list1, null);
        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void testUnion() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("b", "c", "d");
        List<String> result = CollectionUtils.union(list1, list2);
        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    @Test
    void testUnion_noDuplicates() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> result = CollectionUtils.union(list1, list2);
        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    // ==================== intersection() 测试 ====================

    @Test
    void testIntersection_bothNull() {
        List<String> result = CollectionUtils.intersection(null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void testIntersection_firstNull() {
        List<String> list2 = Arrays.asList("a", "b");
        List<String> result = CollectionUtils.intersection(null, list2);
        assertThat(result).isEmpty();
    }

    @Test
    void testIntersection_secondNull() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> result = CollectionUtils.intersection(list1, null);
        assertThat(result).isEmpty();
    }

    @Test
    void testIntersection() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("b", "c", "d");
        List<String> result = CollectionUtils.intersection(list1, list2);
        assertThat(result).containsExactly("b", "c");
    }

    @Test
    void testIntersection_noCommon() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> result = CollectionUtils.intersection(list1, list2);
        assertThat(result).isEmpty();
    }

    // ==================== subtract() 测试 ====================

    @Test
    void testSubtract_bothNull() {
        List<String> result = CollectionUtils.subtract(null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void testSubtract_firstNull() {
        List<String> list2 = Arrays.asList("a", "b");
        List<String> result = CollectionUtils.subtract(null, list2);
        assertThat(result).isEmpty();
    }

    @Test
    void testSubtract_secondNull() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> result = CollectionUtils.subtract(list1, null);
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void testSubtract() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("b", "c", "d");
        List<String> result = CollectionUtils.subtract(list1, list2);
        assertThat(result).containsExactly("a");
    }

    @Test
    void testSubtract_allRemoved() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> result = CollectionUtils.subtract(list1, list2);
        assertThat(result).isEmpty();
    }

    // ==================== contains() 测试 ====================

    @Test
    void testContains_nullCollection() {
        assertThat(CollectionUtils.contains(null, "a")).isFalse();
    }

    @Test
    void testContains_emptyCollection() {
        assertThat(CollectionUtils.contains(new ArrayList<>(), "a")).isFalse();
    }

    @Test
    void testContains_found() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.contains(list, "b")).isTrue();
    }

    @Test
    void testContains_notFound() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThat(CollectionUtils.contains(list, "d")).isFalse();
    }

    @Test
    void testContains_nullElement() {
        List<String> list = Arrays.asList("a", null, "c");
        assertThat(CollectionUtils.contains(list, null)).isTrue();
    }
}
