package com.simple.common.annex.manager;

import com.simple.common.annex.common.enums.AnnexType;
import com.simple.common.annex.common.properties.AnnexProperties;
import com.simple.common.core.exception.DefaultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S3 处理器对象存储 URL 边界校验单测：完整 URL 正常签发、裸文件标识与空值抛业务异常。
 *
 * @author qty
 */
class DefaultS3HandlerTest {

    private static final String SERVER_URL = "http://127.0.0.1:9000";

    private static final String BARE_FILE_ID = "2111844814240227328";

    private DefaultS3Handler defaultS3Handler;

    /**
     * 初始化待测处理器：存储参数指向本机地址，仅用于离线签名计算。
     */
    @BeforeEach
    void setUp() {
        AnnexProperties annexProperties = new AnnexProperties();
        annexProperties.setServerUrl(SERVER_URL);
        annexProperties.setRegion("us-east-1");
        annexProperties.setAccessKey("test-access-key");
        annexProperties.setAccessSecret("test-access-secret");
        annexProperties.setType(AnnexType.MINIO);

        defaultS3Handler = new DefaultS3Handler();
        ReflectionTestUtils.setField(defaultS3Handler, "annexProperties", annexProperties);
        defaultS3Handler.afterPropertiesSet();
    }

    /**
     * 完整对象存储 URL 正常签发：桶名与对象键按路径正确拆分，签名参数保持原行为。
     */
    @Test
    void generateUrlWithCompleteUrlShouldSignBucketAndKey() {
        // 使用带多级目录的完整 URL 签发临时链接
        String signedUrl = defaultS3Handler.generateUrl(SERVER_URL + "/user-service-private/docx/recording-1.docx", 30);

        // 校验签名 URL 的桶名、对象键与签名参数未发生回归
        assertTrue(signedUrl.startsWith(SERVER_URL + "/user-service-private/"));
        assertTrue(signedUrl.contains("/user-service-private/docx/recording-1.docx?"));
        assertTrue(signedUrl.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
        assertTrue(signedUrl.contains("X-Amz-Expires=1799"));
        assertTrue(signedUrl.contains("X-Amz-SignedHeaders=host"));
        assertTrue(signedUrl.contains("X-Amz-Credential="));
        assertTrue(signedUrl.contains("X-Amz-Signature="));
    }

    /**
     * https 完整对象存储 URL 正常签发：桶名与对象键按路径正确拆分，签名沿用存储服务地址。
     */
    @Test
    void generateUrlWithHttpsUrlShouldSignBucketAndKey() {
        // 使用 https 完整 URL 签发临时链接
        String signedUrl = defaultS3Handler.generateUrl("https://oss.example.com/bucket/question/answer.pdf", 30);

        // 校验签名 URL 的桶名、对象键与签名参数未发生回归
        assertTrue(signedUrl.startsWith(SERVER_URL + "/bucket/question/answer.pdf?"), signedUrl);
        assertTrue(signedUrl.contains("X-Amz-Signature="), signedUrl);
    }

    /**
     * 裸文件标识不是完整对象存储 URL：生成临时链接抛清晰业务异常而非数组越界。
     */
    @Test
    void generateUrlWithBareFileIdShouldThrowBusinessException() {
        // 裸文件标识会被解析为主机，路径为空，必须被拒绝
        DefaultException exception = assertThrows(DefaultException.class,
                () -> defaultS3Handler.generateUrl(BARE_FILE_ID, 30));

        // 异常信息需指明输入不是完整对象存储 URL
        assertTrue(exception.getMessage().contains("完整的对象存储 URL"));
    }

    /**
     * 四处对象存储操作共用同一入参校验：裸文件标识均抛清晰业务异常。
     */
    @Test
    void allMethodsWithBareFileIdShouldThrowBusinessException() {
        // 逐一校验下载到响应、下载输入流、删除、生成临时链接四个入口
        assertThrows(DefaultException.class, () -> defaultS3Handler.writeGetObjectResponse(BARE_FILE_ID));
        assertThrows(DefaultException.class, () -> defaultS3Handler.download(BARE_FILE_ID));
        assertThrows(DefaultException.class, () -> defaultS3Handler.delete(BARE_FILE_ID));
        assertThrows(DefaultException.class, () -> defaultS3Handler.generateUrl(BARE_FILE_ID, 30));
    }

    /**
     * 空串与空值入参：抛清晰业务异常而非空指针。
     */
    @Test
    void emptyOrNullUrlShouldThrowBusinessException() {
        // 空串与空值均不允许进入 URL 解析
        DefaultException emptyException = assertThrows(DefaultException.class,
                () -> defaultS3Handler.generateUrl("", 30));
        assertTrue(emptyException.getMessage().contains("完整的对象存储 URL"));

        assertThrows(DefaultException.class, () -> defaultS3Handler.generateUrl(null, 30));
        assertThrows(DefaultException.class, () -> defaultS3Handler.generateUrl("   ", 30));
        assertThrows(DefaultException.class, () -> defaultS3Handler.writeGetObjectResponse(""));
        assertThrows(DefaultException.class, () -> defaultS3Handler.download(null));
        assertThrows(DefaultException.class, () -> defaultS3Handler.delete(""));
    }

    /**
     * 不完整 URL 形态：缺对象键、缺桶名、缺主机均抛清晰业务异常。
     */
    @Test
    void incompleteUrlShouldThrowBusinessException() {
        // 缺少对象键
        assertThrows(DefaultException.class, () -> defaultS3Handler.generateUrl(SERVER_URL + "/bucket", 30));
        assertThrows(DefaultException.class, () -> defaultS3Handler.generateUrl(SERVER_URL + "/bucket/", 30));

        // 缺少桶名
        assertThrows(DefaultException.class, () -> defaultS3Handler.generateUrl(SERVER_URL + "//key", 30));

        // 缺少主机
        assertThrows(DefaultException.class, () -> defaultS3Handler.generateUrl("http:///bucket/key", 30));
    }
}
