package com.simple.common.rabbitmq.manager;

import com.simple.common.rabbitmq.common.manager.CompletionVerificationRouterManager;
import com.simple.common.rabbitmq.common.manager.CompletionVerificationStrategyManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的路由管理器，自动收集所有实现了 CompletionVerificationStrategy 的 Spring Bean，
 * 并按 getQueueName() 建立映射。
 * <p>
 * 注册规则：
 * 1) 队列名做 trim 归一化后注册，保证与消费队列名的精确匹配；
 * 2) 空白队列名的策略视为配置错误，启动阶段告警并跳过；
 * 3) 重复队列名属于配置错误——因注入 List 顺序不确定，静默覆盖会把消息路由到不确定的策略，
 *    启动阶段直接失败（fail-fast）。
 * </p>
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
                if (StringUtils.hasText(queueName)) {
                    queueName = queueName.trim();
                    // 重复队列名属于配置错误：注入顺序不定，覆盖会导致校验被路由到不确定策略，启动直接失败
                    if (strategyMap.containsKey(queueName)) {
                        throw new IllegalStateException("队列[" + queueName + "]存在重复的完成校验策略："
                                + strategyMap.get(queueName).getClass().getName()
                                + " 与 " + strategy.getClass().getName());
                    }
                    strategyMap.put(queueName, strategy);
                } else {
                    log.warn("校验策略[{}]未返回有效的队列名，已跳过注册", strategy.getClass().getName());
                }
            }
        }
        log.info("完成校验策略路由器初始化完成，注册策略数量：{}", strategyMap.size());
    }

    @Override
    @Nullable
    public CompletionVerificationStrategyManager getStrategy(String queueName) {
        // ConcurrentHashMap 不允许 null Key，空白/非法输入显式拒绝并告警，避免调用侧拿到无诊断信息的 NPE
        if (!StringUtils.hasText(queueName)) {
            log.warn("查询完成校验策略时队列名为空，返回 null（按无需校验处理）");
            return null;
        }
        String key = queueName.trim();
        CompletionVerificationStrategyManager strategy = strategyMap.get(key);
        if (strategy == null) {
            // 未注册策略是常见误配置（队列名不一致/漏配），必须留痕便于排障
            log.info("队列[{}]未注册完成校验策略，按无需校验处理", key);
        }
        return strategy;
    }
}
