package com.simple.common.core.utils;

import cn.hutool.core.util.RandomUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA
 * Description: 分页数据拉取器
 *
 * @author qty
 */
public class PageRequestUtils<T> {

    /**
     * 拉取全部数据
     *
     * @param fetchPage      分页拉取函数：(page, size) -> List<T>
     * @param pageSize       每页大小（建议 100~1000）
     * @param maxTotal       最大总条数限制（防无限循环，默认 -1 表示不限）
     * @param emptyThreshold 连续空页阈值（默认 1，即一页为空就停止）
     * @return 所有数据列表
     */
    public Set<T> fetchAll(Function<PageRequest, List<T>> fetchPage, int pageSize, int maxTotal, int emptyThreshold) {

        Set<T> allData = new HashSet<>();
        int page = 0;
        int consecutiveEmptyPages = 0;

        while (true) {
            if (maxTotal > 0 && allData.size() >= maxTotal) {
                break; // 达到最大限制
            }

            PageRequest request = new PageRequest(page, pageSize);
            List<T> pageData;

            try {
                Thread.sleep(1000 + RandomUtil.randomInt(0, 1000));
                pageData = fetchPage.apply(request);
            } catch (Exception e) {
                // 可扩展：加入重试逻辑或熔断
                throw new RuntimeException("Failed to fetch page " + page, e);
            }

            if (pageData == null || pageData.isEmpty()) {
                consecutiveEmptyPages++;
                if (consecutiveEmptyPages >= emptyThreshold) {
                    break; // 连续空页，认为已结束
                }
            } else {
                consecutiveEmptyPages = 0; // 重置空页计数
                allData.addAll(pageData);

                // 如果当前页不满，说明是最后一页（可选优化）
                if (pageData.size() < pageSize) {
                    break;
                }
            }

            page++;
        }

        return allData;
    }

    // 简化调用
    public Set<T> fetchAll(Function<PageRequest, List<T>> fetchPage, int pageSize) {
        return fetchAll(fetchPage, pageSize, -1, 1);
    }

    // 分页请求封装
    public static class PageRequest {
        public final int page;

        public final int size;

        public PageRequest(int page, int size) {
            this.page = page;
            this.size = size;
        }
    }
}
