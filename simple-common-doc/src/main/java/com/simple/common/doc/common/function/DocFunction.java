package com.simple.common.doc.common.function;

/**
 * Created with IntelliJ IDEA
 * Description: doc有参数的函数
 *
 * @author qty
 */
@FunctionalInterface
public interface DocFunction<T> {

    /**
     * 由单条业务数据创建表格行内容
     *
     * @param t 单条业务数据
     * @return 表格行单元格内容,按列顺序排列
     */
    String[] createRow(T t);
}
