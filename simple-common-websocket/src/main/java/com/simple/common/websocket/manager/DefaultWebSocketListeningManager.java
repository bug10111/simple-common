package com.simple.common.websocket.manager;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.websocket.common.constant.WebSocketConstant;
import com.simple.common.websocket.common.entity.InvocationTarget;
import com.simple.common.websocket.common.entity.WebSocketRequest;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import com.simple.common.websocket.utils.WebSocketUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
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
public class DefaultWebSocketListeningManager implements WebSocketListeningManager {

    private final Map<String, Map<String, InvocationTarget>> listenerMap = new ConcurrentHashMap<>();

    @Override
    public void registerMethod(String type, String cliKey, Object bean, Method method, Class<?> dataType) {
        AssertUtils.notEmpty(type, "type 不能为空白");
        AssertUtils.notEmpty(cliKey, "cliKey 不能为空白");
        AssertUtils.notNull(bean, "bean 不能为空");
        AssertUtils.notNull(method, "method 不能为空");
        AssertUtils.notNull(dataType, "dataType 不能为空，无法解析泛型时请传 Object.class");

        // 重复注册策略：后注册者覆盖先注册者，与 Map.put 语义一致；覆盖时告警提示潜在的配置冲突
        InvocationTarget previous = listenerMap.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                .put(cliKey, new InvocationTarget(bean, method, dataType));
        if (previous != null) {
            log.warn("WebSocket监听方法重复注册，已覆盖旧注册 [type={}, cliKey={}, oldMethod={}, newMethod={}]",
                    type, cliKey, previous.method(), method);
        }
    }

    @Override
    public Optional<Object> invoke(String type, String cliKey, WebSocketRequest<?> request) {
        Map<String, InvocationTarget> cliKeyMap = listenerMap.get(type);
        if (ObjectUtil.isEmpty(cliKeyMap)) {
            log.warn("未找到WebSocket监听方法，消息被丢弃 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey));
            return Optional.empty();
        }

        // 先精确匹配 cliKey，支持为特定客户端注册专属处理器
        InvocationTarget target = cliKey == null ? null : cliKeyMap.get(cliKey);

        // 精确匹配不到则回退到通配处理器，支持中央处理器模式
        if (ObjectUtil.isEmpty(target)) {
            target = cliKeyMap.get(WebSocketConstant.DEFAULT_CLI_KEY);
        }
        if (ObjectUtil.isEmpty(target)) {
            log.warn("未找到WebSocket监听方法，消息被丢弃 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey));
            return Optional.empty();
        }

        Object typedData = convertData(request.getData(), target.dataType());
        WebSocketRequest<Object> typedRequest = new WebSocketRequest<>();
        typedRequest.setData(typedData);

        // 将通道上下文与请求标识完整传递给新构建的 typedRequest，业务方法中 reply() 可用；
        // requestId 透传保证同步回复降级为普通分发时业务仍可见原请求ID
        typedRequest.setType(request.getType());
        typedRequest.setCliKey(request.getCliKey());
        typedRequest.setRequestId(request.getRequestId());

        try {
            Object invoke = target.method().invoke(target.bean(), typedRequest);

            // void 监听方法反射调用返回 null，用 ofNullable 承载，避免 Optional.of 抛出 NPE
            return Optional.ofNullable(invoke);
        } catch (InvocationTargetException e) {

            // 解包反射包装：向上抛业务原始异常，调用方拿到真实失败信息而非 InvocationTargetException 包装
            Throwable cause = e.getTargetException();
            log.error("WebSocket监听方法执行异常 [type={}, cliKey={}, method={}]",
                    type, WebSocketUtils.maskKey(cliKey), target.method().getName(), cause);
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        } catch (IllegalAccessException e) {
            log.error("WebSocket监听方法调用失败 [type={}, cliKey={}, method={}]",
                    type, WebSocketUtils.maskKey(cliKey), target.method().getName(), e);
            throw new IllegalStateException("监听方法不可访问: " + target.method(), e);
        }
    }

    /**
     * 将原始数据转换为目标类型
     */
    private Object convertData(Object rawData, Class<?> targetType) {
        if (rawData == null || targetType == null || targetType == Object.class) {
            return rawData;
        }
        if (targetType.isInstance(rawData)) {
            return rawData;
        }
        return JSON.toJavaObject(rawData, targetType);
    }
}
