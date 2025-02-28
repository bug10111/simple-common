package com.simple.common.mp.page;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Getter
@Setter
@Schema(description = "分页参数基类")
public class PageBase {

    @Schema(description = "当前页")
    private long current = 1;

    @Schema(description = "每页显示条数，<0的时候默认查询所有")
    private long size = 10;

    @Schema(description = "排序，格式为字段名-true/false，true代表正序，false代表倒序，字段名为驼峰")
    private String[] sort = new String[] {};

    /**
     * 获取分页参数对象
     *
     * @return 分页对象
     */
    public Page<?> getPage() {
        Page<?> tPage = new Page<>(this.current, this.size);
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
        return tPage;
    }

    /**
     * 获取分页参数对象
     *
     * @param tClass 预留
     * @return 分页对象
     */
    @Deprecated
    public <R> Page<R> getPage(Class<R> tClass) {
        Page<R> tPage = new Page<>(this.current, this.size);
        List<OrderItem> orders = Arrays.stream(sort).map(s -> {
            String[] split = s.split("-");
            AssertUtils.isTrue(split.length == 2, "排序字段格式必须为：字段名-true/false");
            OrderItem item = new OrderItem();
            if (SqlInjectionUtils.check(split[0])) {
                log.error("动态排序失败！检测到SQL注入语句 [{}]", split[0]);
                AssertUtils.error("请求错误");
            } else {
                item.setColumn(StrUtil.toUnderlineCase(split[0]));
            }
            item.setAsc(Boolean.parseBoolean(split[1]));

            return item;
        }).toList();

        tPage.setOrders(orders);
        return tPage;
    }
}
