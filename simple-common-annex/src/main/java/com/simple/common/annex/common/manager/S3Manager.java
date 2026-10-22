package com.simple.common.annex.common.manager;

import com.simple.common.annex.common.enums.ShareType;

import java.io.InputStream;

/**
 * S3协议操作管理器接口。
 * <p>
 * 提供基于S3协议的文件上传、下载、删除等操作功能。
 * 支持MinIO、阿里云OSS、AWS S3等兼容S3协议的存储服务。
 * 默认实现 {@link com.simple.common.annex.manager.DefaultS3Handler} 基于 AWS SDK 实现。
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
 * public class CustomS3Handler implements S3Manager {
 *     @Autowired
 *     private S3Client s3Client;
 *     
 *     @Override
 *     public String upload(String fileName, String applicationName, String packageName, 
 *                         ShareType shareType, InputStream inputStream) {
 *         // 自定义上传逻辑
 *         String objectKey = buildObjectKey(applicationName, packageName, fileName);
 *         PutObjectRequest request = PutObjectRequest.builder()
 *             .bucket(bucketName)
 *             .key(objectKey)
 *             .build();
 *         // contentLength 必须是对象字节总数,不能使用 inputStream.available()(那只是阻塞可读字节数的估计值)
 *         s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
 *         // 返回完整对象存储 URL（{服务地址}/{桶名}/{对象键}）
 *         return buildObjectUrl(bucketName, objectKey);
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
     * 将文件流上传到S3存储服务,返回文件的完整对象存储 URL。
     * packageName 作为对象键业务前缀,键的 UUID 文件名部分自动生成以避免重名。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 上传图片
     * String objectUrl = s3Manager.upload("avatar.jpg", "user-service", null,
     *                                     ShareType.PUBLIC, inputStream);
     * // 返回: http://minio.example.com/user-service-public/jpg/{uuid}.jpg
     * }</pre>
     *
     * @param fileName        原始文件名,如 "avatar.jpg"
     * @param applicationName 应用服务名称,用于组织文件目录结构;仅允许字母、数字、'-'、'_'、'.',含 '/'、'\\'、'..' 等字符将被拒绝
     * @param packageName     业务包名前缀,如 "avatars"、"documents" 等;对象键形如 {packageName}/{后缀}/{uuid}.{后缀};
     *                        为空时直接使用 {后缀}/{uuid}.{后缀};同样仅允许安全字符
     * @param shareType       文件访问权限类型,PUBLIC(公开) 或 PRIVATE(私有)
     * @param inputStream     文件输入流,调用方负责关闭
     * @return 文件的完整对象存储 URL（形如 {服务地址}/{桶名}/{对象键}）,可作为后续下载、删除与临时链接签发的入参
     * @throws RuntimeException 入参非法(对象键片段含非法字符)或存储侧失败(对象/桶不存在、存储服务不可用等)时抛出异常,
     *                          两类失败均经由统一运行时异常,具体原因见异常消息
     */
    String upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream);

    /**
     * 附件下载(直接写入响应)
     * <p>
     * 从S3存储服务下载文件并直接写入HTTP响应流。
     * 适用于Controller层直接返回文件下载的场景。
     * </p>
     * <p>线程约束：本方法从当前线程的 Servlet 请求上下文获取响应对象,
     * 只能在处理 HTTP 请求的线程中调用;响应的提交与输出流关闭由实现负责,调用方无需再操作响应。</p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @GetMapping("/download")
     * public void download(@RequestParam String objectUrl) {
     *     s3Manager.writeGetObjectResponse(objectUrl);
     * }
     * }</pre>
     *
     * @param objectUrl 文件的完整对象存储 URL（形如 {服务地址}/{桶名}/{对象键}）,仅传对象键或文件标识将被拒绝
     * @throws RuntimeException 当入参不是完整对象存储 URL、不属于本实例配置端点、文件不存在或下载失败时抛出异常
     */
    void writeGetObjectResponse(String objectUrl);

    /**
     * 附件下载(返回输入流)
     * <p>
     * 从S3存储服务下载文件并返回输入流。
     * 适用于需要对文件内容进行处理后再返回的场景。
     * </p>
     * <p>流所有权：返回的流关闭时会同时释放底层 S3 HTTP 连接,
     * 调用方必须用 try-with-resources 关闭,否则连接池会被耗尽。</p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * try (InputStream inputStream = s3Manager.download(objectUrl)) {
     *     // 处理文件内容
     *     byte[] data = inputStream.readAllBytes();
     *     // 进行业务处理...
     * }
     * }</pre>
     *
     * @param objectUrl 文件的完整对象存储 URL（形如 {服务地址}/{桶名}/{对象键}）,仅传对象键或文件标识将被拒绝
     * @return 文件输入流,调用方必须手动关闭(关闭即释放底层连接)
     * @throws RuntimeException 当入参不是完整对象存储 URL、不属于本实例配置端点、文件不存在或下载失败时抛出异常
     */
    InputStream download(String objectUrl);

    /**
     * 附件删除
     * <p>
     * 从S3存储服务中删除指定文件。
     * 删除操作不可恢复,请谨慎操作。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 删除用户头像
     * s3Manager.delete("http://minio.example.com/user-service-public/avatars/avatar_123.jpg");
     * }</pre>
     *
     * @param objectUrl 文件的完整对象存储 URL（形如 {服务地址}/{桶名}/{对象键}）,仅传对象键或文件标识将被拒绝
     * @throws RuntimeException 当入参不是完整对象存储 URL、不属于本实例配置端点或删除失败时抛出异常
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
     * // 生成有效期为30分钟的访问链接(expireTime 单位为分钟)
     * String url = s3Manager.generateUrl(objectUrl, 30);
     * // 返回: https://minio.example.com/bucket/path/file.pdf?X-Amz-Algorithm=...&X-Amz-Expires=1800 (30分钟 = 1800秒)
     * }</pre>
     *
     * @param objectUrl  文件的完整对象存储 URL（形如 {服务地址}/{桶名}/{对象键}）,仅传对象键或文件标识将被拒绝
     * @param expireTime 过期时间,单位:分钟,建议设置为5-60分钟
     * @return 带签名的临时访问URL,过期后无法访问
     * @throws RuntimeException 当入参不是完整对象存储 URL、不属于本实例配置端点或生成URL失败时抛出异常
     */
    String generateUrl(String objectUrl, int expireTime);

}
