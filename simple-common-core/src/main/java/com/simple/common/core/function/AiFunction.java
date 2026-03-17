package com.simple.common.core.function;

/**
 * Created with IntelliJ IDEA
 * Description: ai函数接口
 *
 * @author qty
 */
@FunctionalInterface
public interface AiFunction extends Function {

    void handler(String msg);

}
