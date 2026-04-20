package com.simple.common.core.common.service.process;

import com.simple.common.core.common.enums.process.DefaultKindProcess;
import org.springframework.core.Ordered;

/**
 * 责任链处理服务基础接口。
 * <p>
 * 所有责任链处理器的标记接口，用于统一责任链模式下的处理器类型。
 * 具体的处理器接口应继承此接口，如 {@code AuthProcess}、{@code LoginErrorProcess} 等。
 * </p>
 *
 * <h3>责任链模式说明：</h3>
 * <p>
 * 责任链模式用于将多个处理步骤串联执行，每个处理器可以决定是否继续执行后续处理器。
 * 在本框架中，责任链通过枚举类实现 {@link com.simple.common.core.common.enums.process.DefaultKindProcess}
 * 接口来定义处理器的执行顺序。
 * </p>
 *
 * <h3>实现步骤：</h3>
 * <ol>
 *   <li>定义处理器接口继承 {@code BasProcessService}</li>
 *   <li>创建枚举类实现 {@code DefaultKindProcess} 接口，定义处理器顺序</li>
 *   <li>实现具体的处理器类</li>
 *   <li>通过枚举的 order 字段控制执行顺序</li>
 * </ol>
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