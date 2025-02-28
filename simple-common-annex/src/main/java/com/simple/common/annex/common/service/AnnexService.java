package com.simple.common.annex.common.service;

import com.simple.common.annex.common.dto.UploadResponse;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.annex.common.function.UploadFunction;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA
 * Description: 统一附件上传接口，所有的附件、图片上传都需要实现这个接口
 *
 * @author 兄台丶请冷静
 */
public interface AnnexService {

    /**
     * 附件上传
     *
     * @param file            file
     * @param applicationName 系统服务名
     * @param shareType       文件权限
     */
    UploadResponse upload(MultipartFile file, String applicationName, ShareType shareType);

    /**
     * 附件上传
     *
     * @param file            file
     * @param applicationName 系统服务名
     * @param packageName     包名
     * @param shareType       文件权限
     */
    UploadResponse upload(MultipartFile file, String applicationName, String packageName, ShareType shareType);

    /**
     * 附件上传
     *
     * @param fileName        文件名.格式
     * @param applicationName 系统服务名
     * @param packageName     包名
     * @param shareType       文件权限
     * @param inputStream     文件流
     * @param uploadFunction  文件对比操作
     */
    UploadResponse upload(String fileName, String applicationName, String packageName, ShareType shareType, InputStream inputStream,
                          UploadFunction uploadFunction);

    /**
     * 附件下载，以流的形式返回
     *
     * @param objectUrl
     */
    void writeGetObjectResponse(String objectUrl);

    /**
     * 获取附件访问路径
     *
     * @param objectUrl 文件完整路径
     * @return 有效期的文件访问url
     */
    String generateUrl(String objectUrl);

    /**
     * 附件删除
     *
     * @param objectUrl 文件完整路径
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
