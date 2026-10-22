package com.simple.common.websocket.common.entity;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * WebSocket监听方法的调用目标（分发器的存储值，按 type + cliKey 寻址）。
 * <p>
 * 注意：本类型是 {@code DefaultWebSocketListeningManager} 中 listenerMap 的
 * <b>值</b>而非键——record 自动生成的 equals/hashCode 含 bean 实例与 Method 身份，
 * 不适合作为 Map 键使用。
 * </p>
 *
 * @param bean     目标 Bean 实例（不可为 null）
 * @param method   监听方法（不可为 null；构造时开放可访问性，含非 public 方法）
 * @param dataType 请求数据目标类型（不可为 null；无法解析泛型时为 Object.class，表示不做转换）
 * @author qty
 */
public record InvocationTarget(Object bean, Method method, Class<?> dataType) {

    public InvocationTarget {
        Objects.requireNonNull(bean, "bean 不能为空");
        Objects.requireNonNull(method, "method 不能为空");
        Objects.requireNonNull(dataType, "dataType 不能为空");
        try {
            method.setAccessible(true);
        } catch (Exception e) {

            // JPMS 模块封闭/安全管理器场景下 setAccessible 可能抛 InaccessibleObjectException 等运行时异常，
            // 在注册阶段以明确信息快速失败，而不是分发阶段以晦涩堆栈暴露
            throw new IllegalStateException("无法开放监听方法的反射访问: " + method, e);
        }
    }
}
