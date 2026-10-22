package com.simple.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public static final String INITIAL_ID = "0000000000";

    // 定义id字段名
    private static final String ID_KEY = "id";

    // 定义父id字段名
    private static final String PARENT_ID_KEY = "parentId";

    // 定义子集合名称
    private static final String CHILDREN_KEY = "list";

    // 定义排序字段
    private static final String SERIAL_KEY = "serial";

    // 顶级父节点特殊值：父ID为字符串 "0" 时也视为顶级
    private static final String ZERO_PARENT_ID = "0";

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
        return getFromRoot(list, SERIAL_KEY, INITIAL_ID);
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
     * 从数据的最高层级开始构建树（顶级节点 = 父ID为 null/空串/"0"，或父ID不在现有ID集合中的节点）
     */
    private static <T> List<Tree<String>> buildTreeFromTopLevel(List<T> list, String weightKey) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        // 一次性把全部节点解析为 Tree 并建立 id -> 节点 索引（O(n)），避免对每个根重复全量解析
        Map<String, Tree<String>> nodeMap = new LinkedHashMap<>();
        for (T item : list) {
            Tree<String> tree = new Tree<>(createTreeNodeConfig(weightKey));
            configureTreeNode(tree, item, weightKey);
            if (tree.getId() != null) {
                nodeMap.putIfAbsent(tree.getId(), tree);
            }
        }

        // 按 id/parentId 一次性挂接父子关系；父ID为 null/空串/"0" 或父节点不在集合中的节点作为根
        List<Tree<String>> forest = new ArrayList<>();
        for (Tree<String> tree : nodeMap.values()) {
            String parentId = tree.getParentId();
            Tree<String> parent = parentId == null ? null : nodeMap.get(parentId);
            if (isTopLevelParent(parentId, nodeMap.keySet())) {
                forest.add(tree);
            } else {
                parent.addChildren(tree);
            }
        }

        // 权重排序（与 TreeUtil.build 的 weightKey 排序语义保持一致），排序失败时保持插入顺序
        sortSiblings(forest);
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

        return cn.hutool.core.lang.tree.TreeUtil.build(list, rootId, treeNodeConfig, (node, tree) -> {
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
        config.setChildrenKey(CHILDREN_KEY);
        config.setWeightKey(weightKey);
        return config;
    }

    /**
     * 配置树节点：从源节点读取真实的 id/parentId/weight 值写入树节点。
     * <p>注意：这里必须写字段值而不是字段名，id/parentId 由树装配逻辑用于建立父子关系，
     * weight（保留原始 Comparable 类型）用于兄弟节点排序。</p>
     */
    private static <T> void configureTreeNode(Tree<String> tree, T node, String weightKey) {
        tree.setId(toStringOrNull(getFieldValue(node, ID_KEY)));
        tree.setParentId(toStringOrNull(getFieldValue(node, PARENT_ID_KEY)));
        tree.putExtra(weightKey, getFieldValue(node, weightKey));
        autoInject(tree, node);
    }

    /**
     * 自动注入方法，接受泛型参数，字段沿继承链逐级向上收集。
     * <p>跳过 static 与编译器合成字段（如 this$0、$jacocoData），且父类字段不覆盖子类同名注入值，
     * 避免内部状态或敏感字段（如密码列）被原样带出。</p>
     */
    private static <T> void autoInject(Tree<String> tree, T base) {
        if (base == null) {
            return;
        }
        // 沿继承链逐级向上遍历，保证继承基类实体的字段也能注入；子类字段优先（先注入后访问的被覆盖）
        List<Field> fields = new ArrayList<>();
        Class<?> clazz = base.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(base);
                if (value != null) {
                    tree.putExtra(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("读取字段失败: " + field.getDeclaringClass().getName() + "." + field.getName(), e);
            }
        }
    }

    /**
     * 获取字段值（辅助方法），字段沿继承链逐级向上查找，找不到返回 null
     */
    private static <T> Object getFieldValue(T obj, String fieldName) {
        if (obj == null) {
            return null;
        }
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
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("读取字段失败: " + clazz.getName() + "." + fieldName, e);
            }
        }
        return null;
    }

    /**
     * 顶级节点判定（供建树与 getTopLevelNodes 共用）：父ID为 null/空串/"0"，或父ID不在现有ID集合中
     */
    private static boolean isTopLevelParent(String parentId, Set<String> allIds) {
        return parentId == null || parentId.isEmpty() || ZERO_PARENT_ID.equals(parentId) || !allIds.contains(parentId);
    }

    /**
     * 对每层子节点按 weight 排序；weight 为 null 或不可比较时保持插入顺序
     */
    private static void sortSiblings(List<Tree<String>> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return;
        }
        try {
            nodes.sort((a, b) -> {
                Comparable<?> weightA = weightOf(a);
                Comparable<?> weightB = weightOf(b);
                if (weightA == null && weightB == null) {
                    return 0;
                }
                if (weightA == null) {
                    return 1;
                }
                if (weightB == null) {
                    return -1;
                }
                return compareWeight(weightA, weightB);
            });
        } catch (ClassCastException ignore) {
            // weight 类型不可比较时保持插入顺序
        }
        for (Tree<String> node : nodes) {
            sortSiblings(node.getChildren());
        }
    }

    private static Comparable<?> weightOf(Tree<String> node) {
        Object weight = node.get(node.getConfig().getWeightKey());
        return weight instanceof Comparable ? (Comparable<?>) weight : null;
    }

    /**
     * 权重比较（weight 保留原始 Comparable 类型，数值字段按数值序而非字典序）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compareWeight(Comparable<?> a, Comparable<?> b) {
        return ((Comparable) a).compareTo(b);
    }

    private static String toStringOrNull(Object value) {
        return value == null ? null : value.toString();
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

        Set<String> allIds = list.stream().map(item -> toStringOrNull(getFieldValue(item, ID_KEY))).filter(Objects::nonNull).collect(Collectors.toSet());

        return list.stream().filter(item -> {
            String parentId = toStringOrNull(getFieldValue(item, PARENT_ID_KEY));
            return isTopLevelParent(parentId, allIds);
        }).collect(Collectors.toList());
    }
}
