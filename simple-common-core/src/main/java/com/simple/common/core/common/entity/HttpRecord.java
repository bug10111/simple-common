package com.simple.common.core.common.entity;

import cn.hutool.http.HttpResponse;
import com.simple.common.core.exception.AbstractException;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.function.HttpRecordFunction;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * Description: http请求工具信息记录
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
public class HttpRecord {

    /**
     * 请求结果
     */
    private HttpResponse execute;

    public HttpRecord(HttpResponse execute) {
        this.execute = execute;
    }

    /**
     * 获取结果字符串
     */
    @SneakyThrows
    public <T> T get(Class<T> tClass, HttpRecordFunction function) {
        String body = execute.body();
        if(execute.getStatus() == 200) {
            return JsonUtils.toJsonObj(body, tClass);
        }else{
            throw function.handler(body);
        }
    }
}
