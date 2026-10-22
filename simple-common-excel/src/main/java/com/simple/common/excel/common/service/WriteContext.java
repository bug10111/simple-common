package com.simple.common.excel.common.service;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;

import java.util.Objects;

/**
 * 流式写入上下文。
 * <p>
 * 封装 EasyExcel 的 {@link ExcelWriter} 和 {@link WriteSheet}，
 * 用于大数据量分批写入场景，支持边查边写，避免内存溢出。
 * 调用方通过 {@link EasyExcelWriteService#createWriter} 获取本实例。
 * </p>
 * <p>
 * 本类实现了 {@link AutoCloseable}：{@link #close()} 委托 {@code excelWriter.finish()}，
 * 推荐 try-with-resources 使用，避免分批写入抛异常时 ExcelWriter 及底层流资源泄漏。
 * </p>
 * <p>
 * 类型参数 {@code <T>} 仅为 API 语义标注（数据类型），不参与编译期校验；
 * 保留它是为了兼容既有 {@code WriteContext<X>} 泛型声明。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * try (WriteContext<DemoData> ctx = writeService.createWriter(
 *         response.getOutputStream(), DemoData.class, "数据导出")) {
 *
 *     while (hasMore) {
 *         List<DemoData> batch = queryBatch();
 *         ctx.getExcelWriter().write(batch, ctx.getWriteSheet());
 *     }
 *     // close() 内部会调用 excelWriter.finish() 收尾
 * }
 * }</pre>
 *
 * @param <T> 数据类型
 * @author qty
 */
public class WriteContext<T> implements AutoCloseable {

    private final ExcelWriter excelWriter;

    private final WriteSheet writeSheet;

    /**
     * 构造流式写入上下文。
     *
     * @param excelWriter EasyExcel 写入器,不能为 null
     * @param writeSheet  Sheet 配置,不能为 null
     */
    public WriteContext(ExcelWriter excelWriter, WriteSheet writeSheet) {
        this.excelWriter = Objects.requireNonNull(excelWriter, "excelWriter 不能为空");
        this.writeSheet = Objects.requireNonNull(writeSheet, "writeSheet 不能为空");
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

    /**
     * 收尾并释放 ExcelWriter（等价于 {@code getExcelWriter().finish()}）。
     * 支持 try-with-resources 语法，任何写入异常路径都会收尾刷盘。
     */
    @Override
    public void close() {
        excelWriter.finish();
    }
}
