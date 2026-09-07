package com.simple.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 树形结构工具类
 *
 * @author qty
 */
public class RecursiveUtils {

    // 定义默认的递归第一级父节点数据
    public static final String initial_id = "0000000000";

    // 定义id字段名
    private static final String ID_KEY = "id";

    // 定义父id字段名
    private static final String PARENT_ID_KEY = "parentId";

    // 定义子集合名称
    private static final String CHILDREN_KEY = "list";

    // 定义排序字段
    private static final String SERIAL_KEY = "serial";

    /**
     * 自动递归（从数据的最高层级开始构建树）
     * 注意，手写sql的时候，查询的范围必须是 T 一一对应，多张表连接查询不能直接返回*
     *
     * @param list 需要递归的集合
     * @param <T>  需要排序的数据对象
     * @return 构建完成的多棵树（森林）
     */
    public static <T> List<Tree<String>> get(List<T> list) {
        return get(list, SERIAL_KEY);
    }

    /**
     * 自动递归（从数据的最高层级开始构建树）
     * 注意，手写sql的时候，查询的范围必须是 T 一一对应，多张表连接查询不能直接返回*
     *
     * @param list      需要递归的集合
     * @param weightKey 排序权重字段名
     * @param <T>       需要排序的数据对象
     * @return 构建完成的多棵树（森林）
     */
    public static <T> List<Tree<String>> get(List<T> list, String weightKey) {
        return buildTreeFromTopLevel(list, weightKey);
    }

    /**
     * 从固定根节点开始构建树
     *
     * @param list 需要递归的集合
     * @param <T>  需要排序的数据对象
     * @return 以默认根节点构建的树
     */
    public static <T> List<Tree<String>> getFromRoot(List<T> list) {
        return getFromRoot(list, SERIAL_KEY, initial_id);
    }

    /**
     * 从指定根节点开始构建树
     *
     * @param list      需要递归的集合
     * @param weightKey 排序权重字段名
     * @param rootId    根节点ID
     * @param <T>       需要排序的数据对象
     * @return 以指定根节点构建的树
     */
    public static <T> List<Tree<String>> getFromRoot(List<T> list, String weightKey, String rootId) {
        return buildTreeFromRoot(list, weightKey, rootId);
    }

    /**
     * 从数据的最高层级开始构建树
     */
    private static <T> List<Tree<String>> buildTreeFromTopLevel(List<T> list, String weightKey) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        // 配置递归参数
        TreeNodeConfig treeNodeConfig = createTreeNodeConfig(weightKey);

        // 自动识别顶级节点（父ID为null、空字符串、0，或者父ID不在现有ID集合中的节点）
        Set<String> allIds = list.stream().map(item -> getFieldValue(item, ID_KEY)).filter(Objects::nonNull).map(Object::toString).collect(Collectors.toSet());

        Set<Object> topLevelItems = list.stream().map(t ->  getFieldValue(t, PARENT_ID_KEY)).filter(parentId -> parentId == null || parentId.toString().isEmpty() || "0".equals(parentId.toString()) || !allIds.contains(parentId.toString())).collect(Collectors.toSet());

        // 如果有顶级节点，从顶级节点开始构建
        if (CollUtil.isNotEmpty(topLevelItems)) {
            return buildTreeFromMultipleRoots(list, topLevelItems, treeNodeConfig, weightKey);
        }

        // 如果没有明确的顶级节点，回退到从固定根节点构建
        return buildTreeFromRoot(list, weightKey, initial_id);
    }

    /**
     * 从多个根节点构建森林（多棵树）
     */
    private static <T> List<Tree<String>> buildTreeFromMultipleRoots(List<T> allItems, Set<Object> rootItems, TreeNodeConfig config, String weightKey) {
        List<Tree<String>> forest = new ArrayList<>();

        for (Object rootItem : rootItems) {
            if(rootItem != null) {
                List<Tree<String>> tree = TreeUtil.build(allItems, rootItem.toString(), config, (node, treeNode) -> {
                    configureTreeNode(treeNode, node, weightKey);
                });
                if (CollUtil.isNotEmpty(tree)) {
                    forest.addAll(tree);
                }
            }

        }

        return forest;
    }

    /**
     * 从固定根节点构建树
     */
    private static <T> List<Tree<String>> buildTreeFromRoot(List<T> list, String weightKey, String rootId) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        TreeNodeConfig treeNodeConfig = createTreeNodeConfig(weightKey);

        return TreeUtil.build(list, rootId, treeNodeConfig, (node, tree) -> {
            configureTreeNode(tree, node, weightKey);
        });
    }

    /**
     * 创建树节点配置
     */
    private static TreeNodeConfig createTreeNodeConfig(String weightKey) {
        TreeNodeConfig config = new TreeNodeConfig();
        config.setIdKey(ID_KEY);
        config.setParentIdKey(PARENT_ID_KEY);
        config.setChildrenKey(CHILDREN_KEY); // 建议使用更标准的children
        config.setWeightKey(weightKey);
        return config;
    }

    /**
     * 配置树节点
     */
    private static <T> void configureTreeNode(Tree<String> tree, T node, String weightKey) {
        tree.setId(ID_KEY);
        tree.setParentId(PARENT_ID_KEY);
        tree.setWeight(weightKey);
        autoInject(tree, node);
    }

    /**
     * 自动注入方法，接受泛型参数，字段沿继承链逐级向上收集
     */
    @SneakyThrows
    private static <T> void autoInject(Tree<String> tree, T base) {
        // 沿继承链逐级向上遍历，保证继承基类实体的字段也能注入
        Class<?> clazz = base.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(base);
                if (value != null) {
                    tree.putExtra(field.getName(), value);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 获取字段值（辅助方法），字段沿继承链逐级向上查找
     */
    @SneakyThrows
    private static <T> Object getFieldValue(T obj, String fieldName) {
        // 沿继承链逐级向上查找字段，找到即取值
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                // 当前类未声明该字段，继续向父类查找
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 获取所有顶级节点（工具方法，供外部使用）
     *
     * @param list 需要筛选的集合
     * @param <T>  数据对象类型
     * @return 顶级节点集合
     */
    public static <T> List<T> getTopLevelNodes(List<T> list) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        Set<String> allIds = list.stream().map(item -> getFieldValue(item, ID_KEY)).filter(Objects::nonNull).map(Object::toString).collect(Collectors.toSet());

        return list.stream().filter(item -> {
            Object parentId = getFieldValue(item, PARENT_ID_KEY);
            return parentId == null || parentId.toString().isEmpty() || "0".equals(parentId.toString()) || !allIds.contains(parentId.toString());
        }).collect(Collectors.toList());
    }
}