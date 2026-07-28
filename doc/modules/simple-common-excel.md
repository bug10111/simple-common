# simple-common-excel Excel 导入导出模块

> @author qty
> 版本：1.0.1

## 1. 模块介绍

`simple-common-excel` 是 simple-common 框架的 Excel 导入导出模块，基于阿里巴巴 EasyExcel 和 Apache POI 两套引擎，提供低内存、高性能的 Excel 读写能力。

该模块提供以下核心能力：

- **EasyExcel 写入**：`EasyExcelWriteService` 支持全量写入、流式分批写入，可导出到浏览器下载、输出流、输入流
- **EasyExcel 读取**：`EasyExcelReadService` 支持从 `MultipartFile`、文件路径、输入流读取，逐行回调，适合大数据量导入
- **POI 复杂导出**：`PoiWriteService` 支持自定义列宽、分页 Sheet、函数式单元格填充，基于 `SXSSFWorkbook` 流式写入避免 OOM
- **POI SAX 读取**：`PoiReadService` 基于事件驱动（SAX）解析，适合 GB 级超大文件读取
- **默认读取处理器**：`DefaultEasyExcelReadHandler` / `DefaultPoiReadHandler` 提供批量缓存、异常收集的抽象基类

## 2. Maven 依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-excel</artifactId>
    <version>1.0.1</version>
</dependency>
```

该模块会自动传递引入以下依赖：

- `easyexcel`：阿里巴巴 EasyExcel（排除其自带旧版 POI）
- `poi` + `poi-ooxml`：Apache POI（统一版本管理）
- `commons-io`：解决 POI 5.2.3 兼容性问题
- `simple-common-core`：框架核心基础模块

## 3. 核心功能

| 类名 | 功能描述 | 关键特性 |
|------|---------|---------|
| [`EasyExcelWriteService`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/EasyExcelWriteService.java:43) | EasyExcel 写入服务接口 | 浏览器下载、输出流、输入流、流式分批写入 |
| [`EasyExcelReadService`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/EasyExcelReadService.java:49) | EasyExcel 读取服务接口 | 文件路径、输入流、MultipartFile 三种读取方式 |
| [`PoiWriteService`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/PoiWriteService.java:33) | POI 写入服务接口 | 自定义列宽、分页 Sheet、函数式单元格填充 |
| [`PoiReadService`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/PoiReadService.java:32) | POI 读取服务接口 | SAX 事件驱动解析，多 Sheet 遍历 |
| [`WriteContext`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/WriteContext.java:29) | 流式写入上下文 | 封装 ExcelWriter 和 WriteSheet |
| [`PoiExportFunction`](simple-common-excel/src/main/java/com/simple/common/excel/common/function/PoiExportFunction.java:44) | POI 导出函数式接口 | 自定义每行单元格填充逻辑 |
| [`DefaultEasyExcelReadHandler`](simple-common-excel/src/main/java/com/simple/common/excel/common/handler/DefaultEasyExcelReadHandler.java:22) | EasyExcel 默认读取处理器 | 批量缓存、自动保存、异常处理 |
| [`DefaultPoiReadHandler`](simple-common-excel/src/main/java/com/simple/common/excel/common/handler/DefaultPoiReadHandler.java:17) | POI 默认读取处理器 | 行列范围控制、异常数据收集 |
| [`ExcelConfig`](simple-common-excel/src/main/java/com/simple/common/excel/common/config/ExcelConfig.java:12) | 模块自动配置 | `@ComponentScan` 扫描 `com.simple.common.excel` |

## 4. 配置说明

### 4.1 自动配置

模块通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动注册 [`ExcelConfig`](simple-common-excel/src/main/java/com/simple/common/excel/common/config/ExcelConfig.java:12)，提供以下功能：

- `@Configuration` + `@ComponentScan(basePackages = {"com.simple.common.excel"})`：自动扫描并注册模块内所有 `@Service` Bean

引入依赖后无需任何额外配置，`EasyExcelWriteService`、`EasyExcelReadService`、`PoiWriteService`、`PoiReadService` 均可直接 `@Autowired` 注入使用。

## 5. 核心类与接口详细说明

### 5.1 EasyExcelWriteService 写入服务

**类路径**：`com.simple.common.excel.common.service.EasyExcelWriteService`
**默认实现**：[`DefaultEasyExcelWriteService`](simple-common-excel/src/main/java/com/simple/common/excel/service/DefaultEasyExcelWriteService.java:26)

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `writeOutputStream(ByteArrayOutputStream, Class<T>, List<T>)` | `ByteArrayOutputStream` | 将数据写入指定输出流 |
| `writeResponse(Class<T>, List<T>, String)` | `void` | 导出到浏览器下载（自动设置响应头） |
| `writeOutputStream(Class<T>, List<T>)` | `ByteArrayOutputStream` | 自动创建输出流并写入（default 方法） |
| `writeInputStream(Class<T>, List<T>)` | `ByteArrayInputStream` | 写入并返回输入流（default 方法） |
| `createWriter(OutputStream, Class<T>, String)` | `WriteContext<T>` | 创建流式 ExcelWriter，用于大数据量分批写入 |

**方法参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `outputStream` | `ByteArrayOutputStream` | 目标输出流，为 null 时自动创建 |
| `clazz` | `Class<T>` | 数据实体类，需用 `@ExcelProperty` 标注列 |
| `data` | `List<T>` | 待导出的数据集合 |
| `writeName` | `String` | 下载文件名（不含 `.xlsx` 扩展名，自动添加） |
| `sheetName` | `String` | Sheet 名称 |

**实现细节**（[`DefaultEasyExcelWriteService`](simple-common-excel/src/main/java/com/simple/common/excel/service/DefaultEasyExcelWriteService.java:26)）：

- `writeOutputStream`：调用 `EasyExcel.write(outputStream, clazz).sheet().doWrite(data)` 一次性写入
- `writeResponse`：通过 [`HttpServletUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/HttpServletUtils.java:17) 获取 Response，设置 `Content-Type` 为 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`，文件名经 `URLEncoder.encode` 防止中文乱码，并处理跨域 `Access-Control-Allow-Origin`
- `createWriter`：构建 `ExcelWriter` 和 `WriteSheet`，封装为 [`WriteContext`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/WriteContext.java:29) 返回，调用方自行控制分批 `write()` 和 `finish()`

### 5.2 EasyExcelReadService 读取服务

**类路径**：`com.simple.common.excel.common.service.EasyExcelReadService`
**默认实现**：[`DefaultEasyExcelReadService`](simple-common-excel/src/main/java/com/simple/common/excel/service/DefaultEasyExcelReadService.java:16)

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `read(String, int, Class<T>, ReadListener<T>)` | `void` | 从文件路径读取 |
| `read(InputStream, int, Class<T>, ReadListener<T>)` | `void` | 从输入流读取 |
| `read(MultipartFile, int, Class<T>, ReadListener<T>)` | `void` | 从 MultipartFile 读取（default 方法） |

**方法参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `filePath` | `String` | Excel 文件完整路径 |
| `inputStream` | `InputStream` | Excel 文件输入流 |
| `file` | `MultipartFile` | Spring MVC 上传的文件对象 |
| `headRowNumber` | `int` | 表头行数，如 1 表示第 1 行为表头，从第 2 行开始读取数据 |
| `head` | `Class<T>` | 数据实体类，属性顺序需与 Excel 列顺序一致（也可传 `Map.class` 动态映射） |
| `readListener` | `ReadListener<T>` | 数据读取监听器，逐行回调 `invoke()` |

**实现细节**（[`DefaultEasyExcelReadService`](simple-common-excel/src/main/java/com/simple/common/excel/service/DefaultEasyExcelReadService.java:16)）：

- 内部调用 `EasyExcel.read(source, head, readListener).sheet().headRowNumber(headRowNumber).doRead()`
- 文件流会自动关闭，调用方无需手动处理

### 5.3 PoiWriteService POI 写入服务

**类路径**：`com.simple.common.excel.common.service.PoiWriteService`
**默认实现**：[`DefaultPoiWriteService`](simple-common-excel/src/main/java/com/simple/common/excel/service/DefaultPoiWriteService.java:26)

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `writeOutputStream(PoiExportFunction<T>, List<T>, String[], Integer[], Integer, ByteArrayOutputStream)` | `ByteArrayOutputStream` | 将数据写入指定输出流 |
| `exportResponse(PoiExportFunction<T>, List<T>, String[], Integer[], Integer, String)` | `void` | 导出到浏览器下载 |
| `writeOutputStream(PoiExportFunction<T>, List<T>, String[], Integer[], Integer)` | `ByteArrayOutputStream` | 自动创建输出流并写入（default 方法） |
| `writeInputStream(PoiExportFunction<T>, List<T>, String[], Integer[], Integer)` | `ByteArrayInputStream` | 写入并返回输入流（default 方法） |

**方法参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `function` | `PoiExportFunction<T>` | 数据填充函数，接收 `Row` 和实体对象，填充单元格 |
| `list` | `List<T>` | 数据集合 |
| `head` | `String[]` | 表头名称数组，如 `{"用户ID", "姓名", "年龄"}` |
| `width` | `Integer[]` | 列宽数组（字符单位），如 `{20, 30, 15}`；长度为 1 时所有列共用同一宽度 |
| `num` | `Integer` | 每个 Sheet 的最大行数（不超过 1048576） |
| `excelName` | `String` | 下载文件名（不含扩展名，自动添加 `.xlsx`） |

**实现细节**（[`DefaultPoiWriteService`](simple-common-excel/src/main/java/com/simple/common/excel/service/DefaultPoiWriteService.java:26)）：

- 使用 `SXSSFWorkbook`（流式工作簿），默认内存中只保留 100 行，超出写入磁盘，避免 OOM
- 当 `width.length > 1` 时，通过 [`AssertUtils.isTrue`](simple-common-core/src/main/java/com/simple/common/core/utils/AssertUtils.java:16) 校验 `head.length == width.length`
- 分页逻辑：每 `num` 条数据创建新 Sheet（命名为"第N个工单簿"），每个 Sheet 第 0 行为表头
- 表头样式：楷体、加粗、12号、居中、细边框（由 `setHeadStyle` 方法提供，`protected` 可被子类覆盖）
- `exportResponse`：设置 `Content-Type` 为 `application/octet-stream`，文件名经 `URLEncoder.encode`

### 5.4 PoiReadService POI 读取服务

**类路径**：`com.simple.common.excel.common.service.PoiReadService`
**默认实现**：[`DefaultPoiReadService`](simple-common-excel/src/main/java/com/simple/common/excel/service/DefaultPoiReadService.java:28)

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `read(String, SheetContentsHandler)` | `void` | 从文件路径读取 |
| `read(InputStream, SheetContentsHandler)` | `void` | 从输入流读取 |
| `read(MultipartFile, SheetContentsHandler)` | `void` | 从 MultipartFile 读取（default 方法） |

**方法参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `filename` | `String` | Excel 文件完整路径 |
| `inputStream` | `InputStream` | Excel 文件输入流 |
| `xlsxFile` | `MultipartFile` | Spring MVC 上传的文件对象 |
| `sheetContentsHandler` | `XSSFSheetXMLHandler.SheetContentsHandler` | Sheet 内容处理器，逐行逐单元格回调 |

**实现细节**（[`DefaultPoiReadService`](simple-common-excel/src/main/java/com/simple/common/excel/service/DefaultPoiReadService.java:28)）：

- 基于 SAX 事件驱动解析，通过 `OPCPackage` 打开文件，`XSSFReader` 遍历所有 Sheet
- `execution` 方法：创建 `ReadOnlySharedStringsTable`，遍历 `SheetIterator`，逐 Sheet 调用 `parserSheetXml`
- `parserSheetXml` 方法：使用 `XMLHelper.newXMLReader()` 创建 SAX 解析器，`XSSFSheetXMLHandler` 作为 ContentHandler 解析 Sheet XML
- 支持遍历同一个 Excel 文件下的多个 Sheet

### 5.5 WriteContext 流式写入上下文

**类路径**：`com.simple.common.excel.common.service.WriteContext<T>`

| 字段 | 类型 | 说明 |
|------|------|------|
| `excelWriter` | `ExcelWriter` | EasyExcel 写入器，用于分批写入数据 |
| `writeSheet` | `WriteSheet` | Sheet 配置，指定写入目标 Sheet |

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getExcelWriter()` | `ExcelWriter` | 获取写入器 |
| `getWriteSheet()` | `WriteSheet` | 获取 Sheet 配置 |

> ⚠️ 调用方**必须**在写入完成后调用 `ctx.getExcelWriter().finish()` 收尾，否则文件不完整且流不会关闭。

### 5.6 PoiExportFunction 导出函数式接口

**类路径**：`com.simple.common.excel.common.function.PoiExportFunction<T>`

`@FunctionalInterface` 接口，用于自定义 Excel 导出时的单元格数据填充逻辑。

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `execute(Row row, T entity)` | `void` | 对每一行数据调用，通过 `row.createCell(i).setCellValue(...)` 填充单元格 |

**使用场景**：

- 自定义单元格样式（字体、颜色、边框）
- 数据转换（枚举值转中文、日期格式化）
- 合并单元格
- 条件渲染

### 5.7 DefaultEasyExcelReadHandler 默认读取处理器

**类路径**：`com.simple.common.excel.common.handler.DefaultEasyExcelReadHandler<T>`

抽象类，实现 `ReadListener<T>`，提供批量缓存和自动保存能力。

| 字段 | 类型 | 说明 |
|------|------|------|
| `BATCH_COUNT` | `int` | 批量保存阈值，默认 2000，通过构造方法指定 |
| `cachedDataList` | `List<T>` | 缓存的数据集合 |

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `invoke(T, AnalysisContext)` | `void` | 每条数据解析后调用，加入缓存，达到阈值时调用 `saveData` 并清理 |
| `doAfterAllAnalysed(AnalysisContext)` | `void` | 所有数据解析完成后调用，再次保存剩余数据 |
| `onException(Exception, AnalysisContext)` | `void` | 异常回调，记录日志，单元格转换异常时通过 `AssertUtils.error` 抛出 |
| `invokeHead(Map, AnalysisContext)` | `void` | 读取到表头时调用（debug 级别记录） |
| `saveData(List<T>)` | `void` | **抽象方法**，由子类实现批量保存逻辑 |

### 5.8 DefaultPoiReadHandler 默认读取处理器

**类路径**：`com.simple.common.excel.common.handler.DefaultPoiReadHandler<T>`

抽象类，实现 `XSSFSheetXMLHandler.SheetContentsHandler`，提供行列范围控制和异常数据收集能力。

| 字段 | 类型 | 说明 |
|------|------|------|
| `beginRow` | `int` | 第几行开始读取数据 |
| `beginCell` | `int` | 第几列开始读取数据 |
| `endCell` | `int` | 第几列结束读取数据 |
| `list` | `List<T>` | 解析成功的数据集合（`@Getter`） |
| `error` | `List<String>` | 异常数据信息集合（`@Getter`） |
| `empty` | `String` | 空数据默认填充字符串，值为 `"empty"` |

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `startRow(int)` | `void` | 行开始时清空缓存、行号计数、列号清零 |
| `endRow(int)` | `void` | 行结束时将缓存数据按 `\|@\|` 分割，调用 `handler` 解析为实体 |
| `cell(String, String, XSSFComment)` | `void` | 单元格处理，按行列范围过滤，空值填充 `empty`，特殊空格 `\u00A0` 清理 |
| `handler(String[])` | `T` | **抽象方法**，将一行字符串数组解析为实体对象 |
| `getResults()` | `Boolean` | 返回是否无异常（`error` 集合为空） |

## 6. 使用示例

### 6.1 EasyExcel 导出到浏览器下载

> 示例来源：[`EasyExcelController`](simple-common-test/src/main/java/com/simple/common/test/controller/EasyExcelController.java:37)

```java
@Autowired
private EasyExcelWriteService easyExcelWriteService;

@GetMapping("export")
public void export() {
    List<EasyExcelDemo> list = new ArrayList<>();
    for (int i = 0; i < 4000; i++) {
        EasyExcelDemo demo = new EasyExcelDemo();
        demo.setName("name" + i);
        demo.setParentCode("code" + i);
        demo.setCode("AreaCode" + i);
        demo.setCreateTime(DateTime.now());
        list.add(demo);
    }
    // 浏览器下载 "测试导出.xlsx"
    easyExcelWriteService.writeResponse(EasyExcelDemo.class, list, "测试导出");
}
```

### 6.2 EasyExcel 导出并上传到文件存储

> 示例来源：[`EasyExcelController.exportUrl()`](simple-common-test/src/main/java/com/simple/common/test/controller/EasyExcelController.java:84)

```java
@GetMapping("exportUrl")
public R<UploadResponse> exportUrl() {
    List<EasyExcelDemo> list = new ArrayList<>();
    // ... 构建数据 ...
    ByteArrayInputStream inputStream = easyExcelWriteService.writeInputStream(EasyExcelDemo.class, list);
    UploadResponse response = annexService.upload("区域列表.xlsx", "simple", ShareType.PRIVATE, inputStream);
    return R.ok(response);
}
```

### 6.3 EasyExcel 流式分批写入（大数据量）

```java
@Autowired
private EasyExcelWriteService writeService;

@GetMapping("/export/large")
public void exportLargeData(HttpServletResponse response) throws IOException {
    // 设置响应头
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-disposition", "attachment;filename=large-export.xlsx");

    // 创建流式写入器
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
    // 必须调用 finish() 收尾
    ctx.getExcelWriter().finish();
}
```

### 6.4 EasyExcel 从 MultipartFile 导入

> 示例来源：[`EasyExcelController.importExcel()`](simple-common-test/src/main/java/com/simple/common/test/controller/EasyExcelController.java:68)

```java
@PostMapping("import")
public R<Object> importExcel(MultipartFile file) {
    easyExcelReadService.read(file, 2, EasyExcelDemo.class, new DefaultEasyExcelReadHandler<EasyExcelDemo>(2000) {
        @Override
        protected void saveData(List<EasyExcelDemo> cachedDataList) {
            cachedDataList.forEach(demo -> {
                if (log.isDebugEnabled()) {
                    log.debug("读取到数据：[{}]", JsonUtils.toJsonStr(demo));
                }
            });
        }
    });
    return R.ok();
}
```

### 6.5 EasyExcel 实体类定义

> 示例来源：[`EasyExcelDemo`](simple-common-test/src/main/java/com/simple/common/test/common/entity/excel/EasyExcelDemo.java:28)

```java
@Data
@Accessors(chain = false) // 必须为 false，否则读取数据为空
@HeadRowHeight(20)        // 表头行高
@ColumnWidth(30)          // 列宽
@ContentRowHeight(20)     // 内容行高
@HeadStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
@ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
public class EasyExcelDemo {

    @ExcelProperty(value = {"主标题", "名称"}, index = 0)
    private String name;

    @ExcelProperty(value = {"主标题", "父编码"}, index = 1)
    private String parentCode;

    @ExcelProperty(value = {"主标题", "编码"}, index = 2)
    private String code;

    @DateTimeFormat("yyyy年MM月dd日HH时mm分ss秒")
    @ExcelProperty(value = {"创建时间"}, index = 3)
    private Date createTime;

    @ExcelIgnore
    private String ignore;
}
```

> ⚠️ `@Accessors(chain = true)` 会导致 EasyExcel 读取数据为空，必须设置为 `chain = false`。

### 6.6 POI 复杂导出到浏览器

> 示例来源：[`PoiController.export()`](simple-common-test/src/main/java/com/simple/common/test/controller/PoiController.java:51)

```java
@Autowired
private PoiWriteService poiWriteService;

@GetMapping("export")
public void export() {
    List<SysAreaEntity> list = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        SysAreaEntity entity = new SysAreaEntity();
        entity.setAreaName("name" + i);
        entity.setParentCode("code" + i);
        entity.setAreaCode("AreaCode" + i);
        list.add(entity);
    }

    // 表头和列宽
    String[] head = {"名称", "父编码", "编码", "创建时间"};
    Integer[] width = {30 * 100}; // 长度为1时所有列共用

    // 自定义单元格填充
    PoiExportFunction<SysAreaEntity> function = (row, entity) -> {
        row.createCell(0).setCellValue(entity.getAreaName());
        row.createCell(1).setCellValue(entity.getParentCode());
        row.createCell(2).setCellValue(entity.getAreaCode());
        row.createCell(3).setCellValue(DateTime.now().toString());
    };

    // 导出到浏览器，每页100万行
    poiWriteService.exportResponse(function, list, head, width, 1000000, "区域列表");
}
```

### 6.7 POI 从 MultipartFile 导入

> 示例来源：[`PoiController.importExcel()`](simple-common-test/src/main/java/com/simple/common/test/controller/PoiController.java:78)

```java
@PostMapping("import")
public R<Object> importExcel(MultipartFile file) {
    // 从第2行开始，第1列到第4列
    DefaultPoiReadHandler<SysAreaEntity> handler = new DefaultPoiReadHandler<>(2, 1, 4) {
        @Override
        public SysAreaEntity handler(String[] row) {
            SysAreaEntity entity = new SysAreaEntity();
            entity.setAreaName(row[0]);
            entity.setParentCode(row[1]);
            entity.setAreaCode(row[2]);
            return entity;
        }
    };
    poiReadService.read(file, handler);

    if (handler.getResults()) {
        // 解析成功
        for (SysAreaEntity entity : handler.getList()) {
            log.info(JsonUtils.toJsonStr(entity));
        }
    } else {
        // 返回异常数据信息
        return R.ok(handler.getError());
    }
    return R.ok();
}
```

### 6.8 POI 导出并上传到文件存储

> 示例来源：[`PoiController.exportUrl()`](simple-common-test/src/main/java/com/simple/common/test/controller/PoiController.java:105)

```java
@GetMapping("exportUrl")
public R<UploadResponse> exportUrl() {
    List<SysAreaEntity> list = new ArrayList<>();
    // ... 构建数据 ...

    String[] head = {"名称", "父编码", "编码", "创建时间"};
    Integer[] width = {30 * 100};
    PoiExportFunction<SysAreaEntity> function = (row, entity) -> {
        row.createCell(0).setCellValue(entity.getAreaName());
        row.createCell(1).setCellValue(entity.getParentCode());
        row.createCell(2).setCellValue(entity.getAreaCode());
        row.createCell(3).setCellValue(DateTime.now().toString());
    };

    ByteArrayInputStream inputStream = poiWriteService.writeInputStream(function, list, head, width, 1000000);
    UploadResponse response = annexService.upload("区域列表.xlsx", "simple", null, ShareType.PRIVATE, inputStream);
    return R.ok(response);
}
```

## 7. 扩展点与自定义方式

### 7.1 自定义 EasyExcelWriteService 实现

实现 `EasyExcelWriteService` 接口并注册为 `@Service`，覆盖默认实现：

```java
@Service
public class MyEasyExcelWriteService implements EasyExcelWriteService {

    @Override
    public <T> ByteArrayOutputStream writeOutputStream(ByteArrayOutputStream outputStream, Class<T> clazz, List<T> data) {
        // 自定义写入逻辑，如添加全局样式、水印等
        EasyExcel.write(outputStream, clazz)
                 .registerWriteHandler(new MyStyleStrategy())
                 .sheet()
                 .doWrite(data);
        return outputStream;
    }

    @Override
    public <T> void writeResponse(Class<T> clazz, List<T> data, String writeName) {
        // 自定义响应逻辑
    }

    @Override
    public <T> WriteContext<T> createWriter(OutputStream outputStream, Class<T> clazz, String sheetName) {
        // 自定义流式写入器
        return new WriteContext<>(EasyExcel.write(outputStream, clazz).build(),
                                  EasyExcel.writerSheet(sheetName).build());
    }
}
```

### 7.2 自定义 POI 表头样式

继承 [`DefaultPoiWriteService`](simple-common-excel/src/main/java/com/simple/common/excel/service/DefaultPoiWriteService.java:26) 并覆盖 `setHeadStyle` 方法：

```java
@Service
public class MyPoiWriteService extends DefaultPoiWriteService {

    @Override
    protected CellStyle setHeadStyle(SXSSFWorkbook workbook) {
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setFontName("微软雅黑");
        font.setFontHeight(14);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
```

### 7.3 自定义 DefaultEasyExcelReadHandler

继承 [`DefaultEasyExcelReadHandler`](simple-common-excel/src/main/java/com/simple/common/excel/common/handler/DefaultEasyExcelReadHandler.java:22) 实现 `saveData` 方法：

```java
public class UserImportHandler extends DefaultEasyExcelReadHandler<UserImportDTO> {

    @Autowired
    private UserService userService;

    public UserImportHandler() {
        super(2000); // 每2000条保存一次
    }

    @Override
    protected void saveData(List<UserImportDTO> cachedDataList) {
        userService.saveBatch(cachedDataList);
    }
}
```

### 7.4 自定义 DefaultPoiReadHandler

继承 [`DefaultPoiReadHandler`](simple-common-excel/src/main/java/com/simple/common/excel/common/handler/DefaultPoiReadHandler.java:17) 实现 `handler` 方法：

```java
public class OrderImportHandler extends DefaultPoiReadHandler<Order> {

    public OrderImportHandler() {
        super(2, 1, 5); // 第2行开始，第1列到第5列
    }

    @Override
    public Order handler(String[] row) {
        Order order = new Order();
        order.setOrderNo(row[0]);
        order.setAmount(new BigDecimal(row[1]));
        order.setCustomerName(row[2]);
        order.setPhone(row[3]);
        order.setAddress(row[4]);
        return order;
    }
}
```

### 7.5 自定义 PoiExportFunction

通过 Lambda 表达式或实现接口自定义单元格填充：

```java
// 数据转换：枚举值转中文
PoiExportFunction<User> function = (row, user) -> {
    row.createCell(0).setCellValue(user.getId());
    row.createCell(1).setCellValue(user.getName());

    // 性别转换
    Cell genderCell = row.createCell(2);
    genderCell.setCellValue(user.getGender() == 1 ? "男" : "女");

    // 日期格式化
    Cell dateCell = row.createCell(3);
    dateCell.setCellValue(DateUtils.format(user.getCreateTime(), "yyyy-MM-dd"));
};
```

## 8. 注意事项

### 8.1 EasyExcel 与 POI 选型

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| 简单导出（注解定义列） | EasyExcelWriteService | API 简洁，`@ExcelProperty` 注解即可 |
| 大数据量导出（百万级） | EasyExcelWriteService.createWriter | 流式分批写入，边查边写 |
| 复杂样式导出（合并单元格、自定义列宽） | PoiWriteService | `PoiExportFunction` 灵活控制每个单元格 |
| 简单导入（注解映射） | EasyExcelReadService | `ReadListener` 逐行回调，封装友好 |
| 超大文件导入（GB 级） | PoiReadService | SAX 事件驱动，内存占用最低 |

### 8.2 线程安全

- **`SXSSFWorkbook`**：POI 流式工作簿，非线程安全，每次导出创建新实例，不可跨线程共享。
- **`ExcelWriter`**：EasyExcel 写入器，非线程安全，每次导出创建新实例。
- **`DefaultEasyExcelReadHandler`**：内部 `cachedDataList` 为实例变量，每个读取任务应创建独立的 Handler 实例，不可跨任务复用。
- **`DefaultPoiReadHandler`**：内部 `StringBuilder` 和 `List` 为实例变量，同上。

### 8.3 性能建议

- **批量保存**：`DefaultEasyExcelReadHandler` 默认每 2000 条保存一次，可根据数据库性能调整 `BATCH_COUNT`。
- **流式写入**：百万级数据导出必须使用 `createWriter` 分批写入，避免一次性加载到内存。
- **SXSSFWorkbook 窗口大小**：`DefaultPoiWriteService` 默认内存保留 100 行，可通过继承覆盖调整。
- **文件流关闭**：EasyExcel 的 `read` 和 `write` 方法会自动关闭文件流；POI 的 `SXSSFWorkbook` 在 `write` 后需调用 `close()`（默认实现已处理）。

### 8.4 常见问题

1. **EasyExcel 读取数据为空**：实体类 `@Accessors(chain = true)` 会导致反射 setter 失败，必须设置为 `chain = false`。

2. **POI 列宽不生效**：`width` 数组长度为 1 时所有列共用同一宽度；长度大于 1 时必须与 `head` 数组长度一致，否则抛出断言异常。

3. **POI 导入空单元格**：`DefaultPoiReadHandler` 将空单元格填充为 `"empty"` 字符串，`handler` 方法中需自行处理该占位值。

4. **流式写入未调用 finish()**：使用 `createWriter` 后必须调用 `ctx.getExcelWriter().finish()`，否则文件不完整且输出流不会关闭。

5. **POI SAX 读取仅支持 .xlsx**：`PoiReadService` 基于 `XSSFSheetXMLHandler`，仅支持 `.xlsx` 格式（OOXML），不支持 `.xls`（HSSF）。

6. **EasyExcel 表头行数**：`headRowNumber` 参数表示表头占用的行数。如 Excel 有 1 行表头，传入 1 表示从第 2 行开始读取数据；有 2 行表头则传入 2。

7. **POI 分页 Sheet 命名**：`DefaultPoiWriteService` 自动按"第N个工单簿"命名 Sheet，每个 Sheet 最多 `num` 行数据（不含表头）。
