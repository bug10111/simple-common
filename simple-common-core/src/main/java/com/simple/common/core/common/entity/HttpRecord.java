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
        // 提前校验，避免null响应在get()时才以裸NPE暴露在远离成因的位置
        AssertUtils.notNull(execute, "HttpResponse 不能为null");
        this.execute = execute;
    }

    /**
     * 获取结果字符串，HTTP状态码为2xx视为成功
     *
     * @param tClass   响应体反序列化的目标类型
     * @param function 非2xx状态码时的异常构造函数，入参为响应体字符串；为null或返回null时使用默认DefaultException
     * @param <T>      目标类型
     * @return 反序列化后的响应对象
     * @throws AbstractException 当响应状态码非2xx时由 function 构造并抛出
     */
    @SneakyThrows
    public <T> T get(Class<T> tClass, HttpRecordFunction function) {
        try {
            String body = execute.body();
            int status = execute.getStatus();
            // 2xx（200-299）均为成功状态，201/204等与200同等对待
            if (status >= 200 && status < 300) {
                return JsonUtils.toJsonObj(body, tClass);
            }
            // function缺失或返回null时兜底为默认业务异常，避免throw null变成NPE
            DefaultException exception = function == null ? null : function.handler(body);
            if (exception == null) {
                throw new DefaultException(DefaultExceptionEnum.ERROR, "请求失败: HTTP " + status + ", " + body);
            }
            throw exception;
        } finally {
            // 响应体已读取完毕，释放底层连接
            execute.close();
        }
    }
}
