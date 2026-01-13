package com.simple.common.core.common.service.process;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import org.springframework.core.Ordered;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 定义流程基类接口
 *
 * @author qty
 */
public interface BasProcessService extends Ordered {

    /**
     * 获取流程
     */
    DefaultKindProcess getProcess();

    default int getOrder() {
        return getProcess().getOrdered();
    }

}
