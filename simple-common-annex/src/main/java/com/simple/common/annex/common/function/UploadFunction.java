package com.simple.common.annex.common.function;

import com.simple.common.annex.common.dto.UploadResponse;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@FunctionalInterface
public interface UploadFunction {

    /**
     * 判断是否已经上传,已经上传则查询出结果
     *
     * @param uploadResponse 附件上传的返回参数
     * @return 是否已经上传
     */
    boolean handler(UploadResponse uploadResponse) throws Throwable;

}
