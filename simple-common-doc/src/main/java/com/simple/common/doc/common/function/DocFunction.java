package com.simple.common.doc.common.function;

import com.deepoove.poi.data.Tables;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.xwpf.usermodel.XWPFTable;

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
