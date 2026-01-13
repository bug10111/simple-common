package com.simple.common.excel.common.function;

import org.apache.poi.ss.usermodel.Row;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@FunctionalInterface
public interface PoiExportFunction<T> {
    void execute(Row row, T entity);
}
