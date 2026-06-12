---
name: "simple-common-annex"
description: "Provides complete API documentation for simple-common-annex module (S3 file management). Invoke when uploading/downloading/deleting files or generating signed URLs with MinIO/OSS/S3."
---

# simple-common-annex 认知文档

**Maven**: `simple-common-annex`
**包路径**: `com.simple.common.annex`
**存储后端**: S3协议（支持 MinIO / 阿里云OSS / AWS S3）

## AnnexService API

```java
@Autowired
private AnnexService annexService;

// ==== 上传 ====

// 上传MultipartFile（基础版，使用默认包路径）
UploadResponse resp = annexService.upload(file, "user-service", ShareType.PUBLIC);

// 上传MultipartFile（带包名，按业务模块分类）
UploadResponse resp = annexService.upload(avatarFile, "user-service", "avatars", ShareType.PUBLIC);
// 文件路径: user-service/avatars/2024/01/15/avatar_xxx.jpg

// 通过输入流上传（从URL下载后上传等场景）
InputStream is = url.openStream();
UploadResponse resp = annexService.upload(
    "avatar.jpg",            // fileName: 含扩展名
    "user-service",          // applicationName: 应用名
    "avatars",               // packageName: 业务包名（可为null）
    ShareType.PUBLIC,        // shareType: PUBLIC公开/PRIVATE私有
    is,                      // inputStream: 文件流
    existingUrl -> existingUrl == null  // UploadFunction: 是否需要重新上传，null=总是上传
);

// 简化版输入流上传（不传UploadFunction，总是上传）
UploadResponse resp = annexService.upload("report.pdf", "report-service", ShareType.PRIVATE, inputStream);

// ==== 下载 ====

// 下载到浏览器（Controller中使用）
@GetMapping("/download/{objectKey}")
public void download(@PathVariable String objectKey, HttpServletResponse response) {
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition", "attachment; filename=\"file.pdf\"");
    annexService.writeGetObjectResponse(objectKey);
}

// ==== 生成临时访问URL ====
String url = annexService.generateUrl(objectKey);
// 返回带签名的临时URL，过期后无法访问（有效期由配置决定）

// ==== 删除 ====
annexService.delete(objectKey);
// 删除不可恢复，建议业务层先软删除再定时清理
```

## UploadResponse 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 文件名 |
| `totalSize` | `long` | 文件大小（字节） |
| `algorithmValue` | `String` | 摘要算法值（验证文件一致性） |
| `algorithmType` | `Algorithm` | 摘要算法类型 |
| `suffix` | `String` | 文件扩展名（不带点） |
| `saveUrl` | `String` | 文件完整URL（objectKey） |
| `shareType` | `ShareType` | 附件权限类型 |
| `applicationName` | `String` | 系统名称 |
| `isTrue` | `Boolean` | 有判断方法时返回，表示文件是否存在 |
| `extension` | `String` | 扩展字段 |

## ShareType 枚举

| 枚举 | code | 说明 |
|------|------|------|
| `PUBLIC` | 1 | 公开访问（无需签名） |
| `PRIVATE` | 2 | 私有访问（需签名URL） |

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-annex</artifactId>
</dependency>
```