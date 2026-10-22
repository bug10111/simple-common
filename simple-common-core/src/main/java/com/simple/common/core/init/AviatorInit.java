package com.simple.common.core.init;

import com.googlecode.aviator.AviatorEvaluator;
import com.simple.common.core.common.enums.order.SimpleOrder;
import com.simple.common.core.common.service.aviator.DefAviatorFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: Aviator初始化自定义计算规则
 *
 * @author qty
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "simple.aviator", name = "open", havingValue = "true")
public class AviatorInit implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    /**
     * 自定义函数集合：允许为空——开启表达式引擎但未定义自定义函数是合法形态，不应阻断启动
     */
    @Autowired(required = false)
    private List<DefAviatorFunction> list;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (list == null || list.isEmpty()) {
            log.info("Aviator 未发现自定义函数，跳过初始化");
            return;
        }
        // 单函数注册失败只告警不中断，避免一个坏函数导致整个应用启动失败
        for (DefAviatorFunction function : list) {
            try {
                AviatorEvaluator.addFunction(function);
            } catch (Exception e) {
                log.error("Aviator 自定义函数注册失败: {}", function.getName(), e);
            }
        }
        log.info("Aviator 初始化完成，注册自定义函数 {} 个", list.size());
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Aviator.getOrder();
    }
}
