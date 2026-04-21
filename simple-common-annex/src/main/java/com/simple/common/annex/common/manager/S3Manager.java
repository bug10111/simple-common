package com.simple.common.annex.common.manager;

import com.simple.common.annex.common.enums.ShareType;

import java.io.InputStream;

/**
 * S3协议操作管理器接口。
 * <p>
 * 提供基于S3协议的文件上传、下载、删除等操作功能。
 * 支持MinIO、阿里云OSS、AWS S3等兼容S3协议的存储服务。
 * 默认实现 {@link com.simple.common.annex.manager.DefaultS3Manager} 基于 AWS SDK 实现。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>图片/文档上传：用户头像、商品图片、合同文档等</li>
 *   <li>文件下载：报表导出、附件下载等</li>
 *   <li>临时链接生成：生成带有效期的文件访问URL</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class CustomS3Manager implements S3Manager {
 *     @Autowired
 *     private S3Client s3Client;
 *     
 *     @Override
 *     public String upload(String fileName, String applicationName, String packageName, 
 *                         ShareType shareType, InputStream inputStream) {
 *         // 自定义上传逻辑
 *         String objectKey = generateObjectKey(applicationName, packageName, fileName);
 *         PutObjectRequest request = PutObjectRequest.builder()
 *             .bucket(bucketName)
 *             .key(objectKey)
 *             .build();
 *         s3Client.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available()));
 *         return objectKey;
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface S3Manager {

    /**
     * 附件上传
     * <p>
     * 将文件流上传到S3存储服务,返回文件的对象Key(路径)。
     * 文件名会自动添加时间戳和UUID以避免重名。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 上传图片
     * String objectKey = s3Manager.upload("avatar.jpg", "user-service", "avatars", 
     *                                     ShareType.PUBLIC, inputStream);
     * // 返回: user-service/avatars/2024/01/15/avatar_1705312345678_uuid.jpg
     * }</pre>
     *
     * @param fileName        原始文件名,如 "avatar.jpg"
     * @param applicationName 应用服务名称,用于组织文件目录结构
     * @param packageName     业务包名,如 "avatars"、"documents" 等
     * @param shareType       文件访问权限类型,PUBLIC(公开) 或 PRIVATE(私有)
     * @param inputStream     文件输入流,调用方负责关闭
     * @return 文件的对象Key(完整路径),可用于后续下载或删除操作
     * @throws RuntimeException 当上传失败时抛出异常
     */
    String upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream);

    /**
     * 附件下载(直接写入响应)
     * <p>
     * 从S3存储服务下载文件并直接写入HTTP响应流。
     * 适用于Controller层直接返回文件下载的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @GetMapping("/download/{objectKey}")
     * public void download(@PathVariable String objectKey, HttpServletResponse response) {
     *     s3Manager.writeGetObjectResponse(objectKey);
     * }
     * }</pre>
     *
     * @param objectUrl 文件的对象Key或完整URL
     * @throws RuntimeException 当文件不存在或下载失败时抛出异常
     */
    void writeGetObjectResponse(String objectUrl);

    /**
     * 附件下载(返回输入流)
     * <p>
     * 从S3存储服务下载文件并返回输入流。
     * 调用方需要自行处理流的读取和关闭。
     * 适用于需要对文件内容进行处理后再返回的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * try (InputStream inputStream = s3Manager.download(objectKey)) {
     *     // 处理文件内容
     *     byte[] data = inputStream.readAllBytes();
     *     // 进行业务处理...
     * }
     * }</pre>
     *
     * @param objectUrl 文件的对象Key或完整URL
     * @return 文件输入流,调用方必须手动关闭
     * @throws RuntimeException 当文件不存在或下载失败时抛出异常
     */
    InputStream download(String objectUrl);

    /**
     * 附件删除
     * <p>
     * 从S3存储服务中删除指定文件。
     * 删除操作不可恢复,请谨慎使用。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 删除用户头像
     * s3Manager.delete("user-service/avatars/avatar_123.jpg");
     * }</pre>
     *
     * @param objectUrl 文件的对象Key或完整URL
     * @throws RuntimeException 当删除失败时抛出异常
     */
    void delete(String objectUrl);

    /**
     * 生成带有效期的文件访问URL
     * <p>
     * 为私有文件生成临时访问URL,URL在指定时间后过期。
     * 适用于需要临时分享私有文件的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 生成有效期为30分钟的访问链接
     * String url = s3Manager.generateUrl(objectKey, 30);
     * // 返回: https://minio.example.com/bucket/objectKey?X-Amz-Algorithm=...&X-Amz-Expires=1800
     * }</pre>
     *
     * @param objectUrl  文件的对象Key或完整URL
     * @param expireTime 过期时间,单位:分钟,建议设置为5-60分钟
     * @return 带签名的临时访问URL,过期后无法访问
     * @throws RuntimeException 当生成URL失败时抛出异常
     */
    String generateUrl(String objectUrl, int expireTime);

}
