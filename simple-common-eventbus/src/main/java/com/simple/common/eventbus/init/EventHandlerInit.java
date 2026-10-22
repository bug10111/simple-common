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
import java.util.HashSet;
import java.util.Set;
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
        // 获取所有 Bean 名称，并收集每个 Bean 的原始方法。
        // 遍历范围说明：USER_DECLARED_METHODS 只遍历类本身与父类声明的非桥接方法，
        // 不会遍历接口方法，@EventHandler 仅声明在接口方法上时不会被扫描（需在实现类上标注）。
        // 仅处理单例 bean，避免就绪阶段提前实例化 request/session 等 Web 作用域 bean。
        Stream.of(applicationContext.getBeanDefinitionNames())
              .filter(applicationContext::isSingleton)
              .forEach(beanName -> {
                  // 单个 bean 的扫描失败（FactoryBean 占位、prototype 副作用、懒加载创建抛错等）只记录日志，
                  // 不允许中断其余 @EventHandler bean 的注册导致事件总线部分静默失效
                  try {
                      Object bean = applicationContext.getBean(beanName);
                      Class<?> targetClass = AopUtils.getTargetClass(bean);
                      // 同名同参的覆写方法会以子类/父类两个 Method 对象出现，按签名去重防止重复注册重复分发
                      Set<String> seenSignatures = new HashSet<>();
                      ReflectionUtils.doWithMethods(targetClass, method -> {
                          if (method.isAnnotationPresent(EventHandler.class)
                                  && seenSignatures.add(methodSignature(method))) {
                              // 解析在实际 bean 实例上可调用的方法（兼容 JDK 动态代理与 AOP 代理场景），
                              // 保证 MethodHandle 绑定的接收者与方法声明类型匹配，并让代理增强不被绕过
                              Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
                              eventHandlerManager.register(beanName, invocableMethod);
                          }
                      }, ReflectionUtils.USER_DECLARED_METHODS);
                  } catch (Exception e) {
                      log.warn("扫描事件处理器失败, beanName: {}", beanName, e);
                  }
              });
        log.info("领域事件EventHandler初始化完成");
    }

    /**
     * 方法签名：用于跨类层级去重覆写的同签名 @EventHandler 方法
     */
    private String methodSignature(Method method) {
        StringBuilder signature = new StringBuilder(method.getName());
        for (Class<?> parameterType : method.getParameterTypes()) {
            signature.append(':').append(parameterType.getName());
        }
        return signature.toString();
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Event.getOrder();
    }
}
