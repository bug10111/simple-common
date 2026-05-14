package com.simple.common.mp.page;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simple.common.core.utils.AssertUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

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

    @Schema(description = "当前页")
    @NotNull(message = "当前页不能为空")
    private Integer current = 1;

    @Schema(description = "每页显示条数，<0的时候默认查询所有")
    @NotNull(message = "每页显示条数不能为空")
    private Integer size = 10;

    @Schema(description = "排序，格式为字段名-true/false，true代表正序，false代表倒序，字段名为驼峰")
    @NotNull(message = "排序不能为空")
    private String[] sort = new String[] {};

    /**
     * 获取分页参数对象
     *
     * @return 分页对象，泛型类型为未知类型
     */
    public Page<?> getPage() {
        return createPage();
    }

    /**
     * 获取指定类型的分页参数对象
     *
     * @param clazz 实体类类型
     * @param <T>   实体类型
     * @return 分页对象，泛型类型为指定的实体类型
     */
    public <T> Page<T> getPage(Class<T> clazz) {
        return createPage();
    }

    /**
     * 创建分页对象并设置排序规则
     * <p>
     * 内部方法，处理分页参数和排序逻辑，包含SQL注入检测。
     * 排序格式：字段名-true/false，true为正序，false为倒序，字段名为驼峰命名。
     *
     * @param <T> 实体类型
     * @return 配置好分页参数和排序规则的 Page 对象
     */
    private <T> Page<T> createPage() {
        current = current == null ? 1 : current;
        size = size == null ? 10 : size;
        AssertUtils.isTrue(size <= 1000, "每页显示条数不能超过1000条");

        Page<T> tPage = new Page<>(current, size);
        if (sort != null && sort.length > 0) {
            List<OrderItem> orders = Arrays.stream(sort).map(s -> {
                String[] split = s.split("-");
                AssertUtils.isTrue(split.length == 2, "排序字段格式必须为：字段名-true/false");
                OrderItem item = new OrderItem();
                if (SqlInjectionUtils.check(split[0])) {
                    log.error("动态排序失败！检测到SQL注入语句 [{}]", split[0]);
                    AssertUtils.error("请求错误");
                } else {
                    item.setColumn(StrUtil.toUnderlineCase(StrUtil.replace(split[0], " ", "")));
                }
                item.setAsc(Boolean.parseBoolean(split[1]));

                return item;
            }).toList();

            tPage.setOrders(orders);
        }

        return tPage;
    }
}
