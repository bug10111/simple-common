package com.simple.common.eventbus.common.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * 事件处理器值对象（运行期注册元数据，不参与任何序列化/反序列化）
 * <p>本类型仅存活于事件管理器的内存注册表中，不落地 JSON；
 * equals/hashCode/toString 有意省略：{@code methodHandle} 类成员不稳定，
 * 参与哈希会在存入监听映射后因字段变更产生悬挂键，调试请依赖 {@link #getMethodForDebug()}。</p>
 *
 * @author qty
 */
@Getter
@Setter
@Accessors(chain = true)
public class EventHandlerValue {

    /**
     * 事件参数类型
     */
    private Class<?> aClass;

    /**
     * 高效方法调用句柄（Java 17 优化，替代反射 Method.invoke）
     */
    private MethodHandle methodHandle;

    /**
     * 注册时所属的 bean 名称（仅保存名称字符串，不持有 bean 强引用，
     * 避免注册表长期 pin 住 prototype/临时 bean 导致其无法回收；用于按 bean 精确注销）
     */
    private String beanName;

    /**
     * 保留原始的反射 Method 对象，用于调试、日志输出或注销时匹配
     */
    private Method methodForDebug;
}
