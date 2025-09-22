package com.simple.common.core.utils;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 树形结构工具类
 *
 * @author 兄台丶请冷静
 */
public class RecursiveUtils {

    //定义递归的第一级父节点数据
    public static final String initial_id = "0000000000";

    //定义id
    private static final String id_key = "id";

    //定义父id
    private static final String parentId_key = "parentId";

    //定义子集合名称
    private static final String list_obj = "list";

    //定义排序字段
    private static final String serial = "serial";

    /**
     * 自动递归
     * 注意，手写sql的时候，查询的范围必须是 T 一一对应，多张表连接查询不能直接返回*
     *
     * @param list 需要递归的集合
     * @param <T>  需要排序的数据对象
     */
    public static <T> List<Tree<String>> get(List<T> list) {
        return get(list, serial);
    }

    /**
     * 自动递归
     * 注意，手写sql的时候，查询的范围必须是 T 一一对应，多张表连接查询不能直接返回*
     *
     * @param list        需要递归的集合
     * @param childrenKey 顺序字段
     * @param <T>         需要排序的数据对象
     */
    public static <T> List<Tree<String>> get(List<T> list, String childrenKey) {

        //配置递归参数(返回数据的核心字段参数名称)
        TreeNodeConfig treeNodeConfig = new TreeNodeConfig();

        //id
        treeNodeConfig.setIdKey(id_key);

        //父id
        treeNodeConfig.setParentIdKey(parentId_key);

        //子节点对应名称
        treeNodeConfig.setChildrenKey(list_obj);

        //顺序字段
        treeNodeConfig.setWeightKey(childrenKey);

        return TreeUtil.build(list, initial_id, treeNodeConfig, (baseRecursive, tree) -> {
            tree.setId(id_key);
            tree.setParentId(parentId_key);
            tree.setWeight(childrenKey);
            autoInject(tree, baseRecursive);
        });
    }

    /**
     * 自动注入方法，接受泛型参数
     */
    @SneakyThrows
    private static <T> void autoInject(Tree<String> tree, T base) {

        // 使用反射获取 base 的所有属性
        Field[] fields = base.getClass().getDeclaredFields();

        for (Field field : fields) {

            // 允许访问私有属性
            field.setAccessible(true);

            // 获取属性值
            Object value = field.get(base);
            if (value != null) {

                // 将属性值注入到 tree 中
                tree.putExtra(field.getName(), value);
            }
        }
    }
}
