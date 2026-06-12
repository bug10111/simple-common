---
name: "simple-common-excel"
description: "Provides complete API documentation for simple-common-excel module (Excel import/export). Invoke when using EasyExcelWriteService, EasyExcelReadService, or PoiWriteService for Excel operations."
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