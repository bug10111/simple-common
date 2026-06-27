package com.simple.common.excel.common.service;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;

/**
 * 流式写入上下文。
 * <p>
 * 封装 EasyExcel 的 {@link ExcelWriter} 和 {@link WriteSheet}，
 * 用于大数据量分批写入场景，支持边查边写，避免内存溢出。
 * 调用方通过 {@link EasyExcelWriteService#createWriter} 获取本实例。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * WriteContext<DemoData> ctx = writeService.createWriter(
 *     response.getOutputStream(), DemoData.class, "数据导出");
 *
 * while (hasMore) {
 *     List<DemoData> batch = queryBatch();
 *     ctx.getExcelWriter().write(batch, ctx.getWriteSheet());
 * }
 * ctx.getExcelWriter().finish();
 * }</pre>
 *
 * @param <T> 数据类型
 * @author qty
 */
public class WriteContext<T> {

    private final ExcelWriter excelWriter;

    private final WriteSheet writeSheet;

    /**
     * 构造流式写入上下文。
     *
     * @param excelWriter EasyExcel 写入器
     * @param writeSheet  Sheet 配置
     */
    public WriteContext(ExcelWriter excelWriter, WriteSheet writeSheet) {
        this.excelWriter = excelWriter;
        this.writeSheet = writeSheet;
    }

    /**
     * 获取 ExcelWriter，用于分批写入数据。
     *
     * @return ExcelWriter 实例
     */
    public ExcelWriter getExcelWriter() {
        return excelWriter;
    }

    /**
     * 获取 WriteSheet，用于指定写入目标 Sheet。
     *
     * @return WriteSheet 实例
     */
    public WriteSheet getWriteSheet() {
        return writeSheet;
    }
}