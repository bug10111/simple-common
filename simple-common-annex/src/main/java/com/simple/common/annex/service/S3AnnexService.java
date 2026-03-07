package com.simple.common.annex.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import com.simple.common.annex.common.dto.UploadResponse;
import com.simple.common.annex.common.enums.Algorithm;
import com.simple.common.annex.common.enums.ShareType;
import com.simple.common.annex.common.function.UploadFunction;
import com.simple.common.annex.common.manager.S3Manager;
import com.simple.common.annex.common.properties.AnnexProperties;
import com.simple.common.annex.common.service.AnnexService;
import com.simple.common.core.utils.AlgorithmUtils;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class S3AnnexService implements AnnexService {

    @Autowired
    private S3Manager s3Manager;

    @Autowired
    private AnnexProperties annexProperties;

    @Override
    @SneakyThrows
    public UploadResponse upload(MultipartFile file, String applicationName, ShareType shareType) {
        @Cleanup var fileInputStream = file.getInputStream();
        String filename = file.getOriginalFilename();
        return upload(filename, applicationName, null, shareType, fileInputStream);
    }

    @Override
    @SneakyThrows
    public UploadResponse upload(MultipartFile file, String applicationName, String packageName, ShareType shareType) {
        @Cleanup var fileInputStream = file.getInputStream();
        String filename = file.getOriginalFilename();
        return upload(filename, applicationName, packageName, shareType, fileInputStream);
    }

    @Override
    @SneakyThrows
    public UploadResponse upload(String fileName, String applicationName, String packageName, ShareType shareType, @NonNull InputStream inputStream,
                                 UploadFunction uploadFunction) {
        var bytes = inputStream.readAllBytes();
        @Cleanup var in = new ByteArrayInputStream(bytes);

        //组装数据
        UploadResponse uploadResponse = new UploadResponse().setTotalSize(bytes.length)
                                                            .setName(fileName)
                                                            .setApplicationName(applicationName)
                                                            .setSuffix(FileUtil.getSuffix(fileName))
                                                            .setShareType(shareType);
        if (ObjUtil.isNotNull(annexProperties.getAlgorithm()) && annexProperties.getAlgorithm().equals(Algorithm.MD5)) {
            uploadResponse.setAlgorithmType(Algorithm.MD5);
            uploadResponse.setAlgorithmValue(AlgorithmUtils.md5Hex(bytes));
        }

        //判断是否已经上传，已经上传则返回
        if (uploadFunction != null) {
            boolean handler = uploadFunction.handler(uploadResponse);
            if (handler) {
                return uploadResponse;
            }
        }

        //执行上传
        var fullPath = s3Manager.upload(fileName, applicationName, packageName, shareType, in);
        uploadResponse.setSaveUrl(fullPath);
        uploadResponse.setIsTrue(false);
        return uploadResponse;
    }

    @Override
    public void writeGetObjectResponse(String objectUrl) {
        s3Manager.writeGetObjectResponse(objectUrl);
    }

    @Override
    public String generateUrl(String objectUrl) {
        return s3Manager.generateUrl(objectUrl, annexProperties.getExpireTime());
    }

    @Override
    public void delete(String objectUrl) {
        s3Manager.delete(objectUrl);
    }
}
