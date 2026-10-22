package com.simple.common.cache.common.function;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * Description: 获取redis数据，并提供缓存回写与空值处理能力
 * <p>
 * 注意：实现 {@link #getNullable()} 时，必须返回一个非 null 的占位对象（如 NullObject），
 * 用于防止缓存穿透。若返回 null，会导致缓存空值失效；{@link #set(Object)} 会对该错误快速失败。
 *
 * @author qty
 */
public interface GetRedisFunction<T, R> {

    /**
     * 获取数据
     *
     * @param request 请求参数
     * @return 缓存中的数据，缓存未命中时返回 null
     */
    T get(R request);

    /**
     * 保存数据（底层写入钩子）
     * <p>
     * 低层 SPI：仅供框架内部的 {@link #set(Object)} 调用，业务实现不应在外部直接调用该方法，
     * 否则会绕过 {@link #set(Object)} 中的空值占位与快速失败逻辑，破坏防穿透契约。
     *
     * @param value 需要保存的值
     */
    void setHandler(T value);

    /**
     * 保存数据，含空值处理
     * <p>
     * value 非 null 时直接保存；为 null 时保存 {@link #getNullable()} 返回的空值占位对象
     * （占位对象必须非 null，为 null 时快速失败抛出 NPE，防止静默丢失防穿透语义）。
     *
     * @param value 需要保存的值
     */
    default void set(T value) {
        if (value != null) {
            setHandler(value);
        } else {
            setHandler(Objects.requireNonNull(getNullable(),
                    "getNullable() 必须返回非 null 的空值占位对象"));
        }
    }

    /**
     * 判断读取到的值是否为 {@link #getNullable()} 返回的空值占位对象。
     * <p>
     * 缓存命中空值占位对象时，调用方应按“数据不存在”处理，而不是把占位对象当作真实数据透出。
     * 默认实现基于 equals 比较，实现类使用单例占位对象时无需覆写。
     *
     * @param value 从缓存读取的值
     * @return true 表示该值代表“数据库无此数据”的空值占位
     */
    default boolean isNullValue(T value) {
        return value != null && value.equals(getNullable());
    }

    /**
     * 获取空值占位对象
     * <p>
     * 该方法必须返回一个非 null 的标记对象，用于标识“数据库无此数据”的状态，
     * 避免缓存穿透。典型实现为返回一个特定的单例对象（如 NullObject）。
     *
     * @return 非 null 的空值占位对象
     */
    T getNullable();
}
