package com.simple.common.websocket.init;

import com.simple.common.websocket.common.annotation.WebSocketListening;
import com.simple.common.websocket.common.entity.WebSocketRequest;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * WebSocket监听器初始化器
 * <p>
 * 扫描全部 Bean 中标注 {@link WebSocketListening} 的公开方法并注册到监听管理器。
 * 单个 Bean 实例化或单个方法校验失败只跳过并记录日志，不中断整体扫描；
 * 扫描基于代理目标类并包含父类声明的方法（AOP 代理 / 抽象基类场景不漏注册）。
 * </p>
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
            Object bean;
            try {
                bean = context.getBean(beanName);
            } catch (Exception e) {

                // 懒加载/request/session 等非单例作用域的 Bean 在启动阶段无法安全实例化：
                // 跳过并告警，而不是让单个 Bean 失败拖垮整个应用启动
                log.warn("跳过无法实例化的Bean，其中的@WebSocketListening不会注册 [beanName={}]: {}", beanName, e.getMessage());
                continue;
            }
            registerListenerMethods(bean);
        }
    }

    /**
     * 扫描单个 Bean 上的监听方法。
     * <p>
     * 经 {@link AopUtils#getTargetClass} 取代理目标类（JDK 动态代理下 bean.getClass()
     * 看不到目标方法），用 {@link ReflectionUtils#getUniqueDeclaredMethods} 做包含父类的全量扫描；
     * 单个方法校验失败仅记录错误并跳过该方法，不中断其余 Bean 的注册。
     * </p>
     */
    private void registerListenerMethods(Object bean) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        for (Method method : ReflectionUtils.getUniqueDeclaredMethods(targetClass)) {
            WebSocketListening annotation = method.getAnnotation(WebSocketListening.class);
            if (annotation == null) {
                continue;
            }
            try {
                validateMethod(method);
                Class<?> dataType = extractDataType(method);
                manager.registerMethod(annotation.type(), annotation.cliKey(), bean, method, dataType);
                log.info("注册WebSocket监听方法: {}.{} -> type={}, cliKey={}, dataType={}",
                        targetClass.getSimpleName(), method.getName(), annotation.type(), annotation.cliKey(), dataType.getSimpleName());
            } catch (Exception e) {

                // 单个方法签名/可见性错误只影响该方法，降级为日志，保障其余监听器正常注册
                log.error("WebSocket监听方法注册失败，已跳过 [bean={}, method={}]: {}",
                        targetClass.getSimpleName(), method.getName(), e.getMessage());
            }
        }
    }

    /**
     * 从方法签名中提取 WebSocketRequest 的泛型实参类型。
     * <p>
     * 仅当参数为 {@code WebSocketRequest<X>}（raw type 匹配且实参为 Class）时返回 X；
     * 嵌套泛型/通配符/类型变量等无法解析时回退 Object.class 并告警——
     * 该场景下分发不做类型转换，业务方法收到的 data 类型与签名声明不符，需留意排障。
     * </p>
     */
    private Class<?> extractDataType(Method method) {
        Type[] genericParamTypes = method.getGenericParameterTypes();
        if (genericParamTypes.length == 1 && genericParamTypes[0] instanceof ParameterizedType paramType
                && WebSocketRequest.class.equals(paramType.getRawType())) {
            Type[] actualTypeArgs = paramType.getActualTypeArguments();
            if (actualTypeArgs.length > 0 && actualTypeArgs[0] instanceof Class<?> clazz) {
                return clazz;
            }
            log.warn("无法解析WebSocketRequest的泛型实参（嵌套泛型/通配符/类型变量），按Object处理，分发时不做类型转换 [method={}]", method.getName());
        }
        return Object.class;
    }

    /**
     * 校验监听方法签名与可见性。
     * 要求：参数为 WebSocketRequest 或其子类型，且方法必须为 public——
     * 分发器直接 method.invoke 调用，非 public 方法会在运行期抛 IllegalAccessException。
     */
    private void validateMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1 || !WebSocketRequest.class.isAssignableFrom(paramTypes[0])) {
            throw new IllegalArgumentException(
                    String.format("方法 %s 参数必须为 (WebSocketRequest request) 或其泛型子类型，当前参数: %s",
                            method.getName(), java.util.Arrays.toString(paramTypes)));
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException(
                    String.format("监听方法 %s 必须声明为 public，否则分发期 method.invoke 将抛出 IllegalAccessException",
                            method.getName()));
        }
    }
}
