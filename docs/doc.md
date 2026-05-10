# simple-common-doc

## 模块介绍

Word文档处理模块，提供模板替换、动态生成合同/证书/报表等功能。

## 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| **文档构建器** | `Docs` | **真正的入口，提供Builder模式构建文档数据** |
| 文档替换服务 | `DocReplaceService` | Word模板参数替换 |
| 模板管理器 | `DocTemplateReplaceManager` | 模板解析和替换引擎 |
| HTTP响应封装 | `replaceResponse` | 直接生成文档并下载 |
| 流转换工具 | `replaceAndGetInputStream` | 返回输入流便于进一步处理 |

## 集成方式

**步骤1：添加依赖**

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-doc</artifactId>
    <version>${version}</version>
</dependency>
```

**重要说明：**
- doc模块基于**poi-tl**引擎（官网：https://deepoove.com/poi-tl/）
- **推荐使用`Docs.builder()`作为入口，提供链式调用构建文档数据**
- 模板占位符格式根据类型不同：
  - `${var}` 或 `{{var}}` - 普通文本
  - `{{@var}}` - 图片
  - `{{#var}}` - 表格
  - `{{*var}}` - 列表
- poi-tl天然支持Word的所有特性，包括样式、颜色、表格、图表等

## 使用示例

### 1. 使用Docs构建器（推荐）

`Docs`提供Builder模式构建各种类型的文档数据：

```java
@Service
public class ContractService {
    
    @Autowired
    private DocReplaceService docReplaceService;
    
    /**
     * 生成合同文档
     */
    public void generateContract(String orderId, OutputStream outputStream) {
        // 查询订单数据
        Order order = orderService.findById(orderId);
        
        // 使用Docs构建器构建文档数据
        Map<String, Object> data = Docs.builder()
            // 添加普通文本
            .addStr("orderNo", order.getOrderNo())
            .addStr("customerName", order.getCustomerName())
            .addStr("amount", order.getAmount().toString())
            
            // 添加带颜色的文本
            .addStrColor("status", "已付款", "00AA00")  // 绿色
            
            // 添加带链接的文本
            .addStrLink("contractUrl", "查看合同详情", 
                "http://example.com/contract/" + orderId)
            
            // 添加本地图片（公司Logo）
            .addImgLocal("companyLogo", "/images/logo.png", 100, 50)
            
            // 添加网络图片
            .addImgInputUrl("qrCode", 
                "http://example.com/qr/" + orderId, 80, 80)
            
            // 添加表格
            .addTable("orderItems", "90%", 
                new String[]{"产品名称", "数量", "单价", "小计"},
                order.getItems(),
                item -> new String[]{
                    item.getProductName(),
                    item.getQuantity().toString(),
                    item.getPrice().toString(),
                    item.getSubtotal().toString()
                })
            
            // 添加列表
            .addList("terms", List.of(
                "本合同一式两份",
                "双方签字盖章后生效",
                "有效期一年"
            ))
            
            .create();  // 获取最终的Map
        
        // 加载模板并替换
        try (InputStream templateStream = getClass()
                .getResourceAsStream("/templates/contract.docx")) {
            docReplaceService.replace(templateStream, outputStream, data);
        }
    }
}
```

**Docs.Builder常用方法：**

| 方法 | 模板标记 | 说明 |
|------|---------|------|
| `addStr(key, value)` | `{{key}}` | 添加普通文本 |
| `addStrColor(key, value, color)` | `{{key}}` | 添加带颜色的文本 |
| `addStrLink(key, value, link)` | `{{key}}` | 添加带超链接的文本 |
| `addImgLocal(key, url, w, h)` | `{{@key}}` | 添加本地图片 |
| `addImgInputStream(key, stream, w, h)` | `{{@key}}` | 添加图片流 |
| `addImgInputUrl(key, url, w, h)` | `{{@key}}` | 添加网络图片 |
| `addTable(key, width, head, list, func)` | `{{#key}}` | 添加表格 |
| `addList(key, list)` | `{{*key}}` | 添加无序列表 |
| `addList(key, format, list)` | `{{*key}}` | 添加有序列表 |

### 2. 基础模板替换（不使用Docs）

如果只需要简单的文本替换，可以直接使用Map：

```java
@Service
public class SimpleContractService {
    
    @Autowired
    private DocReplaceService docReplaceService;
    
    public void generateSimpleContract(OutputStream outputStream) {
        // 直接使用Map（适合简单场景）
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", "ORD2024001");
        data.put("customerName", "张三");
        data.put("amount", "10000");
        
        try (InputStream templateStream = getClass()
                .getResourceAsStream("/templates/simple_contract.docx")) {
            docReplaceService.replace(templateStream, outputStream, data);
        }
    }
}
```

### 3. 直接下载文档（推荐）

```java
@RestController
@RequestMapping("/contract")
public class ContractController {
    
    @Autowired
    private DocReplaceService docReplaceService;
    
    /**
     * 下载合同文档
     */
    @GetMapping("/download/{orderId}")
    public void downloadContract(@PathVariable String orderId) {
        Order order = orderService.findById(orderId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("customerName", order.getCustomerName());
        data.put("amount", order.getAmount());
        
        // 一行代码完成：加载模板 → 替换数据 → 设置响应头 → 浏览器下载
        docReplaceService.replaceResponse(
            "合同_" + order.getOrderNo(),
            "/templates/contract.docx",
            data
        );
    }
}
```

### 4. 批量生成证书

```java
@Service
public class CertificateService {
    
    @Autowired
    private DocReplaceService docReplaceService;
    
    /**
     * 批量生成结业证书
     */
    public List<ByteArrayOutputStream> batchGenerateCertificates(List<User> users) {
        List<ByteArrayOutputStream> documents = new ArrayList<>();
        
        for (User user : users) {
            Map<String, Object> data = new HashMap<>();
            data.put("userName", user.getName());
            data.put("courseName", "Java高级编程");
            data.put("completeDate", LocalDate.now().format(
                DateTimeFormatter.ofPattern("yyyy年MM月dd日")
            ));
            
            // 生成证书文档
            ByteArrayOutputStream outputStream = docReplaceService.replaceAndGetOutputStream(
                "/templates/certificate.docx",
                data
            );
            
            documents.add(outputStream);
        }
        
        return documents;
    }
}
```

### 5. 上传到OSS

```java
@Service
public class ReportService {
    
    @Autowired
    private DocReplaceService docReplaceService;
    
    @Autowired
    private OssService ossService;
    
    /**
     * 生成月度报告并上传到OSS
     */
    public String generateAndUploadReport(String month) {
        // 准备报告数据
        Map<String, Object> data = buildReportData(month);
        
        // 生成文档并获取输入流
        ByteArrayInputStream inputStream = docReplaceService.replaceAndGetInputStream(
            "/templates/monthly_report.docx",
            data
        );
        
        // 上传到OSS
        String objectKey = "reports/" + month + ".docx";
        ossService.upload(objectKey, inputStream);
        
        return objectKey;
    }
}
```

### 6. 自定义文档服务

```java
@Service
public class CustomDocService implements DocReplaceService {
    
    @Autowired
    private DocTemplateReplaceManager templateManager;
    
    @Override
    public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
        // 添加业务逻辑：数据校验
        validateData(values);
        
        // 添加业务逻辑：权限检查
        checkPermission();
        
        // 调用模板管理器进行替换
        templateManager.replace(inputStream, outputStream, values);
    }
    
    private void validateData(Map<String, Object> values) {
        if (!values.containsKey("orderNo")) {
            throw new IllegalArgumentException("订单号不能为空");
        }
    }
    
    private void checkPermission() {
        // 权限校验逻辑
    }
}
```

## 高级功能

### 1. 动态表格

在Word模板中插入表格，第一行作为表头，使用 `{{var}}` 标记循环数据：

```java
// 准备表格数据
List<Map<String, Object>> orderItems = new ArrayList<>();
orderItems.add(Map.of("productName", "产品A", "quantity", 10, "price", 100));
orderItems.add(Map.of("productName", "产品B", "quantity", 5, "price", 200));

Map<String, Object> data = new HashMap<>();
data.put("orderItems", orderItems);  // 对应模板中的 {{orderItems}}

docReplaceService.replaceResponse("订单详情", "/templates/order.docx", data);
```

**模板设计：**

| 产品名称 | 数量 | 单价 |
|---------|------|------|
| {{orderItems.productName}} | {{orderItems.quantity}} | {{orderItems.price}} |

poi-tl会自动根据数据行数复制表格行。

### 2. 图片插入

```java
import com.deepoove.poi.data.Pictures;

// 从文件路径加载图片
PictureRenderData picture = Pictures.ofLocal("/path/to/logo.png")
    .size(100, 100)  // 设置尺寸
    .create();

Map<String, Object> data = new HashMap<>();
data.put("companyLogo", picture);  // 对应模板中的 ${companyLogo}

docReplaceService.replaceResponse("公司文档", "/templates/company.docx", data);
```

### 3. 富文本（带样式）

```java
import com.deepoove.poi.data.Texts;

// 创建带样式的文本
TextRenderData styledText = Texts.of("重要提示")
    .color("FF0000")  // 红色
    .bold()           // 加粗
    .fontSize(14)     // 字号
    .create();

Map<String, Object> data = new HashMap<>();
data.put("notice", styledText);  // 对应模板中的 ${notice}

docReplaceService.replaceResponse("通知文档", "/templates/notice.docx", data);
```

### 4. 条件显示

在模板中使用 `?var` 实现条件判断：

```
${?hasDiscount}折扣信息：${discountAmount}元${/hasDiscount}
```

```java
Map<String, Object> data = new HashMap<>();
data.put("hasDiscount", true);   // 控制是否显示
data.put("discountAmount", 50);

docReplaceService.replaceResponse("订单", "/templates/order.docx", data);
```

### 5. 嵌套对象

```java
// 嵌套对象
Map<String, Object> customer = new HashMap<>();
customer.put("name", "张三");
customer.put("phone", "13800138000");

Map<String, Object> address = new HashMap<>();
address.put("province", "广东省");
address.put("city", "深圳市");
customer.put("address", address);

Map<String, Object> data = new HashMap<>();
data.put("customer", customer);

// 模板中使用：${customer.name}、${customer.address.city}
docReplaceService.replaceResponse("客户信息", "/templates/customer.docx", data);
```

### 6. 循环列表（非表格）

```java
// 准备列表数据
List<String> tags = List.of("Java", "Spring Boot", "MyBatis");

Map<String, Object> data = new HashMap<>();
data.put("tags", tags);  // 对应模板中的 {{tags}}

// 模板中使用：{{tags}} 会生成多行文本
docReplaceService.replaceResponse("技能清单", "/templates/skills.docx", data);
```

## 适用场景
- 合同生成：根据订单数据动态生成合同文档
- 证书打印：批量生成培训证书、荣誉证书
- 报表导出：将业务数据填充到Word模板中
- 发票生成：自动生成电子发票

---

[返回主文档](../README.md)
