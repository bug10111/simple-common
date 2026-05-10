# simple-common-annex

## 模块介绍

附件管理模块，基于S3协议的对象存储封装，**支持通过配置在MinIO和阿里云OSS之间无缝切换**。使用统一的S3Manager接口，底层自动适配不同的存储服务。

**核心功能：**
- **存储服务切换**：通过配置项`simple.annex.type`在MINIO和OSS之间切换
- 文件上传（支持MultipartFile和InputStream）
- 文件下载（直接写入响应或返回输入流）
- 文件删除
- 文件校验（MD5摘要算法）
- 公开/私有访问控制
- 预签名URL生成（临时访问链接）
- 自动创建Bucket（根据应用名称和访问类型）

**架构特点：**
- **统一接口**：S3Manager接口屏蔽底层存储差异
- **自动初始化**：根据配置自动初始化AmazonS3、MinioClient、OSSClient
- **智能路由**：DefaultS3Handler根据AnnexType枚举自动选择对应的客户端
- **路径风格**：MinIO使用Path Style，OSS使用Virtual Hosted Style

## 集成方式

### 步骤1：添加依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-annex</artifactId>
    <version>${version}</version>
</dependency>
```

### 步骤2：配置存储服务（必须）

**方案一：使用MinIO（默认）**

```yaml
simple:
  annex:
    # ========== 存储服务类型 ==========
    # 可选值：MINIO（默认）、OSS
    # ⚠️ 重要：通过此配置在MinIO和阿里云OSS之间切换
    type: MINIO
    
    # ========== MinIO配置 ==========
    # MinIO服务端点
    server-url: http://localhost:9000
    # AccessKey
    access-key: minioadmin
    # SecretKey
    access-secret: minioadmin
    # 区域（MinIO默认us-east-1）
    region: us-east-1
    
    # ========== 通用配置 ==========
    # 预签名URL过期时间，单位分钟（默认30）
    expire-time: 30
    # 摘要算法类型（MD5或NONE，默认MD5）
    algorithm: MD5
```

**方案二：使用阿里云OSS**

```yaml
simple:
  annex:
    # ========== 存储服务类型 ==========
    # 切换到阿里云OSS
    type: OSS
    
    # ========== 阿里云OSS配置 ==========
    # OSS Endpoint（如：https://oss-cn-hangzhou.aliyuncs.com）
    server-url: https://oss-cn-hangzhou.aliyuncs.com
    # AccessKey ID
    access-key: your_access_key_id
    # AccessKey Secret
    access-secret: your_access_key_secret
    # 区域（如：cn-hangzhou）
    region: cn-hangzhou
    
    # ========== 通用配置 ==========
    expire-time: 30
    algorithm: MD5
```

**配置说明：**

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| type | AnnexType | 是 | MINIO | 存储服务类型：MINIO或OSS |
| server-url | String | 是 | - | 服务端点地址 |
| access-key | String | 是 | - | AccessKey ID |
| access-secret | String | 是 | - | AccessKey Secret |
| region | String | 否 | us-east-1 | 区域标识 |
| expire-time | Integer | 否 | 30 | 预签名URL过期时间（分钟） |
| algorithm | Algorithm | 否 | MD5 | 文件摘要算法 |

**⚠️ 重要注意事项：**

1. **type配置决定底层客户端**：
   - `type: MINIO` → 使用MinioClient + AmazonS3（Path Style）
   - `type: OSS` → 使用OSSClient + AmazonS3（Virtual Hosted Style）

2. **endpoint格式差异**：
   - MinIO：`http://localhost:9000`
   - OSS：`https://oss-cn-hangzhou.aliyuncs.com`

3. **region配置**：
   - MinIO：默认`us-east-1`
   - OSS：根据实际区域填写，如`cn-hangzhou`、`cn-beijing`

4. **Bucket命名规则**：
   - 自动生成：`{applicationName}-{shareType}`（小写）
   - 例如：`user-service-public`、`order-service-private`

5. **安全建议**：
   - 建议将`access-key`和`access-secret`配置在环境变量或配置中心
   - 不要硬编码在代码中

## 使用示例

### 1. 文件上传

```java
@RestController
@RequestMapping("/annex")
public class AnnexController {
    
    @Autowired
    private AnnexService annexService;
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public R<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        UploadResponse response = annexService.upload(
            file,
            "user-service",      // 应用名称（用于生成Bucket）
            "avatars",           // 包名（可选，用于组织文件目录）
            ShareType.PUBLIC     // 访问类型：PUBLIC或PRIVATE
        );
        return R.ok(response);
    }
}
```

**上传后的文件路径示例：**
- MinIO：`http://localhost:9000/user-service-public/avatars/jpg/uuid.jpg`
- OSS：`https://user-service-public.oss-cn-hangzhou.aliyuncs.com/avatars/jpg/uuid.jpg`

### 2. 获取文件访问URL

```java
/**
 * 获取预签名URL（适用于私有文件）
 */
@GetMapping("/url")
public R<String> getUrl(@RequestParam String objectUrl) {
    // 生成有效期为30分钟的临时访问链接
    String url = annexService.generateUrl(objectUrl);
    return R.ok(url);
}
```

**预签名URL示例：**
```
https://user-service-private.oss-cn-hangzhou.aliyuncs.com/avatars/jpg/uuid.jpg?
X-Amz-Algorithm=AWS4-HMAC-SHA256&
X-Amz-Credential=...&
X-Amz-Date=...&
X-Amz-Expires=1800&
X-Amz-SignedHeaders=...&
X-Amz-Signature=...
```

### 3. 文件下载（直接写入响应）

```java
/**
 * 下载文件（直接返回给浏览器）
 */
@GetMapping("/download")
public void download(@RequestParam String objectUrl, HttpServletResponse response) {
    // 框架自动设置Content-Disposition和文件名
    annexService.writeGetObjectResponse(objectUrl);
}
```

### 4. 文件下载（返回输入流）

```java
/**
 * 下载文件并处理（适用于需要二次处理的场景）
 */
@GetMapping("/download/stream")
public void downloadStream(@RequestParam String objectUrl) throws IOException {
    try (InputStream inputStream = ((S3Manager) annexService).download(objectUrl)) {
        // 读取文件内容
        byte[] data = inputStream.readAllBytes();
        
        // 进行业务处理，如：图片压缩、水印添加等
        // ...
    }
}
```

### 5. 删除文件

```java
/**
 * 删除文件
 */
@DeleteMapping("/delete")
public R<Void> delete(@RequestParam String objectUrl) {
    annexService.delete(objectUrl);
    return R.ok();
}
```

### 6. UploadResponse返回字段

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 原始文件名 |
| totalSize | long | 文件大小（字节） |
| algorithmValue | String | MD5摘要值（用于验证文件完整性） |
| algorithmType | Algorithm | 摘要算法类型 |
| suffix | String | 文件扩展名（不带点） |
| saveUrl | String | 文件完整URL |
| shareType | ShareType | 访问类型（PUBLIC/PRIVATE） |
| applicationName | String | 应用名称 |
| isTrue | Boolean | 文件是否已存在（配合UploadFunction使用） |

## 高级特性

### 1. 存储服务切换原理

框架在`DefaultS3Handler.afterPropertiesSet()`中根据配置初始化三个客户端：

```java
@Override
public void afterPropertiesSet() {
    // 1. 初始化AmazonS3（通用客户端）
    amazonS3 = AmazonS3ClientBuilder.standard()
        .withCredentials(credential)
        .withEndpointConfiguration(endpointConfiguration)
        .withPathStyleAccessEnabled(annexProperties.getType() == AnnexType.MINIO) // MinIO使用Path Style
        .build();

    // 2. 初始化MinioClient（仅MinIO需要，用于创建公共桶）
    minioClient = MinioClient.builder()
        .endpoint(annexProperties.getServerUrl())
        .credentials(annexProperties.getAccessKey(), annexProperties.getAccessSecret())
        .build();

    // 3. 初始化OSSClient（仅OSS需要）
    ossClient = new OSSClientBuilder().build(
        annexProperties.getServerUrl(), 
        annexProperties.getAccessKey(), 
        annexProperties.getAccessSecret()
    );
}
```

**上传时的自动路由：**

```java
@Override
public String upload(...) {
    var bucketName = buildBucketName(applicationName, shareType);
    
    // 根据type配置选择创建Bucket的方式
    if (annexProperties.getType() == AnnexType.MINIO) {
        createMinioBucket(bucketName, shareType);  // 使用MinioClient
    } else {
        createOssBucket(bucketName, shareType);    // 使用OSSClient
    }
    
    // 统一使用AmazonS3上传文件
    uploadToS3(bucketName, inputStream, key, fileName, acl);
    return annexProperties.getServerUrl() + "/" + bucketName + "/" + key;
}
```

### 2. Bucket自动管理

框架会根据应用名称和访问类型自动创建Bucket：

- **Bucket命名规则**：`{applicationName}-{shareType}`（小写）
- **自动创建**：首次上传时自动检查并创建Bucket
- **权限设置**：
  - PUBLIC：设置公共读策略
  - PRIVATE：保持私有

**MinIO公共桶策略：**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:ListBucket", "s3:GetBucketLocation"],
      "Resource": ["arn:aws:s3:::user-service-public"]
    },
    {
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::user-service-public/*"]
    }
  ]
}
```

### 3. 文件路径组织

上传的文件会按照以下规则组织路径：

```
{bucketName}/{packageName}/{suffix}/{uuid}.{suffix}
```

**示例：**
- 上传头像：`user-service-public/avatars/jpg/abc123.jpg`
- 上传文档：`order-service-private/documents/pdf/def456.pdf`

如果不指定`packageName`，则直接使用：`{bucketName}/{suffix}/{uuid}.{suffix}`

### 4. 文件去重机制

通过`UploadFunction`可以在上传前检查文件是否已存在：

```java
@Service
public class FileUploadService {
    
    @Autowired
    private AnnexService annexService;
    
    public UploadResponse uploadWithCheck(MultipartFile file, String md5) {
        return annexService.upload(
            file.getOriginalFilename(),
            "user-service",
            "avatars",
            ShareType.PUBLIC,
            file.getInputStream(),
            // 自定义校验逻辑
            (response) -> {
                // 检查数据库中是否已有相同MD5的文件
                boolean exists = fileRepository.existsByMd5(md5);
                if (exists) {
                    response.setIsTrue(true);  // 标记文件已存在
                    response.setExtension("跳过上传");
                    return true;  // 返回true表示跳过上传
                }
                return false;  // 返回false表示继续上传
            }
        );
    }
}
```

---

[返回主文档](../README.md)
