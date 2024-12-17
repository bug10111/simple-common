package com.simple.common.core.common.entity;

import cn.hutool.http.HttpResponse;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.utils.JsonUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * Description: http请求工具的异常信息记录
 *
 * @author 兄台丶请冷静
 */
@Data
@Accessors(chain = true)
@Schema(description = "http请求工具的异常信息记录")
public class HttpErrorRecord {

    @Schema(description = "请求结果")
    private HttpResponse execute;

    /**
     * 获取结果字符串
     */
    public String getStr() {
        return execute.body();
    }

    /**
     * 获取结果对象
     *
     * @param clazz 结果对象class
     * @param <T>   结果对象
     */
    public <T> T getObj(Class<T> clazz) {
        return JsonUtils.toJsonObj(getStr(), clazz);
    }

    /**
     * 获取异常对象
     */
    public DefaultException getException() {
        return new DefaultException(DefaultExceptionEnum.ERROR, getStr());
    }

    /**
     * 获取异常对象
     */
    public DefaultException getException(String errorStr) {
        return new DefaultException(DefaultExceptionEnum.ERROR, errorStr);
    }
}
