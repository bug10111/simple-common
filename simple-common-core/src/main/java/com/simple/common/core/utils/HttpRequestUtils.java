package com.simple.common.core.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpRequest;
import com.simple.common.core.common.entity.HttpRecord;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: http请求帮助类
 *
 * @author qty
 */
public class HttpRequestUtils {

    /**
     * JSON 请求体响应头名称
     */
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    /**
     * JSON 请求体 Content-Type 取值
     */
    private static final String JSON_CONTENT_TYPE = "application/json";

    /**
     * 默认请求超时时间（毫秒），outTime为null时的兜底，避免拆箱NPE
     */
    private static final int DEFAULT_TIMEOUT_MILLIS = 30_000;

    /**
     * post请求（JSON 请求体）
     *
     * <p>历史重载：与 {@link #postJson(String, Map, Object, Integer)} 语义一致，保留以兼容既有调用方；
     * 新增调用一律使用语义显式的 postJson / postForm。
     * （原 {@code post(String, Map, Map, Integer)} form 形态重载已删除：其与 Object 形态构成静态分派陷阱，
     * Map 实参被静默按表单发送，仓库内无任何调用方。）</p>
     *
     * @param url     请求路径
     * @param heads   请求头
     * @param body    请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static HttpRecord post(String url, Map<String, String> heads, Object body, Integer outTime) {
        return postJson(url, heads, body, outTime);
    }

    /**
     * post表单请求（form 表单体）
     *
     * @param url     请求路径
     * @param heads   请求头
     * @param form    表单参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static HttpRecord postForm(String url, Map<String, String> heads, Map<String, Object> form, Integer outTime) {
        HttpRequest post = HttpRequest.post(url);
        if (ObjUtil.isNotEmpty(form)) {
            post.form(form);
        }

        if (ObjUtil.isNotEmpty(heads)) {
            post.headerMap(heads, true);
        }
        return new HttpRecord(post.timeout(defaultTimeout(outTime)).execute());
    }

    /**
     * post请求（JSON 请求体）
     *
     * <p>请求体显式序列化为 JSON 文本，并保证 Content-Type 为 application/json；
     * 调用方已显式声明 Content-Type（大小写不敏感）时保留原值，不覆盖。</p>
     *
     * @param url     请求路径
     * @param heads   请求头
     * @param body    请求参数（JSON 序列化）
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static HttpRecord postJson(String url, Map<String, String> heads, Object body, Integer outTime) {
        HttpRequest post = HttpRequest.post(url);
        // JSON体仅判null：{}、""、[] 是合法JSON载荷，非空判断会把它们静默丢掉
        if (body != null) {
            post.body(JsonUtils.toJsonStr(body));
        }

        Map<String, String> headers = ensureJsonContentType(heads);
        if (ObjUtil.isNotEmpty(headers)) {
            post.headerMap(headers, true);
        }
        return new HttpRecord(post.timeout(defaultTimeout(outTime)).execute());
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
        HttpRequest get = HttpRequest.get(url);
        if (ObjUtil.isNotEmpty(map)) {
            get.form(map);
        }

        if (ObjUtil.isNotEmpty(heads)) {
            get.headerMap(heads, true);
        }
        return new HttpRecord(get.timeout(defaultTimeout(outTime)).execute());
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

    /**
     * 保证请求头携带 JSON Content-Type
     *
     * <p>调用方已声明 Content-Type 时保留原值（大小写不敏感判定）；未声明时补充 application/json。</p>
     *
     * @param heads 原始请求头
     * @return 补充后的请求头
     */
    private static Map<String, String> ensureJsonContentType(Map<String, String> heads) {
        Map<String, String> headers = heads == null ? new HashMap<>() : new HashMap<>(heads);
        for (String headerName : headers.keySet()) {
            if (CONTENT_TYPE_HEADER.equalsIgnoreCase(headerName)) {
                return headers;
            }
        }
        headers.put(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);
        return headers;
    }

    /**
     * 超时兜底：outTime为null时返回默认超时，避免自动拆箱NPE
     */
    private static int defaultTimeout(Integer outTime) {
        return outTime == null ? DEFAULT_TIMEOUT_MILLIS : outTime;
    }

}
