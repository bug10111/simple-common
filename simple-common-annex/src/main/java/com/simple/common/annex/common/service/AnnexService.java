package com.simple.common.annex.common.service;

import com.simple.common.annex.common.dto.UploadResponse;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.annex.common.function.UploadFunction;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 附件服务接口。
 * <p>
 * 提供统一的文件上传、下载、删除等功能。
 * 基于S3协议实现,支持MinIO、阿里云OSS、AWS S3等存储服务。
 * 默认实现 {@link com.simple.common.annex.service.DefaultAnnexService} 封装了文件类型校验、大小限制等业务逻辑。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>用户头像上传：支持图片格式校验和压缩</li>
 *   <li>文档管理：合同、报告等文件的存储和管理</li>
 *   <li>商品图片：电商平台的商品图片上传和管理</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class CustomAnnexService implements AnnexService {
 *     @Autowired
 *     private S3Manager s3Manager;
 *     
 *     @Override
 *     public UploadResponse upload(MultipartFile file, String applicationName, ShareType shareType) {
 *         // 1. 校验文件大小
 *         AssertUtils.isTrue(file.getSize() <= 10 * 1024 * 1024, "文件大小不能超过10MB");
 *         
 *         // 2. 校验文件类型
 *         String contentType = file.getContentType();
 *         AssertUtils.isTrue(isValidImageType(contentType), "不支持的文件类型");
 *         
 *         // 3. 上传到S3
 *         try (InputStream inputStream = file.getInputStream()) {
 *             String objectKey = s3Manager.upload(
 *                 file.getOriginalFilename(), 
 *                 applicationName, 
 *                 "images", 
 *                 shareType, 
 *                 inputStream
 *             );
 *             
 *             // 4. 构建响应
 *             UploadResponse response = new UploadResponse();
 *             response.setObjectKey(objectKey);
 *             response.setUrl(s3Manager.generateUrl(objectKey, 60));
 *             return response;
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface AnnexService {

    /**
     * 附件上传(基础版)
     * <p>
     * 上传文件到存储服务,使用默认包路径。
     * 适用于不需要细分目录的简单场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @PostMapping("/upload")
     * public R<UploadResponse> upload(@RequestParam MultipartFile file) {
     *     UploadResponse response = annexService.upload(file, "user-service", ShareType.PUBLIC);
     *     return R.ok(response);
     * }
     * }</pre>
     *
     * @param file            上传的文件对象,不能为null
     * @param applicationName 应用服务名称,用于组织文件目录结构,如 "user-service"
     * @param shareType       文件访问权限类型,PUBLIC(公开) 或 PRIVATE(私有)
     * @return 上传响应结果,包含objectKey、访问URL等信息
     * @throws RuntimeException 当文件为空、格式不支持或上传失败时抛出异常
     */
    UploadResponse upload(MultipartFile file, String applicationName, ShareType shareType);

    /**
     * 附件上传(带包名)
     * <p>
     * 上传文件到存储服务,支持自定义包路径。
     * 适用于需要按业务模块细分文件存储的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 上传用户头像
     * UploadResponse response = annexService.upload(
     *     avatarFile, 
     *     "user-service", 
     *     "avatars",  // 包名: avatars
     *     ShareType.PUBLIC
     * );
     * // 文件路径: user-service/avatars/2024/01/15/avatar_xxx.jpg
     * }</pre>
     *
     * @param file            上传的文件对象,不能为null
     * @param applicationName 应用服务名称,如 "user-service"
     * @param packageName     业务包名,如 "avatars"、"documents"、"reports" 等
     * @param shareType       文件访问权限类型,PUBLIC(公开) 或 PRIVATE(私有)
     * @return 上传响应结果,包含objectKey、访问URL等信息
     * @throws RuntimeException 当文件为空、格式不支持或上传失败时抛出异常
     */
    UploadResponse upload(MultipartFile file, String applicationName, String packageName, ShareType shareType);

    /**
     * 附件上传(通过输入流)
     * <p>
     * 通过输入流上传文件,支持自定义文件对比逻辑。
     * 适用于需要从其他系统获取文件流后上传的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 从网络下载图片后上传到S3
     * URL url = new URL("https://example.com/image.jpg");
     * try (InputStream inputStream = url.openStream()) {
     *     UploadResponse response = annexService.upload(
     *         "image.jpg",
     *         "user-service",
     *         "imports",
     *         ShareType.PUBLIC,
     *         inputStream,
     *         (existingUrl) -> {
     *             // 判断是否需要重新上传
     *             return existingUrl == null || !fileExists(existingUrl);
     *         }
     *     );
     * }
     * }</pre>
     *
     * @param fileName        文件名(含扩展名),如 "avatar.jpg"
     * @param applicationName 应用服务名称,如 "user-service"
     * @param packageName     业务包名,如 "avatars",可为null
     * @param shareType       文件访问权限类型,PUBLIC(公开) 或 PRIVATE(私有)
     * @param inputStream     文件输入流,调用方负责关闭
     * @param uploadFunction  文件对比函数,用于判断是否需要重新上传,null表示总是上传
     * @return 上传响应结果,包含objectKey、访问URL等信息
     * @throws RuntimeException 当上传失败时抛出异常
     */
    UploadResponse upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream,
                          UploadFunction uploadFunction);

    /**
     * 附件下载(直接写入响应)
     * <p>
     * 从存储服务下载文件并直接写入HTTP响应流。
     * 适用于Controller层直接返回文件下载的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @GetMapping("/download/{objectKey}")
     * public void download(@PathVariable String objectKey, HttpServletResponse response) {
     *     // 设置响应头
     *     response.setContentType("application/octet-stream");
     *     response.setHeader("Content-Disposition", "attachment; filename=\"file.pdf\"");
     *     // 下载文件
     *     annexService.writeGetObjectResponse(objectKey);
     * }
     * }</pre>
     *
     * @param objectUrl 文件的对象Key或完整URL
     * @throws RuntimeException 当文件不存在或下载失败时抛出异常
     */
    void writeGetObjectResponse(String objectUrl);

    /**
     * 生成带有效期的文件访问URL
     * <p>
     * 为私有文件生成临时访问URL,默认有效期由配置决定。
     * 适用于需要临时分享私有文件的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 获取合同文件的临时访问链接
     * String url = annexService.generateUrl(contractObjectKey);
     * // 返回: https://minio.example.com/bucket/contract.pdf?X-Amz-Algorithm=...
     * }</pre>
     *
     * @param objectUrl 文件的对象Key或完整URL
     * @return 带签名的临时访问URL,过期后无法访问
     * @throws RuntimeException 当生成URL失败时抛出异常
     */
    String generateUrl(String objectUrl);

    /**
     * 附件删除
     * <p>
     * 从存储服务中删除指定文件。
     * 删除操作不可恢复,请谨慎使用。
     * 建议在业务层做好软删除标记,定期清理已标记的文件。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 删除用户头像
     * annexService.delete("user-service/avatars/avatar_123.jpg");
     * }</pre>
     *
     * @param objectUrl 文件的对象Key或完整URL
     * @throws RuntimeException 当文件不存在或删除失败时抛出异常
     */
    void delete(String objectUrl);

    /**
     * 附件上传
     *
     * @param fileName        文件名.格式
     * @param applicationName 系统服务名
     * @param packageName     包名
     * @param shareType       文件权限
     * @param inputStream     文件流
     */
    default UploadResponse upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream) {
        return upload(fileName, applicationName, packageName, shareType, inputStream, null);
    }

    /**
     * 附件上传
     *
     * @param fileName        文件名.格式
     * @param applicationName 系统服务名
     * @param shareType       文件权限
     * @param inputStream     文件流
     */
    default UploadResponse upload(String fileName, String applicationName, ShareType shareType, InputStream inputStream) {
        return upload(fileName, applicationName, null, shareType, inputStream, null);
    }

}
