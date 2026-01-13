package com.simple.common.core.init;

import com.googlecode.aviator.AviatorEvaluator;
import com.simple.common.core.common.enums.order.SimpleOrder;
import com.simple.common.core.common.service.aviator.DefAviatorFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${simple.aviator.open}")
    private String aviator;

    @Autowired
    private List<DefAviatorFunction> list;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        list.forEach(AviatorEvaluator::addFunction);
        log.info("Aviator 初始化完成");
    }

    @Override
    public int getOrder() {
        return SimpleOrder.Aviator.getOrder();
    }
}
