package com.simple.common.core.common.enums.process;

/**
 * Created with IntelliJ IDEA
 * 流程定义接口
 *
 * @author qty
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
