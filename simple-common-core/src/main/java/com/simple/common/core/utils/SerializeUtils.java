package com.simple.common.core.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA
 * 序列化工具
 * <p>共享 ObjectMapper 统一安全基线：注册 classpath 上可发现的模块（如 jsr310，支持 LocalDateTime 等
 * java.time 类型）、关闭未知字段失败（消息跨版本向前兼容）。如需定制请使用 Spring 容器托管的 Mapper。</p>
 *
 * @author qty
 */
public final class SerializeUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private SerializeUtils() {
    }

    /**
     * 序列化
     *
     * @param obj 对象
     * @return JSON字节数组
     */
    public static byte[] serialize(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("对象序列化失败: " + (obj == null ? "null" : obj.getClass().getName()), e);
        }
    }

    /**
     * 反序列化
     *
     * @param bytes  字节
     * @param aclass 对象类型
     * @param <T>    对象
     * @return 反序列化后的对象
     */
    public static <T> T deserialize(byte[] bytes, Class<T> aclass) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("反序列化入参bytes不能为空");
        }
        try {
            return OBJECT_MAPPER.readValue(bytes, aclass);
        } catch (IOException e) {
            throw new IllegalStateException("字节数组反序列化失败: " + aclass.getName(), e);
        }
    }

    /**
     * ObjectReader工厂
     */
    public static ObjectReader readerFor(Class<?> aclass) {
        return OBJECT_MAPPER.readerFor(aclass);
    }
}
