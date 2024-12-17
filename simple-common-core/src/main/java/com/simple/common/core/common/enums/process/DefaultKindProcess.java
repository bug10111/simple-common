package com.simple.common.core.common.enums.process;

/**
 * Created with IntelliJ IDEA on 2023/12/04/18:05.
 * 流程定义接口
 *
 * @author 兄台丶请冷静
 */
public interface DefaultKindProcess {

    /**
     * 流程是否执行
     */
    boolean isExecute();

    /**
     * 流程执行顺序
     */
    Integer getOrdered();

    /**
     * 说明
     */
    String getMsg();

}
