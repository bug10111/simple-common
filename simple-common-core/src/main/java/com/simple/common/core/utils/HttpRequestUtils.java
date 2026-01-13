package com.simple.common.core.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpRequest;
import com.simple.common.core.common.entity.HttpRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: http请求帮助类
 *
 * @author qty
 */
@Slf4j
public class HttpRequestUtils {

    /**
     * post请求
     *
     * @param url     请求路径
     * @param heads   请求头
     * @param body    请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static HttpRecord post(String url, Map<String, String> heads, Object body, Integer outTime) {
        HttpRequest post = HttpRequest.post(url);
        if (ObjUtil.isNotEmpty(body)) {
            post.body(JsonUtils.toJsonStr(body));
        }

        if (ObjUtil.isNotEmpty(heads)) {
            post.headerMap(heads, true);
        }
        return new HttpRecord(post.timeout(outTime).execute());
    }

    /**
     * get请求
     *
     * @param url     请求路径
     * @param heads   请求头
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static HttpRecord get(String url, Map<String, String> heads, Map<String, Object> map, Integer outTime) {
        HttpRequest post = HttpRequest.get(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }

        if (ObjUtil.isNotEmpty(heads)) {
            post.headerMap(heads, true);
        }
        return new HttpRecord(post.timeout(outTime).execute());
    }

    /**
     * get请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static HttpRecord get(String url, Map<String, Object> map, Integer outTime) {
        return get(url, null, map, outTime);
    }

}
