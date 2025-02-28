package com.simple.common.annex.manager;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
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
 * @author 兄台丶请冷静
 */
@Slf4j
@Service
public class DefaultS3Handler implements S3Manager, InitializingBean {

    @Autowired
    private AnnexProperties annexProperties;

    private AmazonS3 amazonS3;

    private MinioClient minioClient;

    @Override
    public String upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream) {
        var bucketName = buildBucketName(applicationName, shareType);
        if (annexProperties.getType() == AnnexType.MINIO) {
            createMinioBucket(bucketName, shareType);
        } else {
            createS3Bucket(bucketName, shareType);
        }
        var key = StrUtil.isEmpty(packageName) ? buildKey(fileName) : packageName;
        CannedAccessControlList acl = ShareType.PRIVATE.equals(shareType) ? CannedAccessControlList.Private : CannedAccessControlList.PublicRead;

        uploadToS3(bucketName, inputStream, key, fileName, acl);
        return annexProperties.getServerUrl() + "/" + bucketName + "/" + key;
    }

    @Override
    @SneakyThrows
    public void writeGetObjectResponse(String objectUrl) {
        var urlBuilder = UrlBuilder.ofHttp(objectUrl, CharsetUtil.CHARSET_UTF_8);
        var pathStr = urlBuilder.getPathStr();
        var split = pathStr.split("/", 3);

        //获取输入流
        S3Object object = amazonS3.getObject(split[1], split[2]);
        @Cleanup S3ObjectInputStream objectContent = object.getObjectContent();

        HttpServletResponse response = HttpServletUtils.getResponse();
        String[] fileUrl = object.getKey().split("/");
        var key = fileUrl[fileUrl.length - 1];
        key = URLEncoder.encode(key, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + key);

        //写入输出流，并关闭输出流
        ServletOutputStream outputStream = response.getOutputStream();
        IoUtil.write(outputStream, true, objectContent.readAllBytes());
    }

    @Override
    public InputStream download(String objectUrl) {
        var urlBuilder = UrlBuilder.ofHttp(objectUrl, CharsetUtil.CHARSET_UTF_8);
        var pathStr = urlBuilder.getPathStr();
        var split = pathStr.split("/", 3);

        GetObjectRequest getObjectRequest = new GetObjectRequest(split[1], split[2]);
        //        getObjectRequest.setRange(0,5000);
        S3Object object = amazonS3.getObject(getObjectRequest);
        return object.getObjectContent().getDelegateStream();
    }

    @Override
    public void delete(String objectUrl) {
        var urlBuilder = UrlBuilder.ofHttp(objectUrl, CharsetUtil.CHARSET_UTF_8);
        var pathStr = urlBuilder.getPathStr();
        var split = pathStr.split("/", 3);
        var bucketName = split[1];
        var key = split[2];
        if (amazonS3.doesObjectExist(bucketName, key)) {
            amazonS3.deleteObject(bucketName, key);
        }
    }

    @Override
    public String generateUrl(String objectUrl, int expireTime) {

        //通过url获取文件所在桶和存放位置
        var urlBuilder = UrlBuilder.ofHttp(objectUrl, CharsetUtil.CHARSET_UTF_8);
        var pathStr = urlBuilder.getPathStr();
        var split = pathStr.split("/", 3);

        //通过桶和文件位置，获取请求对象
        var urlRequest = new GeneratePresignedUrlRequest(split[1], split[2]);

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
    protected void createS3Bucket(String bucketName, ShareType shareType) {
        var exists = amazonS3.doesBucketExistV2(bucketName);
        synchronized (this) {
            if (!exists) {
                var request = new CreateBucketRequest(bucketName, annexProperties.getRegion());
                request.setCannedAcl(ShareType.PUBLIC == shareType ? CannedAccessControlList.PublicRead : CannedAccessControlList.Private);
                amazonS3.createBucket(request);

                // 检查桶是否创建成功，如果我们要建立的是公共桶，则再次设置桶为公共的
                exists = amazonS3.doesBucketExistV2(bucketName);
                if (exists && ShareType.PUBLIC == shareType) {
                    // 再次设置 ACL，确保 PublicRead 成功应用
                    var aclRequest = new SetBucketAclRequest(bucketName, CannedAccessControlList.PublicRead);
                    amazonS3.setBucketAcl(aclRequest);
                }
            }
        }
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
                         + "{\"AWS\":[\"*\"]},\"Action\":[\"s3:ListBucket\",\"s3:ListBucketMultipartUploads\","
                         + "\"s3:GetBucketLocation\"],\"Resource\":[\"arn:aws:s3:::" + bucketName
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

    @Override
    public void afterPropertiesSet() {
        var credential = new AWSStaticCredentialsProvider(new BasicAWSCredentials(annexProperties.getAccessKey(), annexProperties.getAccessSecret()));
        var endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(annexProperties.getServerUrl(), annexProperties.getRegion());

        //初始化s3
        amazonS3 = AmazonS3ClientBuilder.standard()
                                        .withCredentials(credential)
                                        .withEndpointConfiguration(endpointConfiguration)
                                        .enablePathStyleAccess()
                                        .build();

        //初始化minio原生，支持公共读桶的创建
        minioClient = MinioClient.builder()
                                 .endpoint(annexProperties.getServerUrl())
                                 .credentials(annexProperties.getAccessKey(), annexProperties.getAccessSecret())
                                 .build();
    }
}
