package com.simple.common.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.SneakyThrows;

/**
 * Created with IntelliJ IDEA
 * 序列化工具
 *
 * @author qty
 */
public class SerializeUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 序列化
     *
     * @param obj 对象
     */
    @SneakyThrows
    public static byte[] serialize(Object obj) {
        return objectMapper.writeValueAsBytes(obj);
    }

    /**
     * 反序列化
     *
     * @param bytes 字节
     * @param <T>   对象
     */
    @SneakyThrows
    public static <T> T deserialize(byte[] bytes, Class<T> aclass) {
        return objectMapper.readValue(bytes, aclass);
    }

    /**
     * ObjectReader工厂
     */
    @SneakyThrows
    public static ObjectReader readerFor(Class<?> aclass) {
        return objectMapper.readerFor(aclass);
    }
}
