# simple-common-excel

## 模块介绍

Excel导入导出模块，基于EasyExcel和Apache POI双引擎，支持大数据量导入导出、模板填充等功能。

## 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| **Excel读取服务** | `EasyExcelReadService` | **从文件/流/MultipartFile读取Excel，支持监听器逐行处理** |
| **Excel写入服务** | `EasyExcelWriteService` | **将数据写入输出流/HTTP响应，支持浏览器下载** |
| POI读取服务 | `PoiReadService` | 基于POI的Excel读取（适合复杂样式） |
| POI写入服务 | `PoiWriteService` | 基于POI的Excel写入（适合复杂样式） |

## 集成方式

**步骤1：添加依赖**

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-excel</artifactId>
    <version>${version}</version>
</dependency>
```

**重要说明：**
- excel模块已自动排除EasyExcel自带的旧版POI，统一使用项目中的POI版本
- 推荐使用EasyExcel引擎（内存占用低，适合大数据量）
- POI引擎适合需要复杂样式的场景
- EasyExcel读取采用监听器模式，逐行处理，不会OOM

## 使用示例

### 1. 导入Excel（从MultipartFile读取）

```java
@RestController
@RequestMapping("/excel")
public class ExcelController {
    
    @Autowired
    private EasyExcelReadService readService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 导入用户数据
     */
    @PostMapping("/import")
    public R<String> importUsers(@RequestParam("file") MultipartFile file) {
        List<UserDTO> userList = new ArrayList<>();
        
        // 使用监听器逐行处理
        readService.read(file, 1, UserDTO.class, new ReadListener<UserDTO>() {
            @Override
            public void invoke(UserDTO data, AnalysisContext context) {
                // 处理每一行数据
                userList.add(data);
            }
            
            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 所有数据解析完成后，批量保存
                userService.saveBatch(userList);
                log.info("导入成功，共{}条数据", userList.size());
            }
        });
        
        return R.ok("导入成功，共" + userList.size() + "条数据");
    }
}
```

### 2. 导出Excel（浏览器下载）

```java
@RestController
@RequestMapping("/excel")
public class ExcelController {
    
    @Autowired
    private EasyExcelWriteService writeService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 导出用户列表
     */
    @GetMapping("/export")
    public void exportUsers(HttpServletResponse response) {
        // 查询数据
        List<UserDTO> userList = userService.list();
        
        // 直接写入HTTP响应，触发浏览器下载
        writeService.writeResponse(UserDTO.class, userList, "用户列表");
    }
}
```

### 3. 导出Excel到输出流

```java
@Service
public class ReportService {
    
    @Autowired
    private EasyExcelWriteService writeService;
    
    /**
     * 生成报表并上传到OSS
     */
    public void generateReportAndUpload() {
        List<OrderDTO> orders = orderService.findAll();
        
        // 写入输出流
        ByteArrayOutputStream outputStream = writeService.writeOutputStream(
            OrderDTO.class, orders
        );
        
        // 上传到OSS
        ossClient.putObject("reports/orders.xlsx", outputStream.toByteArray());
    }
}
```

### 4. 从文件路径读取Excel

```java
@Service
public class DataImportService {
    
    @Autowired
    private EasyExcelReadService readService;
    
    /**
     * 从服务器本地文件导入
     */
    public void importFromLocalFile() {
        List<ProductDTO> productList = new ArrayList<>();
        
        readService.read("/data/import/products.xlsx", 1, ProductDTO.class, 
            new ReadListener<ProductDTO>() {
                @Override
                public void invoke(ProductDTO data, AnalysisContext context) {
                    productList.add(data);
                }
                
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    productService.saveBatch(productList);
                }
            });
    }
}
```

### 5. 实体类定义（使用@ExcelProperty注解）

```java
@Data
public class UserDTO {
    
    @ExcelProperty(value = "用户ID", index = 0)
    private String userId;
    
    @ExcelProperty(value = "用户名", index = 1)
    private String username;
    
    @ExcelProperty(value = "邮箱", index = 2)
    private String email;
    
    @ExcelProperty(value = "手机号", index = 3)
    private String phone;
    
    @ExcelProperty(value = "创建时间", index = 4)
    private LocalDateTime createTime;
}
```

## API参考

### EasyExcelReadService常用方法

| 方法 | 说明 |
|------|------|
| `read(filePath, headRowNumber, clazz, listener)` | 从文件路径读取 |
| `read(inputStream, headRowNumber, clazz, listener)` | 从输入流读取 |
| `read(multipartFile, headRowNumber, clazz, listener)` | 从MultipartFile读取（便捷方法） |

### EasyExcelWriteService常用方法

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `writeResponse(clazz, data, writeName)` | 写入HTTP响应（浏览器下载） | void |
| `writeOutputStream(clazz, data)` | 写入输出流 | ByteArrayOutputStream |
| `writeInputStream(clazz, data)` | 写入输入流 | ByteArrayInputStream |

---

[返回主文档](../README.md)
