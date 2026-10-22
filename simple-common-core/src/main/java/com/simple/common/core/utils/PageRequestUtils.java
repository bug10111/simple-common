package com.simple.common.core.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA
 * Description: 分页数据拉取器
 * <p>终止条件：连续空页达到阈值、短页（说明是最后一页）、达到最大条数限制，或达到最大页数硬上限；
 * 四者任一满足即停，防止回调忽略 page 参数或翻页漏数据时死循环。</p>
 *
 * @author qty
 */
public class PageRequestUtils<T> {

    /**
     * 最大页数硬上限（maxTotal未配置时的最终兜底，防回调实现缺陷导致无限拉取）
     */
    private static final int DEFAULT_MAX_PAGES = 10_000;

    /**
     * 拉取全部数据
     *
     * @param fetchPage      分页拉取函数：(page, size) -> List<T>
     * @param pageSize       每页大小（建议 100~1000，必须为正数）
     * @param maxTotal       最大总条数限制（防无限循环，负数或0表示不限）
     * @param emptyThreshold 连续空页阈值（默认 1，即一页为空就停止）
     * @return 全部数据列表（按拉取顺序去重；不会超出maxTotal）
     */
    public Set<T> fetchAll(Function<PageRequest, List<T>> fetchPage, int pageSize, int maxTotal, int emptyThreshold) {
        return fetchAll(fetchPage, pageSize, maxTotal, emptyThreshold, 0);
    }

    /**
     * 拉取全部数据（完整参数）
     *
     * @param fetchPage      分页拉取函数：(page, size) -> List<T>
     * @param pageSize       每页大小（必须为正数）
     * @param maxTotal       最大总条数限制（负数或0表示不限）
     * @param emptyThreshold 连续空页阈值（正数）
     * @param pageDelayMillis 相邻两页之间的间隔毫秒（限流用；首页不等待，0表示不等）
     * @return 全部数据列表（按拉取顺序去重；不会超出maxTotal）
     */
    public Set<T> fetchAll(Function<PageRequest, List<T>> fetchPage, int pageSize, int maxTotal, int emptyThreshold, long pageDelayMillis) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize必须为正数: " + pageSize);
        }
        if (emptyThreshold <= 0) {
            throw new IllegalArgumentException("emptyThreshold必须为正数: " + emptyThreshold);
        }

        // LinkedHashSet保留拉取顺序，去重语义显式且结果可复现
        Set<T> allData = new LinkedHashSet<>();
        // maxTotal有效时按其推导页数上限，否则用硬上限兜底
        int maxPages = maxTotal > 0 ? (int) Math.min(Integer.MAX_VALUE - 1, (long) Math.ceil((double) maxTotal / pageSize)) : DEFAULT_MAX_PAGES;

        int page = 0;
        int consecutiveEmptyPages = 0;

        while (page < maxPages) {
            if (maxTotal > 0 && allData.size() >= maxTotal) {
                break; // 达到最大限制
            }

            // 仅页间等待（首页不等待），把限流间隔做成显式参数而非硬编码sleep
            if (page > 0 && pageDelayMillis > 0) {
                try {
                    Thread.sleep(pageDelayMillis);
                } catch (InterruptedException e) {
                    // 恢复中断标记后终止，让取消信号向上传播
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("分页拉取被中断", e);
                }
            }

            List<T> pageData;
            try {
                pageData = fetchPage.apply(new PageRequest(page, pageSize));
            } catch (RuntimeException e) {
                throw e; // 回调里的编程错误原样抛出，不再包一层丢失语义
            } catch (Exception e) {
                throw new IllegalStateException("Failed to fetch page " + page, e);
            }

            if (pageData == null || pageData.isEmpty()) {
                consecutiveEmptyPages++;
                if (consecutiveEmptyPages >= emptyThreshold) {
                    break; // 连续空页，认为已结束
                }
            } else {
                consecutiveEmptyPages = 0; // 重置空页计数
                allData.addAll(pageData);

                // 如果当前页不满，说明是最后一页
                if (pageData.size() < pageSize) {
                    break;
                }
            }

            page++;
        }

        // 精确收敛到maxTotal：addAll后可能因本页整页入集而超限，直接截断
        if (maxTotal > 0 && allData.size() > maxTotal) {
            List<T> truncated = new ArrayList<>(allData);
            allData.clear();
            allData.addAll(truncated.subList(0, maxTotal));
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
