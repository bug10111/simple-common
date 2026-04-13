package com.simple.common.rabbitmq.manager;

import com.simple.common.rabbitmq.common.manager.CompletionVerificationRouterManager;
import com.simple.common.rabbitmq.common.manager.CompletionVerificationStrategyManager;
import jakarta.annotation.PostConstruct;
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
                    strategyMap.put(queueName, strategy);
                }
            }
        }
    }

    @Override
    public CompletionVerificationStrategyManager getStrategy(String queueName) {
        return strategyMap.get(queueName);
    }
}