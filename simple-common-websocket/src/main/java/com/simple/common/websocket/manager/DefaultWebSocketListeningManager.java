package com.simple.common.websocket.manager;

import com.simple.common.core.utils.AssertUtils;
import com.simple.common.websocket.common.entity.InvocationTarget;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultWebSocketListeningManager implements WebSocketListeningManager {

    private final Map<String, Map<String, InvocationTarget>> listenerMap = new HashMap<>();

    @Override
    public void registerMethod(String type, String cliKey, Object bean, Method method) {
        // 注册到双层 Map 结构: type -> cliKey -> InvocationTarget
        listenerMap.computeIfAbsent(type, k -> new HashMap<>()).put(cliKey, new InvocationTarget(bean, method));
    }

    @Override
    @SneakyThrows
    public Optional<Object> invoke(String type, String cliKey, String msg) {
        Map<String, InvocationTarget> cliKeyMap = listenerMap.get(type);
        AssertUtils.notEmptyParams(cliKeyMap, "不存在该类型：{} 的websocket监听器", type);

        InvocationTarget target = cliKeyMap.get(cliKey);
        AssertUtils.notEmptyParams(target, "不存在该类型下：{}==>客户端{} 的websocket监听器", type, cliKey);
        Object invoke = target.method().invoke(target.bean(), msg);
        return Optional.of(invoke);
    }
}
