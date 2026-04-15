package com.simple.common.rabbitmq.manager;

import com.simple.common.rabbitmq.common.manager.CompletionVerificationRouterManager;
import com.simple.common.rabbitmq.common.manager.CompletionVerificationStrategyManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的路由管理器，自动收集所有实现了 CompletionVerificationStrategy 的 Spring Bean，
 * 并根据 getQueueName() 建立映射。
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultCompletionVerificationRouterManager implements CompletionVerificationRouterManager {

    @Autowired(required = false)
    private List<CompletionVerificationStrategyManager> strategies;

    private final Map<String, CompletionVerificationStrategyManager> strategyMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (strategies != null) {
            for (CompletionVerificationStrategyManager strategy : strategies) {
                String queueName = strategy.getQueueName();
                if (queueName != null) {
                    // 修复：检测重复队列名并警告
                    if (strategyMap.containsKey(queueName)) {
                        log.warn("队列[{}]已有校验策略[{}]，新策略[{}]将覆盖原策略",
                                queueName,
                                strategyMap.get(queueName).getClass().getSimpleName(),
                                strategy.getClass().getSimpleName());
                    }
                    strategyMap.put(queueName, strategy);
                }
            }
        }
        log.info("完成校验策略路由器初始化完成，注册策略数量：{}", strategyMap.size());
    }

    @Override
    public CompletionVerificationStrategyManager getStrategy(String queueName) {
        return strategyMap.get(queueName);
    }
}