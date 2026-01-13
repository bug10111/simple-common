package com.simple.common.annex.common.manager;

import com.simple.common.annex.common.enums.ShareType;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA
 * Description: S3协议操作
 *
 * @author qty
 */
public interface S3Manager {

    /**
     * 附件上传
     *
     * @param fileName        文件名
     * @param applicationName 系统服务名
     * @param packageName     包名
     * @param shareType       文件权限
     * @param inputStream     文件流
     */
    String upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream);

    /**
     * 附件下载
     *
     * @param objectUrl 文件有效URL
     */
    void writeGetObjectResponse(String objectUrl);

    /**
     * 附件下载，业务自己关闭流
     *
     * @param objectUrl 文件有效URL
     */
    InputStream download(String objectUrl);

    /**
     * 附件删除
     *
     * @param objectUrl 文件完整路径
     */
    void delete(String objectUrl);

    /**
     * 获取附件访问路径
     *
     * @param objectUrl  文件完整路径
     * @param expireTime 过期时间，单位分钟
     * @return 有效期的文件访问url
     */
    String generateUrl(String objectUrl, int expireTime);

}
