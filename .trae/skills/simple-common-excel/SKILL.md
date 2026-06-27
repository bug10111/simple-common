---
name: "simple-common-excel"
description: "Excel 导入导出模块。提供 EasyExcelWriteService 写入（全量写入/流式分批写入，导出到浏览器或输出流）、EasyExcelReadService 从 MultipartFile/文件路径/输入流读取、PoiWriteService 复杂导出（自定义列宽/合并单元格/分页Sheet）。当需要 Excel 操作时使用。"
---

# simple-common-excel 认知文档

**Maven**: `simple-common-excel`
**包路径**: `com.simple.common.excel`

## EasyExcelWriteService — 写入/导出（推荐）

```java
@Autowired
private EasyExcelWriteService writeService;

// 导出到浏览器下载（最常用）
@GetMapping("/export/users")
public void exportUsers(HttpServletResponse response) {
    List<UserExportDTO> list = userService.findAllForExport();
    writeService.writeResponse(UserExportDTO.class, list, "用户列表");
    // 浏览器下载 "用户列表.xlsx"
}

// 导出到输出流（用于上传OSS/邮件附件等）
ByteArrayOutputStream os = writeService.writeOutputStream(UserExportDTO.class, list);
ByteArrayInputStream is = writeService.writeInputStream(UserExportDTO.class, list);
```

```java
<T> ByteArrayOutputStream writeOutputStream(ByteArrayOutputStream outputStream, Class<T> clazz, List<T> data);
<T> void writeResponse(Class<T> clazz, List<T> data, String writeName);        // writeName不含扩展名
<T> ByteArrayOutputStream writeOutputStream(Class<T> clazz, List<T> data);     // 自动创建输出流
<T> ByteArrayInputStream writeInputStream(Class<T> clazz, List<T> data);       // 返回输入流
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `clazz` | `Class<T>` | 数据实体类，需用 `@ExcelProperty` 标注列 |
| `data` | `List<T>` | 待导出的数据集合 |
| `writeName` | `String` | 下载文件名（不含 `.xlsx` 扩展名，自动添加） |
| `outputStream` | `ByteArrayOutputStream` | 目标输出流，为null则自动创建 |

### 流式分批写入（大数据量场景）

**适用场景**：百万级数据导出，边查边写，避免内存溢出。

```java
@Autowired
private EasyExcelWriteService writeService;

@GetMapping("/export/large")
public void exportLargeData(HttpServletResponse response) {
    WriteContext<ExportRow> ctx = writeService.createWriter(
        response.getOutputStream(), ExportRow.class, "大数据导出");

    int pageSize = 2000;
    int pageNum = 0;
    while (true) {
        List<ExportRow> batch = repository.queryBatch(pageNum++, pageSize);
        if (batch.isEmpty()) {
            break;
        }

        // 逐批写入，写入后释放内存
        ctx.getExcelWriter().write(batch, ctx.getWriteSheet());
    }

    // 收尾，关闭底层流
    ctx.getExcelWriter().finish();
}
```

```java
<T> WriteContext<T> createWriter(OutputStream outputStream, Class<T> clazz, String sheetName);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `outputStream` | `OutputStream` | 输出流（如 `response.getOutputStream()`） |
| `clazz` | `Class<T>` | 数据实体类，需用 `@ExcelProperty` 标注列 |
| `sheetName` | `String` | Sheet 名称 |
| 返回值 | `WriteContext<T>` | 封装 `ExcelWriter` 和 `WriteSheet`，调用方自行控制分批写入与 `finish()` |

> ⚠️ 调用方**必须**在写入完成后调用 `ctx.getExcelWriter().finish()` 收尾，否则文件不完整且流不会关闭。

## EasyExcelReadService — 读取/导入

```java
@Autowired
private EasyExcelReadService readService;

// 从MultipartFile读取（推荐，Web上传场景）
@PostMapping("/import/users")
public R<?> importUsers(@RequestParam("file") MultipartFile file) {
    List<UserImportDTO> list = new ArrayList<>();
    readService.read(file, 1, UserImportDTO.class, new ReadListener<UserImportDTO>() {
        @Override
        public void invoke(UserImportDTO data, AnalysisContext context) {
            list.add(data);
        }
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            userService.saveBatch(list);
        }
    });
    return R.ok("导入成功，共" + list.size() + "条");
}

// 从文件路径读取
readService.read("/data/import/users.xlsx", 1, UserImportDTO.class, listener);

// 从输入流读取
readService.read(inputStream, 1, OrderImportDTO.class, listener);
```

```java
<T> void read(String filePath, int headRowNumber, Class<T> head, ReadListener<T> readListener);
<T> void read(InputStream inputStream, int headRowNumber, Class<T> head, ReadListener<T> readListener);
<T> void read(MultipartFile file, int headRowNumber, Class<T> head, ReadListener<T> readListener);  // default方法
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `headRowNumber` | `int` | 表头行数，如1=第1行表头，从第2行读数据 |
| `head` | `Class<T>` | 数据实体类，属性顺序需与Excel列顺序一致（也可传 `Map.class` 动态映射） |
| `readListener` | `ReadListener<T>` | 数据读取监听器，逐行回调 `invoke()` |

## PoiWriteService — POI复杂导出

**适用场景**：自定义列宽、合并单元格、分页导出等复杂需求。

```java
@Autowired
private PoiWriteService poiWriteService;

@GetMapping("/export/report")
public void exportReport(HttpServletResponse response) {
    List<ReportData> list = reportService.findAll();
    String[] headers = {"用户ID", "姓名", "年龄", "部门"};
    Integer[] widths = {20, 30, 15, 25};

    poiWriteService.exportResponse(
        (rowNum, data) -> new Object[]{data.getId(), data.getName(), data.getAge(), data.getDept()},
        list,
        headers,
        widths,
        1000,           // 每页1000行（每个Sheet最大行数，不超过1048576）
        "用户报表"       // 文件名
    );
}
```

```java
<T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list,
    String[] head, Integer[] width, Integer num, ByteArrayOutputStream outputStream);
<T> void exportResponse(PoiExportFunction<T> function, List<T> list,
    String[] head, Integer[] width, Integer num, String excelName);
<T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list,
    String[] head, Integer[] width, Integer num);  // 自动创建输出流
<T> ByteArrayInputStream writeInputStream(PoiExportFunction<T> function, List<T> list,
    String[] head, Integer[] width, Integer num);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `function` | `PoiExportFunction<T>` | 数据填充函数 `(rowNum, data) -> Object[]` |
| `head` | `String[]` | 表头名称数组 |
| `width` | `Integer[]` | 列宽数组（字符单位） |
| `num` | `Integer` | 每个Sheet最大行数，不超过1048576 |
| `excelName` | `String` | 下载文件名（不含扩展名） |

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-excel</artifactId>
</dependency>
```