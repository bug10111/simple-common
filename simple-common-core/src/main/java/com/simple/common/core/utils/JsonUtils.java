package com.simple.common.core.utils;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * <p>
 * json工具转化工具类
 * </p>
 * <p>引擎契约（重要）：本类历史上混合使用两套JSON引擎，为不破坏既有缓存数据的读写兼容（鉴权权限缓存重度依赖），
 * 引擎归属按方法固定并在此声明——序列化与Bean转换走 Hutool {@link JSONUtil}，集合反序列化走 fastjson2 {@link JSON}；
 * 两套引擎在日期格式/数字/空值上的默认行为存在差异，跨引擎读写同一份数据时须确认语义兼容。</p>
 *
 * @author qty
 */
public class JsonUtils {

    private JsonUtils() {
    }

    /**
     * 将对象转化为JSON字符串（Hutool引擎）
     *
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJsonStr(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }

    /**
     * 将对象转化为JSON字符串（格式化输出，Hutool引擎）
     *
     * @param obj 对象
     * @return 格式化JSON字符串
     */
    public static String toJsonPrettyStr(Object obj) {
        return JSONUtil.toJsonPrettyStr(obj);
    }

    /**
     * 将JSON字符串转化为对象（Hutool引擎）
     *
     * @param json json字符串（null/空白返回null，不抛解析异常）
     * @param c    对象class
     * @param <T>  对象类型
     * @return 反序列化对象；入参空白或解析失败返回null
     */
    public static <T> T toJsonObj(String json, Class<T> c) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return JSONUtil.toBean(json, c);
    }

    /**
     * 将list JSON字符串转化为集合对象（fastjson2引擎，契约见类说明）
     *
     * @param json json字符串（null/空白返回空集合）
     * @param c    对象class
     * @param <T>  集合元素类型
     * @return 集合对象；入参空白返回空集合，解析异常按fastjson2原语义抛出
     */
    public static <T> List<T> toList(String json, Class<T> c) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        return JSON.parseArray(json, c);
    }
}
