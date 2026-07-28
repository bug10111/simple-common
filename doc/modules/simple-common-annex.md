# simple-common-annex 模块文档

> 模块: `simple-common-annex`
> 包路径: `com.simple.common.annex`
> 存储后端: S3 协议（支持 MinIO / 阿里云OSS / AWS S3）
> 作者: qty

---

## 1. 模块介绍

`simple-common-annex` 是 simple-common 框架中的 S3 协议文件管理模块，提供统一的文件上传、下载、删除、临时签名 URL 生成等功能。底层基于 S3 协议实现，同时兼容 MinIO、阿里云 OSS、AWS S3 三种存储服务。

### 核心能力

| 能力 | 说明 |
|------|------|
| 文件上传 | 支持 `MultipartFile` 上传和 `InputStream` 输入流上传，支持自定义包路径分类 |
| 文件下载 | 直接写入 HTTP 响应流（浏览器下载）或返回输入流（程序内处理） |
| 临时签名 URL | 为私有文件生成带有效期的签名访问 URL |
| 文件删除 | 按 objectKey 或完整 URL 删除存储文件 |
| 公开/私有权限 | 通过 `ShareType` 枚举控制文件桶的访问权限 |
| 文件去重 | 通过 `UploadFunction` 函数式接口支持上传前去重判断，配合 MD5 摘要算法 |

### 架构分层

```
┌─────────────────────────────────────────────────┐
│  AnnexService（业务服务接口）                     │  ← 业务层调用入口
│  └─ S3AnnexService（默认实现）                    │     封装 MD5 计算、去重判断
├─────────────────────────────────────────────────┤
│  S3Manager（S3 协议操作管理器接口）                │  ← 底层存储操作
│  └─ DefaultS3Handler（默认实现）                  │     同时持有 AmazonS3 / MinioClient / OSS
├─────────────────────────────────────────────────┤
│  存储后端                                         │
│  ├─ AmazonS3（AWS SDK，统一 S3 操作）             │
│  ├─ MinioClient（MinIO 原生，公共桶策略创建）      │
│  └─ OSS（阿里云原生，桶创建）                      │
└─────────────────────────────────────────────────┘
```

- `AnnexService` 是业务层调用的统一入口，默认实现 [`S3AnnexService`](simple-common-annex/src/main/java/com/simple/common/annex/service/S3AnnexService.java:29) 封装了 MD5 摘要计算和 `UploadFunction` 去重逻辑。
- `S3Manager` 是底层 S3 协议操作接口，默认实现 [`DefaultS3Handler`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:49) 同时初始化三个客户端（AmazonS3、MinioClient、OSS），根据 [`AnnexType`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/AnnexType.java:14) 配置选择不同的桶创建策略。

---

## 2. Maven 依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-annex</artifactId>
</dependency>
```

该模块已传递引入以下核心依赖，使用方无需重复声明：

| 依赖 | 说明 |
|------|------|
| `simple-common-core` | 框架核心（提供 `IdUtils`、`AlgorithmUtils`、`HttpServletUtils`、`R` 等） |
| `aws-java-sdk-s3` | AWS S3 SDK，统一的 S3 协议操作客户端 |
| `minio` | MinIO 原生 SDK，用于创建公共读桶策略（MinIO 不支持 S3 API 创建公共桶） |
| `aliyun-sdk-oss` | 阿里云 OSS 原生 SDK，用于桶创建 |
| `simple-common-mp` | MyBatis-Plus 封装（`provided` 作用域，编译时需要，运行时由使用方提供，提供 `@EnumValue` 注解支持） |

---

## 3. 核心功能表格

| 核心类 | 类型 | 功能说明 |
|--------|------|----------|
| [`AnnexService`](simple-common-annex/src/main/java/com/simple/common/annex/common/service/AnnexService.java:63) | 服务接口 | 附件业务服务接口，提供上传（MultipartFile/InputStream）、下载、生成签名URL、删除等方法 |
| [`S3AnnexService`](simple-common-annex/src/main/java/com/simple/common/annex/service/S3AnnexService.java:29) | 服务实现 | `AnnexService` 默认实现，封装 MD5 摘要计算和 `UploadFunction` 去重判断 |
| [`S3Manager`](simple-common-annex/src/main/java/com/simple/common/annex/common/manager/S3Manager.java:46) | 管理器接口 | S3 协议操作管理器接口，提供上传、下载（响应流/输入流）、删除、生成签名URL |
| [`DefaultS3Handler`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:49) | 管理器实现 | `S3Manager` 默认实现，同时持有 AmazonS3/MinioClient/OSS 三个客户端，实现 `InitializingBean` 在 Bean 初始化时创建客户端 |
| [`AnnexProperties`](simple-common-annex/src/main/java/com/simple/common/annex/common/properties/AnnexProperties.java:20) | 配置属性 | 附件模块配置类，前缀 `simple.annex`，配置存储类型、服务地址、密钥、有效期等 |
| [`AnnexConfig`](simple-common-annex/src/main/java/com/simple/common/annex/common/config/AnnexConfig.java:12) | 配置类 | Spring 自动装配配置类，`@ComponentScan` 扫描 `com.simple.common.annex` 包 |
| [`UploadResponse`](simple-common-annex/src/main/java/com/simple/common/annex/common/dto/UploadResponse.java:21) | DTO | 上传响应对象，包含文件名、大小、MD5摘要、存储URL、权限类型等 |
| [`UploadFunction`](simple-common-annex/src/main/java/com/simple/common/annex/common/function/UploadFunction.java:46) | 函数式接口 | 上传去重判断接口，返回 true 跳过上传，返回 false 继续上传 |
| [`ShareType`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/ShareType.java:15) | 枚举 | 文件访问权限类型，PUBLIC（公开）/ PRIVATE（私有） |
| [`AnnexType`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/AnnexType.java:14) | 枚举 | 文件存储类型，MINIO / OSS |
| [`Algorithm`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/Algorithm.java:9) | 枚举 | 摘要算法类型，目前仅支持 MD5 |

---

## 4. 配置说明

### 4.1 自动装配

模块通过 Spring Boot 自动装配机制注册，配置文件位于：

[`simple-common-annex/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`](simple-common-annex/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1)

```
com.simple.common.annex.common.config.AnnexConfig
```

[`AnnexConfig`](simple-common-annex/src/main/java/com/simple/common/annex/common/config/AnnexConfig.java:12) 通过 `@ComponentScan(basePackages = { "com.simple.common.annex" })` 扫描整个模块，引入 Maven 依赖后自动注册 `S3AnnexService`、`DefaultS3Handler`、`AnnexProperties` 等 Bean。

### 4.2 AnnexProperties 配置项

**文件**: [`AnnexProperties`](simple-common-annex/src/main/java/com/simple/common/annex/common/properties/AnnexProperties.java:20)

配置前缀：`simple.annex`

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | [`AnnexType`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/AnnexType.java:14) | `MINIO` | 文件存储类型，`MINIO` 或 `OSS` |
| `serverUrl` | `String` | 无 | 存储服务地址，如 `http://192.168.1.100:9000` |
| `expireTime` | `Integer` | `30` | 签名URL有效时长，单位：分钟 |
| `accessKey` | `String` | 无 | 存储服务访问密钥 |
| `accessSecret` | `String` | 无 | 存储服务访问密钥（Secret） |
| `algorithm` | [`Algorithm`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/Algorithm.java:9) | `MD5` | 摘要算法，用于判断文件是否重复上传 |
| `region` | `String` | `us-east-1` | 区域，MinIO/AWS 默认 `us-east-1`，阿里云默认 `cn-chengdu` |

### 4.3 application.yaml 配置示例

```yaml
simple:
  annex:
    type: MINIO                    # 存储类型: MINIO 或 OSS
    server-url: http://192.168.1.100:9000
    access-key: minioadmin
    access-secret: minioadmin
    expire-time: 30                # 签名URL有效期（分钟）
    algorithm: MD5                 # 摘要算法
    region: us-east-1              # 区域
```

---

## 5. 核心类与接口详细说明

### 5.1 AnnexService

**文件**: [`AnnexService`](simple-common-annex/src/main/java/com/simple/common/annex/common/service/AnnexService.java:63)

**职责**: 附件业务服务接口，是业务层调用文件管理的统一入口。默认实现 [`S3AnnexService`](simple-common-annex/src/main/java/com/simple/common/annex/service/S3AnnexService.java:29) 封装了 MD5 摘要计算和 `UploadFunction` 去重逻辑。

#### 方法清单

| 方法签名 | 说明 |
|---------|------|
| `UploadResponse upload(MultipartFile file, String applicationName, ShareType shareType)` | 上传 MultipartFile（基础版，使用默认包路径） |
| `UploadResponse upload(MultipartFile file, String applicationName, String packageName, ShareType shareType)` | 上传 MultipartFile（带包名，按业务模块分类） |
| `UploadResponse upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream, UploadFunction uploadFunction)` | 通过输入流上传（核心方法，支持去重判断） |
| `default UploadResponse upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream)` | 输入流上传简化版（不传 UploadFunction，默认总是上传） |
| `default UploadResponse upload(String fileName, String applicationName, ShareType shareType, InputStream inputStream)` | 输入流上传简化版（无包名、无 UploadFunction） |
| `void writeGetObjectResponse(String objectUrl)` | 下载文件并直接写入 HTTP 响应流（浏览器下载） |
| `String generateUrl(String objectUrl)` | 生成带有效期的签名访问 URL（有效期由配置 `expireTime` 决定） |
| `void delete(String objectUrl)` | 删除指定文件 |

#### S3AnnexService 实现细节

[`S3AnnexService`](simple-common-annex/src/main/java/com/simple/common/annex/service/S3AnnexService.java:29) 的核心上传逻辑（`upload` 输入流版本）：

1. **读取字节**：将输入流读取为字节数组，包装为 `ByteArrayInputStream`。
2. **组装响应**：构建 [`UploadResponse`](simple-common-annex/src/main/java/com/simple/common/annex/common/dto/UploadResponse.java:21)，设置文件名、大小、后缀、应用名、权限类型。
3. **MD5 摘要**：若配置 `algorithm` 为 `MD5`，计算文件 MD5 值并设置到响应中。
4. **去重判断**：若传入 `UploadFunction` 且返回 `true`，表示文件已存在，直接返回响应（跳过上传）。
5. **执行上传**：调用 [`S3Manager.upload()`](simple-common-annex/src/main/java/com/simple/common/annex/common/manager/S3Manager.java:71) 执行实际上传，将返回的完整路径设置到 `saveUrl`。

```java
// S3AnnexService 核心上传逻辑
var bytes = inputStream.readAllBytes();
UploadResponse uploadResponse = new UploadResponse()
    .setTotalSize(bytes.length)
    .setName(fileName)
    .setSuffix(FileUtil.getSuffix(fileName))
    .setShareType(shareType);

// MD5 摘要计算
if (annexProperties.getAlgorithm().equals(Algorithm.MD5)) {
    uploadResponse.setAlgorithmType(Algorithm.MD5);
    uploadResponse.setAlgorithmValue(AlgorithmUtils.md5Hex(bytes));
}

// 去重判断
if (uploadFunction != null && uploadFunction.handler(uploadResponse)) {
    return uploadResponse;  // 文件已存在，跳过上传
}

// 执行上传
var fullPath = s3Manager.upload(fileName, applicationName, packageName, shareType, in);
uploadResponse.setSaveUrl(fullPath);
```

### 5.2 S3Manager

**文件**: [`S3Manager`](simple-common-annex/src/main/java/com/simple/common/annex/common/manager/S3Manager.java:46)

**职责**: S3 协议操作管理器接口，提供底层存储操作。

#### 方法清单

| 方法签名 | 说明 |
|---------|------|
| `String upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream)` | 上传文件流，返回文件完整 URL 路径 |
| `void writeGetObjectResponse(String objectUrl)` | 下载文件并直接写入 HTTP 响应流 |
| `InputStream download(String objectUrl)` | 下载文件并返回输入流（调用方负责关闭） |
| `void delete(String objectUrl)` | 删除指定文件 |
| `String generateUrl(String objectUrl, int expireTime)` | 生成带有效期的签名访问 URL（expireTime 单位：分钟） |

> **注意**：`AnnexService` 接口未暴露 `download(InputStream)` 和 `generateUrl(url, expireTime)` 方法，这两个方法仅在 `S3Manager` 层可用。`AnnexService.generateUrl()` 使用配置的 `expireTime` 调用 `S3Manager.generateUrl()`。

### 5.3 DefaultS3Handler

**文件**: [`DefaultS3Handler`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:49)

**职责**: `S3Manager` 默认实现，同时持有三个存储客户端，根据 [`AnnexType`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/AnnexType.java:14) 配置选择不同的桶创建策略。实现 `InitializingBean` 接口，在 Bean 初始化时自动创建客户端。

#### 客户端初始化

[`afterPropertiesSet()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:224) 方法在 Bean 属性设置完成后执行，初始化三个客户端：

| 客户端 | 用途 | 初始化方式 |
|--------|------|-----------|
| `AmazonS3` | 统一 S3 操作（上传、下载、删除、签名URL） | `AmazonS3ClientBuilder` + `AWSStaticCredentialsProvider` + `EndpointConfiguration`，MinIO 模式启用 path-style 访问 |
| `MinioClient` | MinIO 原生操作（创建公共读桶策略） | `MinioClient.builder()` + endpoint + credentials |
| `OSS` | 阿里云原生操作（创建桶） | `OSSClientBuilder.build()` + endpoint + accessKey + accessSecret |

#### 桶命名与创建策略

**桶命名规则**（[`buildBucketName()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:159)）：

```java
// 桶名 = 应用名 + "-" + 权限类型，全小写
// 示例: "user-service" + ShareType.PUBLIC → "user-service-public"
//       "user-service" + ShareType.PRIVATE → "user-service-private"
return (applicationName + "-" + shareType).toLowerCase();
```

**桶创建策略**：

| 存储类型 | 创建方法 | 说明 |
|---------|---------|------|
| `MINIO` | [`createMinioBucket()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:193) | 使用 `MinioClient.makeBucket()` 创建桶，通过 `setBucketPolicy()` 设置 JSON 策略（PUBLIC 为公共读写，PRIVATE 为空策略）。使用 `synchronized` 保证线程安全 |
| `OSS` | [`createOssBucket()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:169) | 使用 `OSS.createBucket()` 创建桶，默认私有，存储类型为 Standard |

#### 文件 Key 生成规则

[`buildKey()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:217) 方法：

```java
// 文件后缀作为文件夹名 + UUID + 后缀
// 示例: "avatar.jpg" → "jpg/550e8400-e29b-41d4-a716-446655440000.jpg"
var suffix = FileUtil.getSuffix(fileName);
return suffix + "/" + IdUtils.getFastSimpleUUID() + "." + suffix;
```

- 若传入 `packageName` 不为空，则直接使用 `packageName` 作为 Key（不自动生成）。
- 若 `packageName` 为空，则按后缀名分目录 + UUID 生成唯一 Key。

#### 上传实现

[`uploadToS3()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:138) 方法使用 `AmazonS3.putObject()` 上传文件：

- 设置 `ObjectMetadata`：过期时间、内容长度、Content-Disposition（附件下载文件名 URL 编码）。
- 根据 `ShareType` 设置 ACL：`PRIVATE` → `CannedAccessControlList.Private`，`PUBLIC` → `CannedAccessControlList.PublicRead`。
- 返回完整 URL：`serverUrl + "/" + bucketName + "/" + key`。

#### 下载实现

| 方法 | 实现方式 |
|------|---------|
| [`writeGetObjectResponse()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:77) | 通过 `AmazonS3.getObject()` 获取文件，设置 `Content-Disposition` 响应头（文件名 URL 编码），写入 `HttpServletResponse` 输出流 |
| [`download()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:98) | 通过 `AmazonS3.getObject()` 获取文件，返回 `S3ObjectInputStream` 的 delegate stream |

#### URL 解析

下载、删除、生成签名 URL 方法均通过 [`UrlBuilder.ofHttp()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:78) 解析完整 URL，提取桶名和文件 Key：

```java
// URL 格式: http://serverUrl/bucketName/objectKey
// split("/", 3) → ["", bucketName, objectKey]
var split = pathStr.split("/", 3);
var bucketName = split[1];
var key = split[2];
```

### 5.4 UploadResponse

**文件**: [`UploadResponse`](simple-common-annex/src/main/java/com/simple/common/annex/common/dto/UploadResponse.java:21)

上传响应 DTO，使用 `@Accessors(chain = true)` 支持链式调用。

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 文件名 |
| `totalSize` | `long` | 文件总大小（字节） |
| `algorithmValue` | `String` | 摘要算法值（验证文件一致性） |
| `algorithmType` | [`Algorithm`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/Algorithm.java:9) | 摘要算法类型 |
| `suffix` | `String` | 文件扩展名（不带点） |
| `saveUrl` | `String` | 文件完整 URL（objectKey） |
| `shareType` | [`ShareType`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/ShareType.java:15) | 附件权限类型 |
| `applicationName` | `String` | 系统服务名称 |
| `isTrue` | `Boolean` | 有判断方法时返回，表示文件是否存在 |
| `extension` | `String` | 扩展字段 |

### 5.5 UploadFunction

**文件**: [`UploadFunction`](simple-common-annex/src/main/java/com/simple/common/annex/common/function/UploadFunction.java:46)

`@FunctionalInterface` 函数式接口，用于上传前去重判断。

```java
boolean handler(UploadResponse uploadResponse) throws Throwable;
```

- **返回 `true`**：文件已存在或无需上传，`uploadResponse` 中应包含完整的附件信息，跳过实际上传步骤。
- **返回 `false`**：需要继续执行上传流程。

### 5.6 枚举说明

#### ShareType

**文件**: [`ShareType`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/ShareType.java:15)

| 枚举值 | code | label | 说明 |
|--------|------|-------|------|
| `PUBLIC` | 1 | 公开 | 公开访问，无需签名 URL，桶策略为公共读 |
| `PRIVATE` | 2 | 私有 | 私有访问，需通过 `generateUrl()` 生成签名 URL 访问 |

> `code` 字段使用 `@EnumValue` 注解（MyBatis-Plus），支持数据库存储与枚举映射。

#### AnnexType

**文件**: [`AnnexType`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/AnnexType.java:14)

| 枚举值 | 说明 |
|--------|------|
| `MINIO` | MinIO 存储，桶创建使用 MinIO 原生客户端设置公共读策略 |
| `OSS` | 阿里云 OSS 存储，桶创建使用 OSS 原生客户端 |

#### Algorithm

**文件**: [`Algorithm`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/Algorithm.java:9)

| 枚举值 | 说明 |
|--------|------|
| `MD5` | MD5 摘要算法，用于计算文件一致性校验值 |

---

## 6. 使用示例

### 6.1 文件上传（MultipartFile）

```java
@Autowired
private AnnexService annexService;

@PostMapping("upload")
public R<UploadResponse> upload(MultipartFile file, ShareType shareType) {
    // 基础版上传（使用默认包路径）
    UploadResponse response = annexService.upload(file, "user-service", shareType);
    return R.ok(response);
}
```

带包名上传（按业务模块分类）：

```java
// 文件路径: user-service-public/avatars/jpg/uuid.jpg
UploadResponse response = annexService.upload(
    avatarFile,
    "user-service",    // applicationName
    "avatars",         // packageName
    ShareType.PUBLIC   // shareType
);
```

### 6.2 文件上传（InputStream + 去重）

```java
// 从网络下载图片后上传，带去重判断
URL url = new URL("https://example.com/image.jpg");
try (InputStream inputStream = url.openStream()) {
    UploadResponse response = annexService.upload(
        "image.jpg",           // fileName
        "user-service",        // applicationName
        "imports",             // packageName
        ShareType.PUBLIC,      // shareType
        inputStream,           // inputStream
        existingUrl -> existingUrl == null  // UploadFunction: null 时上传
    );
}
```

简化版输入流上传（不传 UploadFunction，总是上传）：

```java
UploadResponse response = annexService.upload(
    "report.pdf",
    "report-service",
    ShareType.PRIVATE,
    inputStream
);
```

### 6.3 文件下载（浏览器下载）

```java
@GetMapping("download")
public void download(String objectUrl, HttpServletResponse response) {
    annexService.writeGetObjectResponse(objectUrl);
}
```

> `writeGetObjectResponse()` 内部自动设置 `Content-Disposition` 响应头并写入文件流，无需手动设置。

### 6.4 生成临时签名 URL

```java
@GetMapping("preview")
public R<String> preview(String objectUrl) {
    // 生成带签名的临时访问URL，有效期由配置 simple.annex.expire-time 决定
    String url = annexService.generateUrl(objectUrl);
    return R.ok(url);
}
```

### 6.5 文件删除

```java
@DeleteMapping
public R<Object> delete(String objectUrl) {
    annexService.delete(objectUrl);
    return R.ok();
}
```

> 删除操作不可恢复，建议业务层先做软删除标记，定期清理已标记的文件。

### 6.6 完整 Controller 示例

参考 test 工程：[`AnnexController`](simple-common-test/src/main/java/com/simple/common/test/controller/AnnexController.java:23)

```java
@Slf4j
@RequestMapping("annex")
@Tag(name = "附件上传")
@RestController
public class AnnexController {

    @Autowired
    private AnnexService annexService;

    @Operation(summary = "文件上传")
    @PostMapping("upload")
    public R<UploadResponse> upload(MultipartFile filter, ShareType shareType) {
        UploadResponse simple = annexService.upload(filter, "simple-test", shareType);
        return R.ok(simple);
    }

    @Operation(summary = "获取文件")
    @GetMapping
    public R<Object> currentLimiting(String objectUrl) {
        return R.ok(annexService.generateUrl(objectUrl));
    }

    @Operation(summary = "下载文件")
    @GetMapping("writeGetObjectResponse")
    public R<Object> writeGetObjectResponse(String objectUrl) {
        annexService.writeGetObjectResponse(objectUrl);
        return R.ok();
    }

    @Operation(summary = "删除文件")
    @DeleteMapping
    public R<Object> delete(String objectUrl) {
        annexService.delete(objectUrl);
        return R.ok();
    }
}
```

---

## 7. 扩展点与自定义方式

### 7.1 自定义 AnnexService

实现 [`AnnexService`](simple-common-annex/src/main/java/com/simple/common/annex/common/service/AnnexService.java:63) 接口，添加文件大小校验、类型校验等业务逻辑：

```java
@Service
public class CustomAnnexService implements AnnexService {

    @Autowired
    private S3Manager s3Manager;

    @Override
    public UploadResponse upload(MultipartFile file, String applicationName, ShareType shareType) {
        // 校验文件大小
        AssertUtils.isTrue(file.getSize() <= 10 * 1024 * 1024, "文件大小不能超过10MB");

        // 校验文件类型
        String contentType = file.getContentType();
        AssertUtils.isTrue(isValidImageType(contentType), "不支持的文件类型");

        // 上传到 S3
        try (InputStream inputStream = file.getInputStream()) {
            return upload(file.getOriginalFilename(), applicationName, null, shareType, inputStream);
        }
    }

    // ... 其他方法
}
```

> 自定义实现需使用 `@Service` 注解并确保优先级高于默认的 `S3AnnexService`，或通过 `@Primary` 注解指定。

### 7.2 自定义 S3Manager

实现 [`S3Manager`](simple-common-annex/src/main/java/com/simple/common/annex/common/manager/S3Manager.java:46) 接口，替换底层存储操作逻辑：

```java
@Service
public class CustomS3Handler implements S3Manager {

    @Override
    public String upload(String fileName, String applicationName, String packageName,
                         ShareType shareType, InputStream inputStream) {
        // 自定义上传逻辑
        String objectKey = generateObjectKey(applicationName, packageName, fileName);
        // ... 执行上传
        return objectKey;
    }

    // ... 其他方法
}
```

### 7.3 自定义 UploadFunction 去重策略

通过 `UploadFunction` 实现文件去重，避免重复上传相同文件：

```java
// MD5 去重：查询数据库是否已存在相同 MD5 的文件
UploadResponse response = annexService.upload(
    fileName,
    "user-service",
    "documents",
    ShareType.PRIVATE,
    inputStream,
    uploadResponse -> {
        // 根据 MD5 查询数据库
        String md5 = uploadResponse.getAlgorithmValue();
        AnnexEntity existing = annexRepository.findByMd5(md5);
        if (existing != null) {
            // 文件已存在，设置已有记录的 URL
            uploadResponse.setSaveUrl(existing.getSaveUrl());
            uploadResponse.setIsTrue(true);
            return true;  // 跳过上传
        }
        return false;  // 继续上传
    }
);
```

### 7.4 扩展 DefaultS3Handler

继承 [`DefaultS3Handler`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:49)，覆盖 `buildKey()`、`buildBucketName()` 等方法自定义命名规则：

```java
@Service
public class CustomS3Handler extends DefaultS3Handler {

    @Override
    protected String buildKey(String fileName) {
        // 自定义 Key 生成规则：按日期分目录
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String suffix = FileUtil.getSuffix(fileName);
        return datePath + "/" + IdUtils.getFastSimpleUUID() + "." + suffix;
    }
}
```

---

## 8. 注意事项

1. **三客户端并存**：[`DefaultS3Handler`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:49) 在 `afterPropertiesSet()` 中同时初始化 `AmazonS3`、`MinioClient`、`OSS` 三个客户端。即使配置为 `OSS` 模式，`MinioClient` 也会被初始化（但不使用）。确保运行环境中三个 SDK 依赖均可用。

2. **桶名全局唯一**：阿里云 OSS 要求桶名全局唯一，[`createOssBucket()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:169) 每次调用都会执行 `createBucket`，若桶已存在会抛出异常。MinIO 模式下 [`createMinioBucket()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:193) 会先检查桶是否存在再创建。

3. **MinIO 公共桶策略**：MinIO 不支持通过 S3 API 创建公共读桶，[`createMinioBucket()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:193) 使用 `MinioClient.setBucketPolicy()` 设置 JSON 策略实现公共读写。该方法使用 `synchronized` 保证线程安全。

4. **URL 解析依赖完整 URL**：下载、删除、生成签名 URL 方法均通过 [`UrlBuilder.ofHttp()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:78) 解析完整 URL 提取桶名和 Key。传入的 `objectUrl` 必须是完整 URL（如 `http://192.168.1.100:9000/bucket/key`），不能仅传 objectKey。

5. **MD5 摘要默认开启**：[`AnnexProperties.algorithm`](simple-common-annex/src/main/java/com/simple/common/annex/common/properties/AnnexProperties.java:38) 默认为 `MD5`，[`S3AnnexService`](simple-common-annex/src/main/java/com/simple/common/annex/service/S3AnnexService.java:66) 会在上传时自动计算 MD5 值。大文件上传时会有内存开销（`readAllBytes()` 读取全部字节到内存）。

6. **输入流读取全部字节**：[`S3AnnexService.upload()`](simple-common-annex/src/main/java/com/simple/common/annex/service/S3AnnexService.java:57) 使用 `inputStream.readAllBytes()` 将整个文件读入内存，不适合超大文件上传。超大文件场景建议直接使用 [`S3Manager.upload()`](simple-common-annex/src/main/java/com/simple/common/annex/common/manager/S3Manager.java:71) 或自定义实现分片上传。

7. **simple-common-mp 为 provided 作用域**：[`pom.xml`](simple-common-annex/pom.xml:33) 中 `simple-common-mp` 依赖作用域为 `provided`，使用方需自行引入 `simple-common-mp`（或包含它的模块）以获得 `@EnumValue` 注解支持。若使用方不需要 MyBatis-Plus 的枚举映射功能，该注解不影响正常运行。

8. **删除前检查存在性**：[`DefaultS3Handler.delete()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:110) 会先通过 `amazonS3.doesObjectExist()` 检查文件是否存在，存在才执行删除，避免删除不存在的文件抛出异常。

9. **签名 URL 有效期**：[`generateUrl()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:122) 使用 `DateUtil.offset()` 计算过期时间（当前时间 + expireTime 分钟）。[`AnnexService.generateUrl()`](simple-common-annex/src/main/java/com/simple/common/annex/service/S3AnnexService.java:92) 使用配置的 `expireTime`，[`S3Manager.generateUrl()`](simple-common-annex/src/main/java/com/simple/common/annex/common/manager/S3Manager.java:153) 可自定义过期时间。

10. **上传文件名编码**：[`uploadToS3()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:147) 对 `Content-Disposition` 中的文件名进行 URL 编码，[`writeGetObjectResponse()`](simple-common-annex/src/main/java/com/simple/common/annex/manager/DefaultS3Handler.java:89) 使用 `filename*=utf-8''` 格式编码文件名，支持中文文件名下载。
