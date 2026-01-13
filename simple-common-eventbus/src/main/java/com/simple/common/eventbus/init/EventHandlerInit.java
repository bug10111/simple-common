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
        Stream.of(applicationContext.getBeanDefinitionNames())
              .map(bName -> applicationContext.getBean(bName))
              .map(AopUtils::getTargetClass)
              .flatMap(c -> Stream.of(c.getMethods()))
              .filter(m -> m.isAnnotationPresent(EventHandler.class))
              .forEach(m -> eventHandlerManager.register(m));
        log.info("领域事件EventHandler初始化完成");
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Event.getOrder();
    }
}