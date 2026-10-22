package com.simple.common.mp.page;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simple.common.core.utils.AssertUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 分页参数基类
 * <p>
 * 提供通用的分页参数（当前页、每页大小、排序），支持动态排序和SQL注入检测。
 * 业务查询对象可继承此类，通过 getPage() 方法获取 MyBatis-Plus 的 Page 对象。
 *
 * @author qty
 */
@Slf4j
@Getter
@Setter
@Schema(description = "分页参数基类")
public class PageBase {

    @Schema(description = "当前页，必须大于0")
    @NotNull(message = "当前页不能为空")
    private Integer current = 1;

    @Schema(description = "每页显示条数，必须在1-1000之间，超出范围会被断言拦截")
    @NotNull(message = "每页显示条数不能为空")
    private Integer size = 10;

    @Schema(description = "排序，格式为字段名-true/false，true代表正序，false代表倒序，字段名为驼峰")
    private String[] pageSort = new String[] {};

    /**
     * 获取分页参数对象
     *
     * @return 分页对象，泛型类型为未知类型
     */
    public Page<?> getPage() {
        return createPage(null);
    }

    /**
     * 获取指定实体类型的分页参数对象
     * <p>
     * 提供实体类型时，排序字段会先按实体列元数据（MyBatis-Plus TableInfo）构建的白名单校验，
     * 未在实体中声明的列名直接拒绝，从根上阻断注入面（黑名单检测仅作为兜底）。
     *
     * @param clazz 实体类类型（需已被 MyBatis-Plus 初始化 TableInfo，即存在对应 Mapper）
     * @param <T>   实体类型
     * @return 分页对象，泛型类型为指定的实体类型
     */
    public <T> Page<T> getPage(Class<T> clazz) {
        return createPage(clazz);
    }

    /**
     * 创建分页对象并设置排序规则
     * <p>
     * 排序格式：字段名-true/false，true为正序，false为倒序，字段名为驼峰命名。
     *
     * @param <T> 实体类型
     * @return 配置好分页参数和排序规则的 Page 对象
     */
    private <T> Page<T> createPage(Class<?> entityClass) {
        current = current == null ? 1 : current;
        size = size == null ? 10 : size;
        // 边界双向校验：size<=0（尤其负值）会被 MyBatis-Plus 视为「不分页」，可整表输出
        AssertUtils.isTrue(current > 0, "当前页必须大于0");
        AssertUtils.isTrue(size > 0 && size <= 1000, "每页显示条数必须在1-1000之间");

        Page<T> tPage = new Page<>(current, size);
        if (pageSort != null && pageSort.length > 0) {
            // 实体列白名单：提供实体类型且其 TableInfo 已初始化时，仅允许实体声明的列参与排序
            TableInfo tableInfo = entityClass == null ? null : TableInfoHelper.getTableInfo(entityClass);
            Set<String> allowedColumns = resolveAllowedColumns(tableInfo);

            List<OrderItem> orders = Arrays.stream(pageSort).map(s -> {
                AssertUtils.isTrue(StrUtil.isNotBlank(s), "排序字段不能为空");
                String[] split = s.split("-");
                AssertUtils.isTrue(split.length == 2, "排序字段格式必须为：字段名-true/false");
                String column = StrUtil.toUnderlineCase(StrUtil.replace(split[0], " ", ""));

                // 黑名单注入检测作为兜底防线
                if (SqlInjectionUtils.check(column)) {
                    log.error("动态排序失败！检测到SQL注入语句 [{}]", split[0]);
                    AssertUtils.error("请求错误");
                }
                // 白名单校验：未在实体中声明的列一律拒绝
                if (allowedColumns != null && !allowedColumns.contains(column)) {
                    log.error("动态排序失败！排序字段 [{}] 不在实体 [{}] 的列清单中", split[0], entityClass.getSimpleName());
                    AssertUtils.error("请求错误");
                }

                // 排序方向显式校验，非法取值快速失败而非静默按倒序处理
                String ascFlag = split[1];
                AssertUtils.isTrue("true".equalsIgnoreCase(ascFlag) || "false".equalsIgnoreCase(ascFlag),
                        "排序方向必须为 true 或 false");

                OrderItem item = new OrderItem();
                item.setColumn(column);
                item.setAsc(Boolean.parseBoolean(ascFlag));

                return item;
            }).toList();

            tPage.setOrders(orders);
        }

        return tPage;
    }

    /**
     * 从实体元数据解析允许排序的数据库列名白名单
     *
     * @param tableInfo 实体 TableInfo（可能为 null，实体未注册 Mapper 时尚未初始化）
     * @return 允许排序的列名集合；tableInfo 为 null 时返回 null（退化为仅黑名单校验）
     */
    private Set<String> resolveAllowedColumns(TableInfo tableInfo) {
        if (tableInfo == null) {
            return null;
        }
        Set<String> columns = new HashSet<>();
        if (StrUtil.isNotBlank(tableInfo.getKeyColumn())) {
            columns.add(tableInfo.getKeyColumn());
        }
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            if (StrUtil.isNotBlank(fieldInfo.getColumn())) {
                columns.add(fieldInfo.getColumn());
            }
        }
        return columns;
    }
}
