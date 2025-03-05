package com.simple.common.excel.common.function;

import org.apache.poi.ss.usermodel.Row;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@FunctionalInterface
public interface PoiExportFunction<T> {
    void execute(Row row, T entity);
}
