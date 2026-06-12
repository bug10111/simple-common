---
name: "simple-common-doc"
description: "Provides complete API documentation for simple-common-doc module (Word template replacement with Poi-Tl). Invoke when using Docs.builder() or DocReplaceService for Word document generation."
---

# simple-common-doc 认知文档

**Maven**: `simple-common-doc`
**包路径**: `com.simple.common.doc`
**基于**: Poi-Tl（Word .docx 模板填充）

## 模板语法

| 占位符格式 | 用途 | 示例 |
|-----------|------|------|
| `{{code}}` | 普通文本 | `{{username}}` |
| `{{@code}}` | 图片 | `{{@seal}}` |
| `{{#code}}` | 表格 | `{{#orderTable}}` |
| `{{*code}}` | 列表 | `{{*itemList}}` |

## Docs Builder — 构建模板数据

```java
Map<String, Object> data = Docs.builder()
    // 普通文本
    .addStr("contractNo", "HT-2024-001")
    // 带链接文本
    .addStrLink("companyName", "XX科技有限公司", "https://www.example.com")
    // 带颜色文本
    .addStrColor("warnText", "请仔细阅读", "#FF0000")
    // 本地图片
    .addImgLocal("seal", "/templates/seal.png", 100, 100)
    // 图片流（数据库存储的图片）
    .addImgInputStream("sign", inputStream, 80, 80)
    // 网络图片
    .addImgInputUrl("logo", "https://cdn.example.com/logo.png", 120, 60)
    // 表格
    .addTable("orderTable", "90%",
        new String[]{"序号", "商品", "数量", "单价", "金额"},
        orderItems,
        (item) -> new String[]{item.getSeq(), item.getName(), item.getQty(), item.getPrice(), item.getAmount()}
    )
    // 表格（指定字体大小）
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

**Docs Builder API**：

| 方法 | 模板格式 | 参数 |
|------|---------|------|
| `addStr(key, value)` | `{{key}}` | key=模板key, value=文本内容 |
| `addStrLink(key, value, link)` | `{{key}}` | 带超链接的文本 |
| `addStrColor(key, value, color)` | `{{key}}` | 带颜色的文本，color=如"#FF0000" |
| `addImgLocal(key, url, width, height)` | `{{@key}}` | 本地图片路径 |
| `addImgInputStream(key, is, width, height)` | `{{@key}}` | 图片输入流 |
| `addImgInputUrl(key, url, width, height)` | `{{@key}}` | 网络图片URL |
| `addTable(key, percentWidth, head, list, function)` | `{{#key}}` | 表格（默认字体15） |
| `addTable(key, percentWidth, size, head, list, function)` | `{{#key}}` | 表格（指定字体大小） |
| `addList(key, list)` | `{{*key}}` | 列表（默认圆点） |
| `addList(key, format, list)` | `{{*key}}` | 列表（指定格式） |

## DocReplaceService — 替换并输出

```java
@Autowired
private DocReplaceService docReplaceService;

// ==== 最常用：从resources模板生成 + 浏览器下载 ====
@GetMapping("/contract/download/{orderId}")
public void downloadContract(@PathVariable String orderId, HttpServletResponse response) {
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
ByteArrayInputStream is = docReplaceService.replaceAndGetInputStream("/templates/report.docx", data);

// ==== 基础方法：指定输入输出流 ====
docReplaceService.replace(inputStream, outputStream, data);
```

```java
void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values);
void replaceResponse(String name, InputStream inputStream, Map<String, Object> values);       // 流模板+下载
void replaceResponse(String name, String templatePath, Map<String, Object> values);           // resources模板+下载
ByteArrayInputStream replaceAndGetInputStream(String templatePath, Map<String, Object> values);
ByteArrayOutputStream replaceAndGetOutputStream(String templatePath, Map<String, Object> values);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `inputStream` | `InputStream` | 模板文件输入流 |
| `outputStream` | `OutputStream` | 替换后文档输出流 |
| `values` | `Map<String, Object>` | 模板参数（通过 `Docs.builder().create()` 构建） |
| `name` | `String` | 下载文件名（不含 `.docx` 扩展名） |
| `templatePath` | `String` | resources下模板路径，如 `"/templates/contract.docx"` |

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-doc</artifactId>
</dependency>
```