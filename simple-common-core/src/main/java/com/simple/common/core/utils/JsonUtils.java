package com.simple.common.core.utils;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Created by 兄台丶请冷静 on 2023/10/28 14:32
 * <p>
 * json工具转化工具类
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class JsonUtils {

    /**
     * 将对象转化为JSON字符串
     *
     * @param obj 对象
     */
    public static String toJsonStr(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }

    /**
     * 将对象转化为JSON字符串
     *
     * @param obj 对象
     */
    public static String toJsonPrettyStr(Object obj) {
        return JSONUtil.toJsonPrettyStr(obj);
    }

    /**
     * 将JSON字符串转化为对象
     *
     * @param json json字符串
     * @param c    对象class
     */
    public static <T> T toJsonObj(String json, Class<T> c) {
        return JSONUtil.toBean(json, c);
    }

    /**
     * 将list JSON字符串转化为集合对象
     *
     * @param json json字符串
     * @param c    对象class
     */
    public static <T> List<T> toList(String json, Class<T> c) {
        return JSONUtil.toList(json, c);
    }

}
