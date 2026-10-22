package com.simple.common.excel.common.handler;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
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
     * 每隔 batchCount 条存储数据库(构造时必须传入正数),达到阈值后清理 list，方便内存回收
     */
    private final int batchCount;

    /**
     * 缓存的数据
     */
    private List<T> cachedDataList;

    public DefaultEasyExcelReadHandler(int batchCount) {
        if (batchCount <= 0) {
            throw new IllegalArgumentException("batchCount(每批处理条数)必须大于 0，当前值：" + batchCount);
        }
        this.batchCount = batchCount;
        this.cachedDataList = ListUtils.newArrayListWithExpectedSize(batchCount);
    }

    /**
     * 每一条数据解析完后调用
     */
    @Override
    public void invoke(T t, AnalysisContext analysisContext) {
        if (log.isDebugEnabled()) {
            log.debug("解析到一条数据:{}", JsonUtils.toJsonStr(t));
        }
        cachedDataList.add(t);

        // 达到batchCount了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (cachedDataList.size() >= batchCount) {
            saveData(cachedDataList);

            // 存储完成清理 list，同时避免同一监听实例复用时旧数据重复持久化
            cachedDataList = ListUtils.newArrayListWithExpectedSize(batchCount);
        }
    }

    /**
     * 所有数据解析完了调用
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

        // 存储剩余不足一批的数据(空批次跳过，避免空列表被下游 saveBatch 拒绝)，存储后清理缓冲
        if (!cachedDataList.isEmpty()) {
            saveData(cachedDataList);
            cachedDataList = ListUtils.newArrayListWithExpectedSize(batchCount);
        }
        log.info("所有数据解析完成！");
    }

    /**
     * 在转换异常 获取其他异常下会调用本接口。
     * 处理契约：单元格数据转换异常(ExcelDataConvertException)记录行列号与原始数据后跳过该行继续解析；
     * 其他异常属于未知错误，原样抛出终止读取，避免静默吞掉真实故障。
     * 注意：终止读取时已缓冲的行不会触发 saveData。
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        if (exception instanceof ExcelDataConvertException excelDataConvertException) {
            // 如果是某一个单元格的转换异常 能获取到具体行号
            // 如果要获取头的信息 配合invokeHeadMap使用
            log.error("第{}行，第{}列数据转换失败，已跳过该行继续解析，数据为:{}",
                      excelDataConvertException.getRowIndex(), excelDataConvertException.getColumnIndex(),
                      excelDataConvertException.getCellData(), exception);
            return;
        }

        // 保留完整堆栈后再抛出，终止读取
        log.error("解析失败，终止本次读取", exception);
        throw exception;
    }

    /**
     * 这里会一行行的返回头
     */
    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {

        if (log.isDebugEnabled()) {
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
