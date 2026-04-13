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

/**
 * Created with IntelliJ IDEA
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
        // 扫描所有 Spring Bean 中的监听方法
        String[] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = context.getBean(beanName);
            for (Method method : bean.getClass().getDeclaredMethods()) {
                WebSocketListening annotation = method.getAnnotation(WebSocketListening.class);
                if (annotation != null) {
                    validateMethod(method);
                    manager.registerMethod(annotation.type(), annotation.cliKey(), bean, method);
                    log.debug("注册WebSocket监听方法: {}.{} -> type={}, cliKey={}", 
                            bean.getClass().getSimpleName(), method.getName(), annotation.type(), annotation.cliKey());
                }
            }
        }
    }

    /**
     * 校验监听方法签名
     * 要求：参数为 WebSocketRequest，返回值为 String（或可转为String的任意类型）
     */
    private void validateMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1 || !paramTypes[0].equals(WebSocketRequest.class)) {
            throw new IllegalArgumentException(
                    String.format("方法 %s 参数必须为 (WebSocketRequest request)，当前参数: %s", 
                            method.getName(), java.util.Arrays.toString(paramTypes)));
        }
    }
}