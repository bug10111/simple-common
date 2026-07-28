package com.simple.common.doc.common.function;

/**
 * Created with IntelliJ IDEA
 * Description: doc有参数的函数
 *
 * @author qty
 */
@FunctionalInterface
public interface DocFunction<T> {

    String[] createRow(T t) ;

}
