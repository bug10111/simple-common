package com.simple.common.core.function;

/**
 * Created with IntelliJ IDEA
 * Description: ai函数接口（AI调用点尚未接入，保留为扩展点；签名与同包函数接口对齐，允许lambda抛受检异常）
 *
 * @author qty
 */
@FunctionalInterface
public interface AiFunction extends Function {

    /**
     * 处理AI输入
     *
     * @param msg 输入内容
     * @throws Throwable 调用方可抛出受检异常，由触发方统一处理（与DefaultFunction/HttpRecordFunction口径一致）
     */
    void handler(String msg) throws Throwable;

}
