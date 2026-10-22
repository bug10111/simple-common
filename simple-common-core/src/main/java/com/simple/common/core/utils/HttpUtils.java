package com.simple.common.core.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.simple.common.core.common.entity.HttpErrorRecord;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 * Description: http请求帮助类
 *
 * @author qty
 */
@Deprecated
@Slf4j
public class HttpUtils {

    /**
     * post请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> post(String url, Map<String, String> heads, String ParametersBody, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.post(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }

    /**
     * post请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> post(String url, String ParametersBody, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.post(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }

    /**
     * post请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> post(String url, Map<String, Object> map, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.post(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }

    /**
     * post请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> post(String url, Map<String, String> heads, Map<String, Object> map, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.post(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }

    /**
     * get请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> get(String url, Map<String, Object> map, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.get(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }

    /**
     * get请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> get(String url, Map<String, String> heads, Map<String, Object> map, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.get(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }

    /**
     * delete请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> delete(String url, String ParametersBody, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.delete(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }


    /**
     * delete请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> delete(String url, Map<String, String> heads, String ParametersBody, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.delete(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }

    /**
     * delete请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> delete(String url, Map<String, Object> map, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.delete(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }

    /**
     * delete请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    public static <T> Optional<T> delete(String url, Map<String, String> heads, Map<String, Object> map, Integer outTime, Class<T> rClass, HttpErrorRecord record) {
        HttpRequest post = HttpRequest.delete(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass, record);
    }

    /**
     * 处理返回结果
     *
     * @param url     请求地址
     * @param execute HttpResponse返回对象
     * @param rClass  返回目标对象class
     */
    private static <T> Optional<T> response(String url, HttpResponse execute, Class<T> rClass, HttpErrorRecord record) throws RuntimeException {
        try {
            if (execute.isOk()) {
                return Optional.ofNullable(JsonUtils.toJsonObj(execute.body(), rClass));
            } else {
                log.error("请求[{}]失败！==>{}", url, execute.body());
                // record为null时不再NPE；响应体已在setExecute中提取，finally统一关闭底层连接
                if (record != null) {
                    record.setExecute(execute);
                }
                return Optional.empty();
            }
        } finally {
            // 无论成败都释放连接，防止高并发下连接池/文件句柄耗尽
            execute.close();
        }
    }

    /**
     * post请求
     *
     * @param url            请求路径
     * @param heads          请求头
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     */
    public static void post(String url, Map<String, String> heads, String ParametersBody, Integer outTime) {
        HttpRequest post = HttpRequest.post(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        response(url, execute);
    }

    /**
     * post请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     */
    public static void post(String url, String ParametersBody, Integer outTime) {
        HttpRequest post = HttpRequest.post(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        response(url, execute);
    }

    /**
     * post请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     */
    public static void post(String url, Map<String, Object> map, Integer outTime) {
        HttpRequest post = HttpRequest.post(url);
        post.form(map);
        HttpResponse execute = post.timeout(outTime).execute();
        response(url, execute);
    }

    /**
     * post请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> post(String url, Map<String, String> heads, String ParametersBody, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.post(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * post请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> post(String url, String ParametersBody, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.post(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * post请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> post(String url, Map<String, Object> map, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.post(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * post请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> post(String url, Map<String, String> heads, Map<String, Object> map, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.post(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * get请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> get(String url, Map<String, Object> map, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.get(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * get请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> get(String url, Map<String, String> heads, Map<String, Object> map, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.get(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * delete请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> delete(String url, String ParametersBody, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.delete(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * delete请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> delete(String url, Map<String, String> heads, String ParametersBody, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.delete(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * delete请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> delete(String url, Map<String, Object> map, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.delete(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * delete请求
     *
     * @param url     请求路径
     * @param map     请求参数
     * @param outTime 请求超时时间 毫秒
     * @return 数据返回
     */
    @Deprecated
    public static <T> Optional<T> delete(String url, Map<String, String> heads, Map<String, Object> map, Integer outTime, Class<T> rClass) {
        HttpRequest post = HttpRequest.delete(url);
        if (ObjUtil.isNotEmpty(map)) {
            post.form(map);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        return response(url, execute, rClass);
    }

    /**
     * delete请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     */
    public static void delete(String url, String ParametersBody, Integer outTime) {
        HttpRequest post = HttpRequest.delete(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.timeout(outTime).execute();
        response(url, execute);
    }

    /**
     * delete请求
     *
     * @param url            请求路径
     * @param ParametersBody 请求参数json
     * @param outTime        请求超时时间 毫秒
     */
    public static void delete(String url, Map<String, String> heads, String ParametersBody, Integer outTime) {
        HttpRequest post = HttpRequest.delete(url);
        if (StrUtil.isNotBlank(ParametersBody)) {
            post.body(ParametersBody);
        }
        HttpResponse execute = post.headerMap(heads, true).timeout(outTime).execute();
        response(url, execute);
    }

    /**
     * 处理返回结果
     *
     * @param url     请求地址
     * @param execute HttpResponse返回对象
     * @param rClass  返回目标对象class
     */
    @Deprecated
    private static <T> Optional<T> response(String url, HttpResponse execute, Class<T> rClass) throws RuntimeException {
        try {
            if (execute.isOk()) {
                return Optional.ofNullable(JsonUtils.toJsonObj(execute.body(), rClass));
            } else {
                log.error("请求[{}]失败！==>{}", url, execute.body());
                throw new DefaultException(DefaultExceptionEnum.ERROR, execute.body());
            }
        } finally {
            // 无论成败都释放连接
            execute.close();
        }
    }

    /**
     * 处理返回结果
     *
     * @param url     请求地址
     * @param execute HttpResponse返回对象
     */
    private static void response(String url, HttpResponse execute) throws RuntimeException {
        try {
            if (!execute.isOk()) {
                log.error("请求[{}]失败！==>{}", url, execute.body());
                // 抛业务异常并携带状态码，替代无信息RuntimeException，便于调用方统一按DefaultException处理
                throw new DefaultException(DefaultExceptionEnum.ERROR, "请求失败: HTTP " + execute.getStatus());
            }
        } finally {
            // 无论成败都释放连接
            execute.close();
        }
    }
}
