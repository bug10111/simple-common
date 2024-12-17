package com.simple.common.core.common.service.process;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import org.springframework.core.Ordered;

/**
 * Created with IntelliJ IDEA on 2023/12/04/17:24.
 * <p>
 * 定义流程基类接口
 *
 * @author 兄台丶请冷静
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
