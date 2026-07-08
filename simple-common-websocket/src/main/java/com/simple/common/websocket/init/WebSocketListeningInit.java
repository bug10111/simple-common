package com.simple.common.websocket.init;

import com.simple.common.websocket.common.annotation.WebSocketListening;
import com.simple.common.websocket.common.entity.WebSocketRequest;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * WebSocket监听器初始化器
 *
 * @author qty
 */
@Slf4j
@Component
public class WebSocketListeningInit implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private WebSocketListeningManager manager;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String[] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = context.getBean(beanName);
            for (Method method : bean.getClass().getDeclaredMethods()) {
                WebSocketListening annotation = method.getAnnotation(WebSocketListening.class);
                if (annotation != null) {
                    Class<?> dataType = extractDataType(method);
                    validateMethod(method);
                    manager.registerMethod(annotation.type(), annotation.cliKey(), bean, method, dataType);
                    log.debug("注册WebSocket监听方法: {}.{} -> type={}, cliKey={}, dataType={}",
                            bean.getClass().getSimpleName(), method.getName(), annotation.type(), annotation.cliKey(), dataType.getSimpleName());
                }
            }
        }
    }

    /**
     * 从方法签名中提取 WebSocketRequest 的泛型类型
     */
    private Class<?> extractDataType(Method method) {
        Type[] genericParamTypes = method.getGenericParameterTypes();
        if (genericParamTypes.length == 1 && genericParamTypes[0] instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericParamTypes[0];
            Type[] actualTypeArgs = paramType.getActualTypeArguments();
            if (actualTypeArgs.length > 0 && actualTypeArgs[0] instanceof Class) {
                return (Class<?>) actualTypeArgs[0];
            }
        }
        return Object.class;
    }

    /**
     * 校验监听方法签名
     * 要求：参数为 WebSocketRequest 或 WebSocketRequest&lt;T&gt;
     */
    private void validateMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1 || !WebSocketRequest.class.isAssignableFrom(paramTypes[0])) {
            throw new IllegalArgumentException(
                    String.format("方法 %s 参数必须为 (WebSocketRequest request) 或其泛型子类型，当前参数: %s",
                            method.getName(), java.util.Arrays.toString(paramTypes)));
        }
    }
}