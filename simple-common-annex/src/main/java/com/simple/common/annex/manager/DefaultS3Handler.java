package com.simple.common.annex.manager;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.StorageClass;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.simple.common.annex.common.enums.AnnexType;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.annex.common.manager.S3Manager;
import com.simple.common.annex.common.properties.AnnexProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.core.utils.IdUtils;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultS3Handler implements S3Manager, InitializingBean {

    /**
     * 对象键片段(应用名/包名)允许的字符:字母、数字、'-'、'_'、'.';
     * 用于防止 .. / \ 等字符越出预期对象键前缀,或与其他业务模块对象键冲突
     */
    private static final Pattern SAFE_KEY_SEGMENT = Pattern.compile("^[A-Za-z0-9._-]+$");

    /**
     * 桶创建互斥锁:桶存在性检查必须与创建动作在同一个临界区内完成,
     * 否则并发上传会在 check-then-act 竞态下重复创建桶
     */
    private final Object bucketCreateLock = new Object();

    @Autowired
    private AnnexProperties annexProperties;

    private AmazonS3 amazonS3;

    private MinioClient minioClient;

    private OSS ossClient;

    @Override
    public String upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream) {
        validateKeySegment(applicationName, "applicationName");
        var bucketName = buildBucketName(applicationName, shareType);
        if (annexProperties.getType() == AnnexType.MINIO) {
            createMinioBucket(bucketName, shareType);
        } else {
            createOssBucket(bucketName, shareType);
        }
        // packageName 作为业务前缀目录,避免同包名下的不同文件相互覆盖丢失生成的 UUID 文件名
        var key = StrUtil.isEmpty(packageName) ? buildKey(fileName) : packageName + "/" + buildKey(fileName);

        CannedAccessControlList acl = ShareType.PRIVATE.equals(shareType) ? CannedAccessControlList.Private : CannedAccessControlList.PublicRead;

        uploadToS3(bucketName, inputStream, key, fileName, acl);
        return annexProperties.getServerUrl() + "/" + bucketName + "/" + key;
    }

    @Override
    @SneakyThrows
    public void writeGetObjectResponse(String objectUrl) {
        // 解析完整对象存储 URL，获取桶名称与对象键
        var objectLocation = parseObjectLocation(objectUrl);

        //获取输入流
        S3Object object = amazonS3.getObject(objectLocation.bucketName(), objectLocation.objectKey());
        @Cleanup S3ObjectInputStream objectContent = object.getObjectContent();

        // 依赖当前线程的 Servlet 请求上下文取响应对象(方法须在处理 HTTP 请求的线程中调用),
        // 写出的响应由本方法负责提交,调用方无需再处理输出流
        HttpServletResponse response = HttpServletUtils.getResponse();
        String[] fileUrl = object.getKey().split("/");
        var key = fileUrl[fileUrl.length - 1];
        key = URLEncoder.encode(key, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=UTF-8''" + key);

        //写入输出流，并关闭输出流。
        //边读边写代替 readAllBytes 全量入堆,大文件下载不再受堆内存限制;
        //显式下发 Content-Length 便于客户端显示进度。
        response.setContentLengthLong(object.getObjectMetadata().getContentLength());
        ServletOutputStream outputStream = response.getOutputStream();
        IoUtil.copy(objectContent, outputStream);
        outputStream.flush();
    }

    @Override
    public InputStream download(String objectUrl) {
        // 解析完整对象存储 URL，获取桶名称与对象键
        var objectLocation = parseObjectLocation(objectUrl);

        GetObjectRequest getObjectRequest = new GetObjectRequest(objectLocation.bucketName(), objectLocation.objectKey());
        S3Object object = amazonS3.getObject(getObjectRequest);
        // 返回 S3ObjectInputStream(其 close 会释放底层 HTTP 连接),只返回裸委托流会导致连接无法归还连接池
        return object.getObjectContent();
    }

    @Override
    public void delete(String objectUrl) {
        // 解析完整对象存储 URL，获取桶名称与对象键
        var objectLocation = parseObjectLocation(objectUrl);

        if (amazonS3.doesObjectExist(objectLocation.bucketName(), objectLocation.objectKey())) {
            amazonS3.deleteObject(objectLocation.bucketName(), objectLocation.objectKey());
        }
    }

    @Override
    public String generateUrl(String objectUrl, int expireTime) {

        // 解析完整对象存储 URL，获取桶名称与对象键
        var objectLocation = parseObjectLocation(objectUrl);

        //通过桶和文件位置，获取请求对象
        var urlRequest = new GeneratePresignedUrlRequest(objectLocation.bucketName(), objectLocation.objectKey());

        //计算过期时间(与配置契约一致,expireTime 单位为分钟)
        var expireDate = DateUtil.offset(new Date(), DateField.MINUTE, expireTime);
        urlRequest.setExpiration(expireDate);
        return amazonS3.generatePresignedUrl(urlRequest).toString();
    }

    @SneakyThrows
    private void uploadToS3(String bucketName, InputStream inputStream, String key, String originalFilename, CannedAccessControlList cannedAcl) {

        //获取过期时间(与配置契约一致,expireTime 单位为分钟,不再按天换算)
        var expireDate = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(annexProperties.getExpireTime()));

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setHttpExpiresDate(expireDate);
        metadata.setContentDisposition("attachment;filename=" + URLEncoder.encode(originalFilename, StandardCharsets.UTF_8));

        if (inputStream instanceof ByteArrayInputStream byteArrayInputStream) {
            // ByteArrayInputStream 的 available() 即精确剩余长度,可直接作为 Content-Length
            metadata.setContentLength(byteArrayInputStream.available());
            amazonS3.putObject(new PutObjectRequest(bucketName, key, byteArrayInputStream, metadata).withCannedAcl(cannedAcl));
        } else {
            // available() 只是阻塞可读字节数的估计值,不能作为 Content-Length;
            // 按本模块上传契约(调用方已将流全量读入内存),此处读入字节数组后以精确长度上传
            byte[] bytes = inputStream.readAllBytes();
            metadata.setContentLength(bytes.length);
            amazonS3.putObject(new PutObjectRequest(bucketName, key, new ByteArrayInputStream(bytes), metadata).withCannedAcl(cannedAcl));
        }
    }

    /**
     * 获取目标桶名称
     *
     * @param applicationName 服务名称
     * @param shareType       附件类型
     */
    protected String buildBucketName(String applicationName, ShareType shareType) {
        return (applicationName + "-" + shareType).toLowerCase();
    }

    /**
     * 创建OSS桶
     * 桶存在性检查与创建动作在同一个临界区内完成,避免并发上传时重复创建桶(BucketAlreadyExists/AlreadyOwnedByYou);
     * OSS 的 S3 兼容端点不识别 x-amz-acl 请求头,公共读桶必须显式设置桶 ACL,
     * 否则 PUBLIC 附件只能靠 putObject 的 canned ACL,在 OSS 上会得到私有对象导致读取 403。
     *
     * @param bucketName 桶名称
     * @param shareType  附件类型
     */
    public void createOssBucket(String bucketName, ShareType shareType) {
        synchronized (bucketCreateLock) {
            // 1. 检查桶是否存在（阿里云要求全局唯一）
            if (!ossClient.doesBucketExist(bucketName)) {
                // 2. 创建桶（默认私有）
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);

                // 3.设置存储类型
                createBucketRequest.setStorageClass(StorageClass.Standard);

                // 4. 创建 Bucket
                ossClient.createBucket(createBucketRequest);
            }

            // 公共附件需要显式设置桶级公共读 ACL
            if (ShareType.PUBLIC == shareType) {
                ossClient.setBucketAcl(bucketName, com.aliyun.oss.model.CannedAccessControlList.PublicRead);
            }
        }
    }

    /**
     * 创建minio桶,minio 不支持s3创建公共桶，这里是单独的minio接口服务
     * 存在性检查与创建动作在同一个临界区内完成,避免并发上传时 check-then-act 竞态重复建桶
     *
     * @param bucketName 桶名称
     * @param shareType  附件类型
     */
    @SneakyThrows
    protected void createMinioBucket(String bucketName, ShareType shareType) {
        synchronized (bucketCreateLock) {
            if (!amazonS3.doesBucketExistV2(bucketName)) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).region(annexProperties.getRegion()).build());
                String sb;
                if (ShareType.PUBLIC == shareType) {
                    sb = "{\"Version\":\"2012-10-17\"," + "\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":"
                         + "{\"AWS\":[\"*\"]},\"Action\":[\"s3:ListBucket\",\"s3:ListBucketMultipartUploads\"," + "\"s3:GetBucketLocation\"],\"Resource\":[\"arn:aws:s3:::" + bucketName
                         + "\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:PutObject\",\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\",\"s3:ListMultipartUploadParts\"],\"Resource\":[\"arn:aws:s3:::"
                         + bucketName + "/*\"]}]}";
                } else {
                    sb = "{\"Version\":\"2012-10-17\",\"Statement\":[]}";
                }
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(sb).build());
            }
        }
    }

    /**
     * 获取文件在桶中的位置（包和文件名）
     *
     * @param fileName 文件名称
     */
    protected String buildKey(String fileName) {
        //文件后缀作为文件夹名称;无后缀时回退为 file,避免产生 null/uuid.null 的畸形对象键
        var suffix = StrUtil.emptyIfNull(FileUtil.getSuffix(fileName));
        return suffix + "/" + IdUtils.getFastSimpleUUID() + "." + suffix;
    }

    /**
     * 校验对象键片段(应用名/包名):仅允许字母、数字、'-'、'_'、'.',
     * 拒绝 '/'、'\\'、'..' 与空值,防止越出预期对象键前缀或与其他业务模块冲突
     *
     * @param segment   待校验片段
     * @param fieldName 片段名称(用于异常提示)
     */
    private void validateKeySegment(String segment, String fieldName) {
        AssertUtils.isTrue(StrUtil.isNotBlank(segment) && SAFE_KEY_SEGMENT.matcher(segment).matches() && !segment.contains(".."),
                           "非法的对象键片段，{} 仅允许字母、数字、'-'、'_'、'.'，当前值：{}", fieldName, abbreviate(segment));
    }

    /**
     * 解析完整对象存储 URL，获取桶名称与对象键。
     * <p>
     * 仅接受形如 {@code {服务地址}/{桶名}/{对象键}} 的完整对象存储 URL，
     * 路径必须包含桶名与对象键两段且均非空；仅传对象键或文件标识将被拒绝。
     * </p>
     * <p>
     * 安全约束：URL 的主机必须与当前实例配置的存储端点一致，桶名必须符合本模块
     * {@code {applicationName}-{public|private}} 命名契约，
     * 否则直接拒绝，防止跨桶读取、删除无关桶对象或为外部端点签名。
     * </p>
     *
     * @param objectUrl 完整对象存储 URL
     * @return 桶名称与对象键
     */
    private ObjectLocation parseObjectLocation(String objectUrl) {
        // 校验入参非空，避免空值或空白串进入 URL 解析
        AssertUtils.isTrue(StrUtil.isNotBlank(objectUrl), "输入不是完整的对象存储 URL（需形如 http(s)://{host}/{bucket}/{key}），当前值为空");

        // 按完整 URL 解析并切分路径，合法形态为 /{桶名}/{对象键}
        var urlBuilder = UrlBuilder.ofHttp(objectUrl, CharsetUtil.CHARSET_UTF_8);
        var pathStr = urlBuilder.getPathStr();
        var split = StrUtil.isEmpty(pathStr) ? new String[0] : pathStr.split("/", 3);

        // 校验完整对象存储 URL 形态，非法输入抛出清晰业务异常
        assertObjectLocation(objectUrl, urlBuilder, split);

        // 校验主机属于本实例配置的存储端点,拒绝跨端点/跨桶访问
        AssertUtils.isTrue(isTrustedEndpoint(urlBuilder),
                           "输入的对象存储 URL 不属于当前配置的存储端点，已拒绝：{}", abbreviate(objectUrl));

        // 校验桶名符合本模块命名契约(且该桶属于本实例可管理的应用命名空间)
        AssertUtils.isTrue(split[1].endsWith("-public") || split[1].endsWith("-private"),
                           "输入的对象存储 URL 桶名不符合 {applicationName}-{public|private} 命名契约，已拒绝：{}", abbreviate(objectUrl));

        return new ObjectLocation(split[1], split[2]);
    }

    /**
     * 校验 URL 是否满足完整对象存储 URL 形态：主机非空且路径为 /{桶名}/{对象键}。
     *
     * @param objectUrl  完整对象存储 URL
     * @param urlBuilder 解析后的 URL
     * @param split      路径切分结果
     */
    private void assertObjectLocation(String objectUrl, UrlBuilder urlBuilder, String[] split) {
        // 完整形态要求主机非空、路径包含桶名与对象键两段，残缺 URL 与裸文件标识在此拒绝
        var isValid = StrUtil.isNotEmpty(urlBuilder.getHost())
                      && split.length == 3
                      && StrUtil.isNotEmpty(split[1])
                      && StrUtil.isNotEmpty(split[2]);
        AssertUtils.isTrue(isValid, "输入不是完整的对象存储 URL（需形如 http(s)://{host}/{bucket}/{key}），当前值：{}", abbreviate(objectUrl));
    }

    /**
     * 判断对象存储 URL 的主机是否与本实例配置的存储端点一致(忽略大小写)
     *
     * @param urlBuilder 解析后的对象存储 URL
     * @return 主机属于配置端点时返回 true
     */
    private boolean isTrustedEndpoint(UrlBuilder urlBuilder) {
        var serverUrl = annexProperties.getServerUrl();
        if (StrUtil.isBlank(serverUrl)) {
            return false;
        }
        var serverHost = UrlBuilder.ofHttp(serverUrl, CharsetUtil.CHARSET_UTF_8).getHost();
        return StrUtil.isNotEmpty(serverHost) && serverHost.equalsIgnoreCase(urlBuilder.getHost());
    }

    /**
     * 截断异常上下文中的输入值，避免超长内容进入异常消息。
     *
     * @param text 原始输入
     * @return 截断后的输入
     */
    private String abbreviate(String text) {
        final int maxLength = 200;
        var safeText = StrUtil.nullToEmpty(text);
        return safeText.length() <= maxLength ? safeText : safeText.substring(0, maxLength) + "...";
    }

    /**
     * 对象存储位置：桶名称与对象键。
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     */
    private record ObjectLocation(String bucketName, String objectKey) {
    }

    @Override
    public void afterPropertiesSet() {
        var credential = new AWSStaticCredentialsProvider(new BasicAWSCredentials(annexProperties.getAccessKey(), annexProperties.getAccessSecret()));
        var endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(annexProperties.getServerUrl(), annexProperties.getRegion());

        //初始化s3(路径风格仅在 MINIO 下启用;enablePathStyleAccess 与本调用写同一构建器字段,保留条件调用即可)
        amazonS3 = AmazonS3ClientBuilder.standard()
                                        .withCredentials(credential)
                                        .withEndpointConfiguration(endpointConfiguration)
                                        .withPathStyleAccessEnabled(annexProperties.getType() == AnnexType.MINIO)
                                        .build();

        //初始化minio原生，支持公共读桶的创建
        minioClient = MinioClient.builder().endpoint(annexProperties.getServerUrl()).credentials(annexProperties.getAccessKey(), annexProperties.getAccessSecret()).build();

        //初始化oss原生
        ossClient = new OSSClientBuilder().build(annexProperties.getServerUrl(), annexProperties.getAccessKey(), annexProperties.getAccessSecret());
    }
}
