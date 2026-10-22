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
 * 默认实现 {@link com.simple.common.annex.service.S3AnnexService} 不做文件类型白名单与大小限制,
 * 输入流通过 readAllBytes 全量读入内存,调用方需自行保障文件类型与大小符合业务要求。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>用户头像上传：将头像文件存储到对象存储并生成访问URL</li>
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
 *             String objectUrl = s3Manager.upload(
 *                 file.getOriginalFilename(),
 *                 applicationName,
 *                 "images",
 *                 shareType,
 *                 inputStream
 *             );
 *
 *             // 4. 构建响应
 *             UploadResponse response = new UploadResponse();
 *             response.setSaveUrl(objectUrl);
 *             // 需要临时访问链接时，以完整对象存储 URL 为入参签发
 *             String tempUrl = s3Manager.generateUrl(objectUrl, 60);
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
     * @param applicationName 应用服务名称,用于组织文件目录结构,仅允许字母、数字、'-'、'_'、'.',如 "user-service"
     * @param shareType       文件访问权限类型,PUBLIC(公开) 或 PRIVATE(私有)
     * @return 上传响应结果,包含完整对象存储 URL（saveUrl）等信息;
     *         当 shareType 为 PRIVATE 时,saveUrl 仅作为后续 generateUrl/delete 的入参标识,
     *         不应作为可直接访问的链接暴露给终端用户
     * @throws RuntimeException 当文件为null或上传失败时抛出异常
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
     * // 对象键: avatars/jpg/{uuid}.jpg (packageName 是前缀目录,不会覆盖同包下其他文件)
     * }</pre>
     *
     * @param file            上传的文件对象,不能为null
     * @param applicationName 应用服务名称,仅允许字母、数字、'-'、'_'、'.',如 "user-service"
     * @param packageName     业务包名前缀,如 "avatars"、"documents"、"reports" 等;
     *                        仅允许字母、数字、'-'、'_'、'.';含 '..'、'/'、'\\' 等字符会被实现拒绝,
     *                        防止对象键越出预期前缀或与其他业务包冲突
     * @param shareType       文件访问权限类型,PUBLIC(公开) 或 PRIVATE(私有)
     * @return 上传响应结果,包含完整对象存储 URL（saveUrl）等信息;PRIVATE 时 saveUrl 仅作为内部标识,不应直接暴露
     * @throws RuntimeException 当文件为null或上传失败时抛出异常
     */
    UploadResponse upload(MultipartFile file, String applicationName, String packageName, ShareType shareType);

    /**
     * 附件上传(通过输入流)
     * <p>
     * 通过输入流上传文件,支持自定义文件对比逻辑。
     * 适用于需要从其他系统获取文件流后上传的场景。
     * </p>
     * <p>大小约束：默认实现会将整个流读入内存(readAllBytes)后计算指纹,
     * 调用方必须先限制文件大小并校验文件类型,否则存在内存溢出风险。</p>
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
     *         (uploadResponse) -> {
     *             // 返回 true 表示文件已存在、跳过上传(此时应把 uploadResponse 补全为最终结果);
     *             // 返回 false 表示继续上传;algorithmValue 可能为 null,必须判空
     *             return fileExists(uploadResponse.getAlgorithmValue());
     *         }
     *     );
     * }
     * }</pre>
     *
     * @param fileName        文件名(含扩展名),如 "avatar.jpg"
     * @param applicationName 应用服务名称,仅允许字母、数字、'-'、'_'、'.',如 "user-service"
     * @param packageName     业务包名前缀,如 "avatars",可为null;仅允许安全字符,非法字符将被拒绝
     * @param shareType       文件访问权限类型,PUBLIC(公开) 或 PRIVATE(私有)
     * @param inputStream     文件输入流,调用方负责关闭;默认实现会将其全量读入内存,调用方须先限制大小
     * @param uploadFunction  文件对比函数,用于判断是否需要重新上传,null表示总是上传
     * @return 上传响应结果,包含完整对象存储 URL（saveUrl）等信息;PRIVATE 时 saveUrl 仅作为内部标识
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
     * <p>安全契约：实现必须校验 objectUrl 的主机与桶名属于当前实例配置的存储端点及允许的桶,
     * 否则必须直接拒绝(防止跨桶读取/越权下载)。</p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @GetMapping("/download")
     * public void download(@RequestParam String objectUrl, HttpServletResponse response) {
     *     // 设置响应头
     *     response.setContentType("application/octet-stream");
     *     response.setHeader("Content-Disposition", "attachment; filename=\"file.pdf\"");
     *     // 下载文件
     *     annexService.writeGetObjectResponse(objectUrl);
     * }
     * }</pre>
     *
     * @param objectUrl 文件的完整对象存储 URL（形如 {服务地址}/{桶名}/{对象键}）,仅传对象键或文件标识将被拒绝
     * @throws RuntimeException 当入参不是完整对象存储 URL、不属于本服务配置的端点/桶、文件不存在或下载失败时抛出异常
     */
    void writeGetObjectResponse(String objectUrl);

    /**
     * 生成带有效期的文件访问URL
     * <p>
     * 为私有文件生成临时访问URL,有效期由配置决定。
     * 适用于需要临时分享私有文件的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 获取合同文件的临时访问链接
     * String url = annexService.generateUrl(contractObjectUrl);
     * // 返回: https://minio.example.com/bucket/contract.pdf?X-Amz-Algorithm=...
     * }</pre>
     *
     * @param objectUrl 文件的完整对象存储 URL（形如 {服务地址}/{桶名}/{对象键}）,仅传对象键或文件标识将被拒绝
     * @return 带签名的临时访问URL,过期后无法访问
     * @throws RuntimeException 当入参不是完整对象存储 URL、不属于本服务配置的端点/桶或生成URL失败时抛出异常
     */
    String generateUrl(String objectUrl);

    /**
     * 附件删除
     * <p>
     * 从存储服务中删除指定文件。
     * 删除操作不可恢复,请谨慎操作。
     * 建议在业务层做好软删除标记,定期清理已标记的文件。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 删除用户头像
     * annexService.delete("http://minio.example.com/user-service-public/avatars/avatar_123.jpg");
     * }</pre>
     *
     * @param objectUrl 文件的完整对象存储 URL（形如 {服务地址}/{桶名}/{对象键}）,仅传对象键或文件标识将被拒绝
     * @throws RuntimeException 当入参不是完整对象存储 URL、不属于本服务配置的端点/桶或删除失败时抛出异常
     */
    void delete(String objectUrl);

    /**
     * 附件上传
     *
     * @param fileName        文件名.格式
     * @param applicationName 系统服务名
     * @param packageName     包名
     * @param shareType       文件权限
     * @param inputStream     文件流(默认实现全量读入内存,调用方须先限制大小并负责关闭)
     * @return 上传响应结果,包含完整对象存储 URL（saveUrl）等信息
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
     * @param inputStream     文件流(默认实现全量读入内存,调用方须先限制大小并负责关闭)
     * @return 上传响应结果,包含完整对象存储 URL（saveUrl）等信息
     */
    default UploadResponse upload(String fileName, String applicationName, ShareType shareType, InputStream inputStream) {
        return upload(fileName, applicationName, null, shareType, inputStream, null);
    }

}
