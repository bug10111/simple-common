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
     * 获取结果字符串，HTTP状态码为2xx视为成功
     *
     * @param tClass   响应体反序列化的目标类型
     * @param function 非2xx状态码时的异常构造函数，入参为响应体字符串
     * @param <T>      目标类型
     * @return 反序列化后的响应对象
     * @throws AbstractException 当响应状态码非2xx时由 function 构造并抛出
     */
    @SneakyThrows
    public <T> T get(Class<T> tClass, HttpRecordFunction function) {
        String body = execute.body();
        // 2xx（200-299）均为成功状态，201/204等与200同等对待
        if(execute.getStatus() >= 200 && execute.getStatus() < 300) {
            return JsonUtils.toJsonObj(body, tClass);
        }else{
            throw function.handler(body);
        }
    }
}
