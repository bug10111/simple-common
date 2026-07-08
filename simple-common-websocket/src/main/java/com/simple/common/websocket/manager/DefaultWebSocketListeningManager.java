package com.simple.common.websocket.manager;

import cn.hutool.core.util.ObjUtil;
import com.alibaba.fastjson2.JSON;
import com.simple.common.websocket.common.entity.InvocationTarget;
import com.simple.common.websocket.common.entity.WebSocketRequest;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket监听管理器默认实现
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultWebSocketListeningManager implements WebSocketListeningManager {

    private final Map<String, Map<String, InvocationTarget>> listenerMap = new ConcurrentHashMap<>();

    @Override
    public void registerMethod(String type, String cliKey, Object bean, Method method, Class<?> dataType) {
        listenerMap.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                .put(cliKey, new InvocationTarget(bean, method, dataType));
    }

    @Override
    @SneakyThrows
    public Optional<Object> invoke(String type, String cliKey, WebSocketRequest<?> request) {
        Map<String, InvocationTarget> cliKeyMap = listenerMap.get(type);
        if (ObjUtil.isNotEmpty(cliKeyMap)) {
            InvocationTarget target = cliKeyMap.get(cliKey);
            if (ObjUtil.isNotEmpty(target)) {
                Object typedData = convertData(request.getData(), target.dataType());
                WebSocketRequest<Object> typedRequest = new WebSocketRequest<>();
                typedRequest.setData(typedData);
                Object invoke = target.method().invoke(target.bean(), typedRequest);
                return Optional.of(invoke);
            }
        }
        return Optional.empty();
    }

    /**
     * 将原始数据转换为目标类型
     */
    private Object convertData(Object rawData, Class<?> targetType) {
        if (rawData == null || targetType == Object.class) {
            return rawData;
        }
        if (targetType.isInstance(rawData)) {
            return rawData;
        }
        return JSON.toJavaObject(rawData, targetType);
    }
}