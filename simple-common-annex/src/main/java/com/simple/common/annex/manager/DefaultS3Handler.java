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

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultS3Handler implements S3Manager, InitializingBean {

    @Autowired
    private AnnexProperties annexProperties;

    private AmazonS3 amazonS3;

    private MinioClient minioClient;

    private OSS ossClient;

    @Override
    public String upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream) {
        var bucketName = buildBucketName(applicationName, shareType);
        if (annexProperties.getType() == AnnexType.MINIO) {
            createMinioBucket(bucketName, shareType);
        } else {
            createOssBucket(bucketName, shareType);
        }
        var key = StrUtil.isEmpty(packageName) ? buildKey(fileName) : packageName;
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

        HttpServletResponse response = HttpServletUtils.getResponse();
        String[] fileUrl = object.getKey().split("/");
        var key = fileUrl[fileUrl.length - 1];
        key = URLEncoder.encode(key, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=UTF-8''" + key);

        //写入输出流，并关闭输出流
        ServletOutputStream outputStream = response.getOutputStream();
        IoUtil.write(outputStream, true, objectContent.readAllBytes());
    }

    @Override
    public InputStream download(String objectUrl) {
        // 解析完整对象存储 URL，获取桶名称与对象键
        var objectLocation = parseObjectLocation(objectUrl);

        GetObjectRequest getObjectRequest = new GetObjectRequest(objectLocation.bucketName(), objectLocation.objectKey());
        //        getObjectRequest.setRange(0,5000);
        S3Object object = amazonS3.getObject(getObjectRequest);
        return object.getObjectContent().getDelegateStream();
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

        //计算过期时间
        var expireDate = DateUtil.offset(new Date(), DateField.MINUTE, expireTime);
        urlRequest.setExpiration(expireDate);
        return amazonS3.generatePresignedUrl(urlRequest).toString();
    }

    @SneakyThrows
    private void uploadToS3(String bucketName, InputStream inputStream, String key, String originalFilename, CannedAccessControlList cannedAcl) {

        //获取过期时间
        var expireDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(annexProperties.getExpireTime()));

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setHttpExpiresDate(expireDate);
        metadata.setContentLength(inputStream.available());
        metadata.setContentDisposition("attachment;filename=" + URLEncoder.encode(originalFilename, StandardCharsets.UTF_8));

        //上传minio
        amazonS3.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata).withCannedAcl(cannedAcl));
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
     * 创建桶
     *
     * @param bucketName 桶名称
     * @param shareType  附件类型
     */
    public void createOssBucket(String bucketName, ShareType shareType) {

        // 1. 检查桶是否存在（阿里云要求全局唯一）
//        if (ossClient.doesBucketExist(bucketName)) {
//            return;
//        }

        // 2. 创建桶（默认私有）
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);

        // 3.设置存储类型
        createBucketRequest.setStorageClass(StorageClass.Standard);

        // 6. 创建 Bucket
        ossClient.createBucket(createBucketRequest);
    }

    /**
     * 创建minio桶,minio 不支持s3创建公共桶，这里是单独的minio接口服务
     *
     * @param bucketName 桶名称
     * @param shareType  附件类型
     */
    @SneakyThrows
    protected void createMinioBucket(String bucketName, ShareType shareType) {
        var exists = amazonS3.doesBucketExistV2(bucketName);
        synchronized (this) {
            if (!exists) {
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
        //文件后缀作为文件夹名称
        var suffix = FileUtil.getSuffix(fileName);
        return suffix + "/" + IdUtils.getFastSimpleUUID() + "." + suffix;
    }

    /**
     * 解析完整对象存储 URL，获取桶名称与对象键。
     * <p>
     * 仅接受形如 {@code {服务地址}/{桶名}/{对象键}} 的完整对象存储 URL，
     * 路径必须包含桶名与对象键两段且均非空；仅传对象键或文件标识将被拒绝。
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
     * 截断异常上下文中的输入值，避免超长内容进入异常消息。
     *
     * @param text 原始输入
     * @return 截断后的输入
     */
    private String abbreviate(String text) {
        final int maxLength = 200;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
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

        //初始化s3
        amazonS3 = AmazonS3ClientBuilder.standard().withCredentials(credential).withEndpointConfiguration(endpointConfiguration).enablePathStyleAccess().withPathStyleAccessEnabled(annexProperties.getType() == AnnexType.MINIO) .build();

        //初始化minio原生，支持公共读桶的创建
        minioClient = MinioClient.builder().endpoint(annexProperties.getServerUrl()).credentials(annexProperties.getAccessKey(), annexProperties.getAccessSecret()).build();

        //初始化oss原生
        ossClient = new OSSClientBuilder().build(annexProperties.getServerUrl(), annexProperties.getAccessKey(), annexProperties.getAccessSecret());
    }
}
