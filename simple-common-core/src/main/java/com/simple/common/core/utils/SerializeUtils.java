package com.simple.common.core.utils;

import java.nio.charset.StandardCharsets;

/**
 * Created with IntelliJ IDEA
 * 序列化工具
 *
 * @author 兄台丶请冷静
 */
public class SerializeUtils {

    /**
     * 序列化
     *
     * @param obj 对象
     */
    public static byte[] serialize(Object obj) {
        return JsonUtils.toJsonStr(obj).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 反序列化
     *
     * @param bytes 字节
     * @param <T>   对象
     */
    public static <T> T deserialize(byte[] bytes, Class<T> aclass) {
        return JsonUtils.toJsonObj(new String(bytes, StandardCharsets.UTF_8), aclass);
    }
}
