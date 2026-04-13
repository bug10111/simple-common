package com.simple.common.websocket.manager;

import cn.hutool.core.util.ObjUtil;
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
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultWebSocketListeningManager implements WebSocketListeningManager {

    private final Map<String, Map<String, InvocationTarget>> listenerMap = new ConcurrentHashMap<>();

    @Override
    public void registerMethod(String type, String cliKey, Object bean, Method method) {
        // 注册到双层 Map 结构: type -> cliKey -> InvocationTarget
        listenerMap.computeIfAbsent(type, k -> new ConcurrentHashMap<>()).put(cliKey, new InvocationTarget(bean, method));
    }

    @Override
    @SneakyThrows
    public Optional<Object> invoke(String type, String cliKey, WebSocketRequest request) {
        Map<String, InvocationTarget> cliKeyMap = listenerMap.get(type);
        if(ObjUtil.isNotEmpty(cliKeyMap)){
            InvocationTarget target = cliKeyMap.get(cliKey);
            if(ObjUtil.isNotEmpty(target)){
                Object invoke = target.method().invoke(target.bean(), request);
                return Optional.of(invoke);
            }
        }
        return Optional.empty();
    }
}