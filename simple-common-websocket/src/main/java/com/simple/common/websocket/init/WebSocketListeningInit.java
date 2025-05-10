package com.simple.common.websocket.init;

import com.simple.common.websocket.common.annotation.WebSocketListening;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Optional;

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
                }
            }
        }
    }

    /**
     * 校验监听方法
     */
    private void validateMethod(Method method) {
        if (method.getParameterCount() != 1 || !method.getParameterTypes()[0].equals(String.class)) {
            throw new IllegalArgumentException("方法 " + method.getName() + " 参数必须为 (String msg)");
        }
        if (!method.getReturnType().equals(String.class)) {
            throw new IllegalArgumentException("方法 " + method.getName() + " 返回值必须为 String");
        }
    }
}
