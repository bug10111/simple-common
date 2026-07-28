# simple-common-doc Word 模板替换模块

> @author qty
> 版本：1.0.1

## 1. 模块介绍

`simple-common-doc` 是 simple-common 框架的 Word 文档模板替换模块，基于 [Poi-Tl](https://deepoove.com/poi-tl/) 模板引擎实现，用于根据业务数据动态生成 `.docx` 格式的 Word 文档。

该模块提供以下核心能力：

- **模板数据构建**：[`Docs.builder()`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:20) 链式构建模板参数，支持文本、带颜色文本、带超链接文本、本地图片、图片流、网络图片、表格、列表等多种元素
- **模板替换输出**：[`DocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/common/service/DocReplaceService.java:55) 提供多种输出方式，包括浏览器下载、输出流、输入流，支持从 resources 目录或输入流加载模板
- **模板引擎抽象**：[`DocTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/common/manager/DocTemplateReplaceManager.java:46) 接口抽象模板替换引擎，默认基于 Poi-Tl 实现，可扩展为其他引擎

**典型使用场景**：

- 合同生成：根据订单数据动态生成合同文档供用户下载
- 报告导出：生成包含图表和数据的分析报告
- 证书制作：批量生成荣誉证书、培训证书等
- 通知函件：自动生成个性化的通知、邀请函等

## 2. Maven 依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-doc</artifactId>
    <version>1.0.1</version>
</dependency>
```

该模块会自动传递引入以下依赖：

- `poi-tl`：Poi-Tl Word 模板引擎（排除其自带的旧版 POI）
- `poi` / `poi-ooxml`：Apache POI（统一版本管理）
- `simple-common-core`：框架核心基础模块

## 3. 核心功能

| 类名 | 功能描述 | 关键特性 |
|------|---------|---------|
| [`Docs`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:18) | 模板数据构建器 | 链式 API、线程安全（ConcurrentHashMap）、支持文本/图片/表格/列表/超链接/颜色 |
| [`DocFunction`](simple-common-doc/src/main/java/com/simple/common/doc/common/function/DocFunction.java:14) | 表格行数据函数式接口 | `@FunctionalInterface`，将业务对象转换为表格行数据 |
| [`DocTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/common/manager/DocTemplateReplaceManager.java:46) | 模板替换管理器接口 | 抽象模板替换引擎，支持扩展 |
| [`PoiTlTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/manager/PoiTlTemplateReplaceManager.java:22) | Poi-Tl 模板替换实现 | 基于 `XWPFTemplate.compile().render()` 实现模板替换 |
| [`DocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/common/service/DocReplaceService.java:55) | 文档替换服务接口 | 提供浏览器下载、输出流、输入流等多种输出方式 |
| [`DefaultDocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/service/DefaultDocReplaceService.java:19) | 文档替换默认实现 | 委托 `DocTemplateReplaceManager` 完成替换 |
| [`DocConfig`](simple-common-doc/src/main/java/com/simple/common/doc/common/config/DocConfig.java:13) | 自动配置类 | `@ComponentScan` 扫描 `com.simple.common.doc` 包 |

## 4. 模板语法

Poi-Tl 模板使用 `{{ }}` 语法标记占位符，不同前缀对应不同类型的元素：

| 占位符格式 | 用途 | Docs Builder 方法 | 示例 |
|-----------|------|-------------------|------|
| `{{code}}` | 普通文本 | `addStr` / `addStrLink` / `addStrColor` | `{{contractNo}}` |
| `{{@code}}` | 图片 | `addImgLocal` / `addImgInputStream` / `addImgInputUrl` | `{{@seal}}` |
| `{{#code}}` | 表格 | `addTable` | `{{#orderTable}}` |
| `{{*code}}` | 列表 | `addList` | `{{*itemList}}` |

**模板制作说明**：

1. 在 Word 文档（`.docx`）中，将占位符直接输入到需要替换的位置
2. 占位符名称（`code`）需与 [`Docs.builder()`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:20) 中传入的 `key` 一致
3. 模板文件放置在 `src/main/resources/` 目录下，通过 `templatePath` 参数指定路径（如 `"/templates/contract.docx"`）

## 5. 核心类与接口详细说明

### 5.1 Docs 模板数据构建器

**类路径**：`com.simple.common.doc.common.builder.Docs`

[`Docs`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:18) 是一个 `final` 工具类，通过 [`Docs.builder()`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:20) 获取 [`DocBuilder`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:24) 实例，链式构建模板数据。

**线程安全**：`DocBuilder` 内部使用 `ConcurrentHashMap` 存储模板数据，线程安全。

#### DocBuilder 方法清单

##### 文本类方法

| 方法签名 | 模板格式 | 说明 |
|---------|---------|------|
| `addStr(String key, String value)` | `{{key}}` | 添加普通文本 |
| `addStrLink(String key, String value, String link)` | `{{key}}` | 添加带超链接的文本 |
| `addStrColor(String key, String value, String color)` | `{{key}}` | 添加带颜色的文本，`color` 为十六进制颜色值（如 `"ff0000"`） |
| `addStr(String key, TextRenderData value)` | `{{key}}` | 添加 Poi-Tl `TextRenderData` 文本（高级用法） |

##### 图片类方法

| 方法签名 | 模板格式 | 说明 |
|---------|---------|------|
| `addImgLocal(String key, String url, int width, int height)` | `{{@key}}` | 添加本地图片（`url` 为本地文件路径） |
| `addImgInputStream(String key, InputStream inputStream, int width, int height)` | `{{@key}}` | 添加图片输入流 |
| `addImgInputUrl(String key, String url, int width, int height)` | `{{@key}}` | 添加网络图片（`url` 为图片 URL） |
| `addImg(String key, PictureRenderData value)` | `{{@key}}` | 添加 Poi-Tl `PictureRenderData` 图片（高级用法） |

##### 表格类方法

| 方法签名 | 模板格式 | 说明 |
|---------|---------|------|
| `addTable(String key, String percentWidth, String[] head, List<T> list, DocFunction<T> function)` | `{{#key}}` | 添加表格（默认字体大小 15） |
| `addTable(String key, String percentWidth, int size, String[] head, List<T> list, DocFunction<T> function)` | `{{#key}}` | 添加表格（指定字体大小 `size`） |

**表格参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | `String` | 模板占位符名称 |
| `percentWidth` | `String` | 表格宽度百分比，如 `"90%"` |
| `size` | `int` | 字体大小（默认 15） |
| `head` | `String[]` | 表头数组 |
| `list` | `List<T>` | 表格数据列表 |
| `function` | `DocFunction<T>` | 行数据转换函数，将 `T` 转换为 `String[]` |

##### 列表类方法

| 方法签名 | 模板格式 | 说明 |
|---------|---------|------|
| `addList(String key, List<String> list)` | `{{*key}}` | 添加列表（默认圆点格式 `NumberingFormat.BULLET`） |
| `addList(String key, NumberingFormat numberingFormat, List<String> list)` | `{{*key}}` | 添加列表（指定编号格式） |

**列表参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | `String` | 模板占位符名称 |
| `numberingFormat` | `NumberingFormat` | 列表编号格式（Poi-Tl 枚举，如 `BULLET` 圆点、`DECIMAL` 数字） |
| `list` | `List<String>` | 列表数据 |

> **空列表处理**：当 `list` 为 `null` 或空时，会放入空的 `Numberings.create()`，不会报错。

##### 构建结果

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `create()` | `Map<String, Object>` | 获取模板参数集，传给 `DocReplaceService` |

### 5.2 DocFunction 表格行数据函数

**类路径**：`com.simple.common.doc.common.function.DocFunction`

[`DocFunction`](simple-common-doc/src/main/java/com/simple/common/doc/common/function/DocFunction.java:14) 是一个 `@FunctionalInterface` 函数式接口，用于将业务对象转换为表格行数据（`String[]`）。

```java
@FunctionalInterface
public interface DocFunction<T> {
    String[] createRow(T t);
}
```

**使用方式**：配合 [`DocBuilder.addTable()`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:134) 使用，通过 Lambda 表达式实现：

```java
DocFunction<OrderItem> function = item -> new String[]{
    item.getSeq(),
    item.getName(),
    item.getQty().toString(),
    item.getPrice().toString(),
    item.getAmount().toString()
};
builder.addTable("orderTable", "90%", new String[]{"序号", "商品", "数量", "单价", "金额"}, orderItems, function);
```

### 5.3 DocTemplateReplaceManager 模板替换管理器

**类路径**：`com.simple.common.doc.common.manager.DocTemplateReplaceManager`

[`DocTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/common/manager/DocTemplateReplaceManager.java:46) 是模板替换引擎的抽象接口，定义了核心替换方法：

```java
void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `inputStream` | `InputStream` | Word 文档模板输入流 |
| `outputStream` | `OutputStream` | 替换后的文档输出流 |
| `values` | `Map<String, Object>` | 模板参数（通过 [`Docs.builder().create()`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:205) 构建） |

> 该方法会自动关闭输入流和输出流。

### 5.4 PoiTlTemplateReplaceManager Poi-Tl 实现

**类路径**：`com.simple.common.doc.manager.PoiTlTemplateReplaceManager`

[`PoiTlTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/manager/PoiTlTemplateReplaceManager.java:22) 是 [`DocTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/common/manager/DocTemplateReplaceManager.java:46) 的默认实现，基于 Poi-Tl 的 `XWPFTemplate` 完成模板替换：

```java
@Override
@SneakyThrows
public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
    XWPFTemplate template = XWPFTemplate.compile(inputStream).render(values);
    template.writeAndClose(outputStream);
}
```

- `XWPFTemplate.compile(inputStream)`：从输入流编译模板
- `.render(values)`：渲染模板，替换占位符
- `template.writeAndClose(outputStream)`：写入输出流并关闭

### 5.5 DocReplaceService 文档替换服务

**类路径**：`com.simple.common.doc.common.service.DocReplaceService`

[`DocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/common/service/DocReplaceService.java:55) 是面向业务的高级服务接口，提供了多种便捷的输出方式。其中 `replace` 为唯一抽象方法，其余为 `default` 方法。

#### 方法清单

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `replace(InputStream, OutputStream, Map)` | `void` | 基础替换方法（抽象方法） |
| `replaceResponse(String name, InputStream, Map)` | `void` | 从流加载模板 + 浏览器下载 |
| `replaceResponse(String name, String templatePath, Map)` | `void` | 从 resources 加载模板 + 浏览器下载 |
| `replaceAndGetInputStream(String templatePath, Map)` | `ByteArrayInputStream` | 从 resources 加载模板 + 返回输入流 |
| `replaceAndGetOutputStream(String templatePath, Map)` | `ByteArrayOutputStream` | 从 resources 加载模板 + 返回输出流 |

#### 方法参数说明

| 参数 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 下载文件名（不含 `.docx` 扩展名，方法会自动添加） |
| `inputStream` | `InputStream` | 模板文件输入流 |
| `outputStream` | `OutputStream` | 替换后文档输出流 |
| `templatePath` | `String` | resources 下模板路径，如 `"/templates/contract.docx"` |
| `values` | `Map<String, Object>` | 模板参数（通过 [`Docs.builder().create()`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:205) 构建） |

#### 方法详解

##### replace — 基础替换

```java
void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values);
```

读取模板输入流，将占位符替换为实际值，写入输出流。该方法会自动关闭输入流和输出流。

##### replaceResponse — 浏览器下载

两个重载方法：

- [`replaceResponse(String name, InputStream inputStream, Map values)`](simple-common-doc/src/main/java/com/simple/common/doc/common/service/DocReplaceService.java:113)：从输入流加载模板
- [`replaceResponse(String name, String templatePath, Map values)`](simple-common-doc/src/main/java/com/simple/common/doc/common/service/DocReplaceService.java:149)：从 resources 目录加载模板

便捷方法，适用于 Controller 层直接返回文档下载。自动设置响应头，浏览器会弹出下载对话框。文件名自动添加 `.docx` 后缀。

##### replaceAndGetOutputStream — 返回输出流

```java
ByteArrayOutputStream replaceAndGetOutputStream(String templatePath, Map<String, Object> values);
```

从 resources 加载模板，替换后返回 `ByteArrayOutputStream`。适用于需要获取完整字节数组的场景，如计算文件大小、加密、上传到 OSS 等。

##### replaceAndGetInputStream — 返回输入流

```java
ByteArrayInputStream replaceAndGetInputStream(String templatePath, Map<String, Object> values);
```

从 resources 加载模板，替换后返回 `ByteArrayInputStream`。适用于需要对生成的文档进行进一步处理的场景，如上传到 OSS、发送邮件附件等。

> **注意**：返回的流已关闭，调用方不应再次关闭。

### 5.6 DefaultDocReplaceService 默认实现

**类路径**：`com.simple.common.doc.service.DefaultDocReplaceService`

[`DefaultDocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/service/DefaultDocReplaceService.java:19) 是 [`DocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/common/service/DocReplaceService.java:55) 的默认实现，通过 `@Autowired` 注入 [`DocTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/common/manager/DocTemplateReplaceManager.java:46)，将 `replace` 方法委托给模板替换管理器：

```java
@Override
public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
    docTemplateReplaceManager.replace(inputStream, outputStream, values);
}
```

### 5.7 DocConfig 自动配置

**类路径**：`com.simple.common.doc.common.config.DocConfig`

[`DocConfig`](simple-common-doc/src/main/java/com/simple/common/doc/common/config/DocConfig.java:13) 是自动配置类，通过 `@ComponentScan(basePackages = {"com.simple.common.doc"})` 扫描模块组件。

通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动注册：

```
com.simple.common.doc.common.config.DocConfig
```

引入 `simple-common-doc` 依赖后，无需额外配置，[`DocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/common/service/DocReplaceService.java:55) 和 [`DocTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/common/manager/DocTemplateReplaceManager.java:46) 会自动注册为 Spring Bean。

## 6. 使用示例

### 6.1 完整示例：构建模板数据并下载

> 示例来源：[`DocController`](simple-common-test/src/main/java/com/simple/common/test/controller/DocController.java:29)

```java
@Slf4j
@RequestMapping("doc")
@Tag(name = "doc文档填充")
@RestController
public class DocController {

    @Autowired
    private DocReplaceService docService;

    @Operation(summary = "填充")
    @PostMapping("replace")
    public R<Object> replace() {
        // 构建模板数据
        Docs.DocBuilder builder = Docs.builder();
        builder.addStr("code", "普通文字填充");
        builder.addStr("num", "普通文字填充");
        builder.addStrColor("code1", "有颜色的文字填充", "ff0000");
        builder.addStrLink("code2", "有超链接的文字填充", "https://www.baidu.com");

        // 构建表格数据
        List<DocTestEntity> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            DocTestEntity doc = new DocTestEntity();
            doc.setName("张三" + i);
            doc.setSex("男" + i);
            doc.setAge(i);
            list.add(doc);
        }
        DocFunction<DocTestEntity> function = docTestEntity -> new String[]{
            docTestEntity.getName(),
            docTestEntity.getSex(),
            docTestEntity.getAge() + ""
        };
        builder.addTable("table", "90%", new String[]{"姓名", "性别", "年龄"}, list, function);

        // 从 resources 加载模板并下载
        docService.replaceResponse("测试文档", "doc/测试模板.docx", builder.create());
        return R.ok();
    }
}
```

### 6.2 Docs.builder() 构建模板数据

```java
Map<String, Object> data = Docs.builder()
    // 普通文本
    .addStr("contractNo", "HT-2024-001")
    // 带链接文本
    .addStrLink("companyName", "XX科技有限公司", "https://www.example.com")
    // 带颜色文本
    .addStrColor("warnText", "请仔细阅读", "ff0000")
    // 本地图片
    .addImgLocal("seal", "/templates/seal.png", 100, 100)
    // 图片流（数据库存储的图片）
    .addImgInputStream("sign", inputStream, 80, 80)
    // 网络图片
    .addImgInputUrl("logo", "https://cdn.example.com/logo.png", 120, 60)
    // 表格（默认字体15）
    .addTable("orderTable", "90%",
        new String[]{"序号", "商品", "数量", "单价", "金额"},
        orderItems,
        (item) -> new String[]{item.getSeq(), item.getName(), item.getQty(), item.getPrice(), item.getAmount()}
    )
    // 表格（指定字体大小12）
    .addTable("detailTable", "100%", 12,
        new String[]{"名称", "规格", "备注"},
        details,
        (d) -> new String[]{d.getName(), d.getSpec(), d.getRemark()}
    )
    // 列表（默认圆点）
    .addList("terms", Arrays.asList("条款一：xxx", "条款二：yyy", "条款三：zzz"))
    // 列表（指定编号格式）
    .addList("steps", NumberingFormat.DECIMAL, Arrays.asList("第一步", "第二步", "第三步"))
    .create();  // 返回 Map<String, Object>
```

### 6.3 DocReplaceService 替换并输出

```java
@Autowired
private DocReplaceService docReplaceService;

// ==== 最常用：从 resources 模板生成 + 浏览器下载 ====
@GetMapping("/contract/download/{orderId}")
public void downloadContract(@PathVariable String orderId) {
    Order order = orderService.findById(orderId);
    Map<String, Object> data = Docs.builder()
        .addStr("orderNo", order.getOrderNo())
        .addStr("customerName", order.getCustomerName())
        .addStr("amount", order.getAmount().toString())
        .create();

    // 一行完成：加载模板 → 替换 → 写入HTTP响应
    docReplaceService.replaceResponse("合同_" + order.getOrderNo(),
                                       "/templates/contract.docx", data);
}

// ==== 从流加载模板 + 浏览器下载 ====
try (InputStream templateStream = getClass().getResourceAsStream("/templates/contract.docx")) {
    docReplaceService.replaceResponse("合同_2024001", templateStream, data);
}

// ==== 获取输出流（上传OSS等场景） ====
ByteArrayOutputStream os = docReplaceService.replaceAndGetOutputStream("/templates/report.docx", data);
byte[] documentBytes = os.toByteArray();

// ==== 获取输入流（发送邮件附件等场景） ====
ByteArrayInputStream is = docReplaceService.replaceAndGetInputStream("/templates/report.docx", data);

// ==== 基础方法：指定输入输出流 ====
try (InputStream templateStream = new FileInputStream("template.docx");
     FileOutputStream outputStream = new FileOutputStream("output.docx")) {
    docReplaceService.replace(templateStream, outputStream, data);
}
```

## 7. 扩展点与自定义方式

### 7.1 自定义 DocTemplateReplaceManager 实现

如需使用其他模板引擎（如 FreeMarker、Aspose 等），实现 [`DocTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/common/manager/DocTemplateReplaceManager.java:46) 接口并注册为 Spring Bean，覆盖默认的 [`PoiTlTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/manager/PoiTlTemplateReplaceManager.java:22)：

```java
@Component
public class CustomDocTemplateManager implements DocTemplateReplaceManager {

    @Override
    public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
        // 使用自定义模板引擎进行替换
        CustomTemplateEngine engine = new CustomTemplateEngine();
        engine.compile(inputStream).render(values).writeTo(outputStream);
    }
}
```

### 7.2 自定义 DocReplaceService 实现

如需在替换前后添加业务逻辑（如数据校验、权限检查、日志记录等），实现 [`DocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/common/service/DocReplaceService.java:55) 接口：

```java
@Service
public class ContractDocService implements DocReplaceService {

    @Autowired
    private DocTemplateReplaceManager templateManager;

    @Override
    public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
        // 替换前：数据校验
        validateContractData(values);

        // 调用模板管理器进行替换
        templateManager.replace(inputStream, outputStream, values);

        // 替换后：记录日志
        log.info("合同文档已生成，合同号：{}", values.get("contractNo"));
    }

    private void validateContractData(Map<String, Object> values) {
        AssertUtils.notEmpty(values.get("contractNo"), "合同号不能为空");
        AssertUtils.notEmpty(values.get("partyA"), "甲方不能为空");
        AssertUtils.notEmpty(values.get("partyB"), "乙方不能为空");
    }
}
```

### 7.3 扩展 Docs Builder

如需支持更多 Poi-Tl 元素类型（如图表、附件等），可继承 [`Docs.DocBuilder`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:24) 或创建新的 Builder 类：

```java
public class ExtendedDocs {

    public static ExtendedDocBuilder builder() {
        return new ExtendedDocBuilder();
    }

    public static class ExtendedDocBuilder extends Docs.DocBuilder {

        /**
         * 添加图表
         * code格式：{{%code}}
         */
        public ExtendedDocBuilder addChart(String key, ChartRenderData chartData) {
            // 调用父类的 protected 方法或直接操作 templateData
            return this;
        }
    }
}
```

## 8. 注意事项

### 8.1 模板文件要求

- **文件格式**：模板必须为 `.docx` 格式（Office Open XML），不支持旧版 `.doc` 格式
- **占位符位置**：占位符 `{{code}}` 可以出现在文档的任意位置（正文、页眉、页脚、表格单元格等）
- **占位符名称**：占位符名称需与 [`Docs.builder()`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:20) 中传入的 `key` 完全一致（区分大小写）
- **模板路径**：模板文件放置在 `src/main/resources/` 目录下，`templatePath` 参数以 `/` 开头（如 `"/templates/contract.docx"`）

### 8.2 线程安全

- [`Docs.DocBuilder`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:24) 内部使用 `ConcurrentHashMap` 存储模板数据，线程安全，可在多线程环境中使用
- [`PoiTlTemplateReplaceManager`](simple-common-doc/src/main/java/com/simple/common/doc/manager/PoiTlTemplateReplaceManager.java:22) 每次调用创建新的 `XWPFTemplate` 实例，无共享状态，线程安全
- [`DefaultDocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/service/DefaultDocReplaceService.java:19) 通过 Spring 单例注入，无实例状态，线程安全

### 8.3 性能建议

- **模板缓存**：Poi-Tl 的 `XWPFTemplate.compile()` 每次都会解析模板，高频场景可考虑缓存编译后的模板
- **大文档处理**：生成大文档时注意内存占用，`replaceAndGetOutputStream` 会将完整文档加载到内存
- **图片处理**：网络图片通过 `addImgInputUrl` 会发起 HTTP 请求下载，注意网络超时设置

### 8.4 常见问题

1. **占位符未替换**：检查模板中的占位符名称与 [`Docs.builder()`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:20) 中的 `key` 是否完全一致（区分大小写），以及占位符格式是否正确（`{{}}`、`{{@}}`、`{{#}}`、`{{*}}`）。

2. **模板文件找不到**：`templatePath` 需以 `/` 开头，且文件确实存在于 `src/main/resources/` 目录下。[`FileUtils.getResourcesFileInputStream()`](simple-common-core/src/main/java/com/simple/common/core/utils/FileUtils.java:17) 从 classpath 加载资源。

3. **表格数据为空**：[`addTable`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:134) 方法当 `list` 为 `null` 时只渲染表头，不会报错。`list` 中的 `null` 元素会被自动过滤。

4. **列表数据为空**：[`addList`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:172) 方法当 `list` 为 `null` 或空时，会放入空的 `Numberings.create()`，不会报错。

5. **颜色值格式**：[`addStrColor`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java:58) 的 `color` 参数为十六进制颜色值，不包含 `#` 前缀（如 `"ff0000"` 表示红色）。

6. **下载文件名乱码**：[`ResponseUtils.writeResponse()`](simple-common-core/src/main/java/com/simple/common/core/utils/ResponseUtils.java:19) 已处理中文文件名编码，无需额外处理。
