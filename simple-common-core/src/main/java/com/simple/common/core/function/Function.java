package com.simple.common.core.function;

/**
 * Created by IntelliJ IDEA
 * Description: 函数接口公共标记接口（纯标记，无任何语义/方法）。
 * 用于统一 {@link AiFunction}、{@code DefaultFunction}、{@link HttpRecordFunction}、
 * {@code ReturnValueFunction} 等函数式接口的类型归属，便于在泛型/工具方法中统一识别。
 * 注意：与 java.util.function.Function 同名，混用场景需全限定名。
 *
 * @author qty
 */
public interface Function {

}
