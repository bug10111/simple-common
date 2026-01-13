package com.simple.common.excel.common.handler;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: easyexcel 读取excel默认实现
 *
 * @author qty
 */
@Slf4j
public abstract class DefaultEasyExcelReadHandler<T> implements ReadListener<T> {

    /**
     * 每隔5条存储数据库，实际使用中可以2000条，然后清理list ，方便内存回收
     */
    private int BATCH_COUNT = 2000;

    /**
     * 缓存的数据
     */
    private List<T> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    public DefaultEasyExcelReadHandler(int batchCount) {
        this.BATCH_COUNT = batchCount;
    }

    /**
     * 每一条数据解析完后调用
     */
    @Override
    public void invoke(T t, AnalysisContext analysisContext) {
        if(log.isDebugEnabled()){
            log.debug("解析到一条数据:{}", JsonUtils.toJsonStr(t));
        }
        cachedDataList.add(t);

        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData(cachedDataList);

            // 存储完成清理 list
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }

    /**
     * 所有数据解析完了调用
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

        //再次保存
        saveData(cachedDataList);
        log.info("所有数据解析完成！");
    }

    /**
     * 在转换异常 获取其他异常下会调用本接口。抛出异常则停止读取。如果这里不抛出异常则 继续读取下一行。
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) {
        log.error("解析失败，但是继续解析下一行:{}", exception.getMessage());
        // 如果是某一个单元格的转换异常 能获取到具体行号
        // 如果要获取头的信息 配合invokeHeadMap使用
        if (exception instanceof ExcelDataConvertException excelDataConvertException) {
            AssertUtils.errorParams("第{}行，第{}列解析异常，数据为:{}", excelDataConvertException.getRowIndex(), excelDataConvertException.getColumnIndex(),
                                    excelDataConvertException.getCellData());
        }
    }

    /**
     * 这里会一行行的返回头
     */
    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {

        if(log.isDebugEnabled()){
            log.debug("读取到excel头[{}]", JsonUtils.toJsonStr(headMap));
        }
        // 如果想转成成 Map<Integer,String> 使用 ConverterUtils.convertToStringMap(headMap, context)
    }

    /**
     * 执行保存
     *
     * @param cachedDataList 读取到的数据
     */
    protected abstract void saveData(List<T> cachedDataList);
}
