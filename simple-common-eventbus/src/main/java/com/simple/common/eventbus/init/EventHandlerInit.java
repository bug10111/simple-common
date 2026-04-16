package com.simple.common.eventbus.init;

import com.simple.common.core.common.enums.order.SimpleOrder;
import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.eventbus.common.manager.EventHandlerManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Component
public class EventHandlerInit implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EventHandlerManager eventHandlerManager;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 获取所有 Bean 名称，并收集每个 Bean 的原始方法
        Stream.of(applicationContext.getBeanDefinitionNames())
              .forEach(beanName -> {
                  Object bean = applicationContext.getBean(beanName);
                  // 【修复】使用 ReflectionUtils.doWithMethods 遍历类及接口的所有方法，避免 JDK 动态代理遗漏方法
                  Class<?> targetClass = AopUtils.getTargetClass(bean);
                  ReflectionUtils.doWithMethods(targetClass, method -> {
                      if (method.isAnnotationPresent(EventHandler.class)) {
                          eventHandlerManager.register(beanName, method);
                      }
                  }, ReflectionUtils.USER_DECLARED_METHODS);
              });
        log.info("领域事件EventHandler初始化完成");
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Event.getOrder();
    }
}