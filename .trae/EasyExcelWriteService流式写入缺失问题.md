# EasyExcelWriteService 流式写入缺失问题

## 问题描述

`simple-common-excel` 模块的 `EasyExcelWriteService` 当前只暴露了**全量写入** API：

```java
// 当前唯一写入方式 —— 要求一次性传入完整 List<T>
<T> void writeResponse(Class<T> clazz, List<T> data, String writeName);
<T> ByteArrayOutputStream writeOutputStream(Class<T> clazz, List<T> data);
<T> ByteArrayInputStream writeInputStream(Class<T> clazz, List<T> data);
```

## 影响

调用方必须将所有数据**全部加载到内存**后才能写入，大数据量场景下必然 OOM。

以 marker 项目为例：导出 100 万条客户信息，即使数据库查询采用分批策略（2000 条/批），最终仍需累积 100 万条 `ExportMemberGroupExcelRow` 到 `ArrayList` 才能调用 `writeResponse`，导致 ~400MB 内存峰值。

## 根因

EasyExcel 原生 API 本身支持流式分批写入，但框架封装时未暴露此能力。

EasyExcel 原生流式写入方式：

```java
// EasyExcel 原生支持多次 write，边查边写，不累积内存
ExcelWriter excelWriter = EasyExcel.write(outputStream, DemoData.class).build();
WriteSheet writeSheet = EasyExcel.writerSheet("Sheet1").build();

// 第一批
excelWriter.write(batch1, writeSheet);
// 第二批
excelWriter.write(batch2, writeSheet);
// 第 N 批
excelWriter.write(batchN, writeSheet);

excelWriter.finish();  // 收尾，写入末尾
```

## 建议新增 API

在 `EasyExcelWriteService` 中新增流式写入方法，支持 `OutputStream` 模式的 `ExcelWriter` 创建，让调用方自行控制分批写入时机：

```java
/**
 * 创建流式 ExcelWriter（用于大数据量分批写入）
 * <p>
 * 调用方拿到 ExcelWriter 和 WriteSheet 后，可多次调用
 * {@code excelWriter.write(batchData, writeSheet)} 分批写入，
 * 最后调用 {@code excelWriter.finish()} 收尾。
 * 大数据量场景下配合 OutputStream 使用，避免内存溢出。
 *
 * @param outputStream 输出流（如 HttpServletResponse.getOutputStream()）
 * @param clazz        数据实体类（需用 @ExcelProperty 标注列）
 * @param sheetName    Sheet 名称
 * @param <T>          数据类型
 * @return WriteContext 包含 ExcelWriter 和 WriteSheet
 */
<T> WriteContext<T> createWriter(OutputStream outputStream, Class<T> clazz, String sheetName);
```

`WriteContext` 结构：

```java
public class WriteContext<T> {
    private ExcelWriter excelWriter;
    private WriteSheet writeSheet;
    // getter / 构造器
}
```

## 使用示例

```java
// 调用方用法
WriteContext<ExportMemberGroupExcelRow> ctx = excelWriteService
        .createWriter(response.getOutputStream(), ExportMemberGroupExcelRow.class, "会员分组导出");

while (hasMore) {
    List<ExportMemberGroupExcelRow> batch = queryBatch();  // 每批 2000 条
    ctx.getExcelWriter().write(batch, ctx.getWriteSheet()); // 逐批写入，写入后释放
}
ctx.getExcelWriter().finish();  // 收尾
```

## 与现有 API 的关系

现有 `writeResponse` / `writeOutputStream` / `writeInputStream` 保持不变，新增 `createWriter` 作为大数据量场景的补充，两者共存。