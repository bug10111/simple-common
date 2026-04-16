package com.simple.common.eventbus.common.service;

import cn.hutool.core.util.StrUtil;
import com.simple.common.core.common.service.cycle.AbsCycleService;
import com.simple.common.eventbus.common.event.CycleEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.aop.support.AopUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于事件的计划任务抽象类
 *
 * @param <T> 任务参数类型
 * @author qty
 */
@Slf4j
public abstract class AbsEventCycleService<T> extends AbsCycleService<T> implements BeanNameAware {

    @Autowired
    private EventBusService eventBusService;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 当前 Bean 在 Spring 容器中的名称（通过 BeanNameAware 自动注入）
     */
    private String beanName;

    public AbsEventCycleService(Class<T> eventClass) {
        super(eventClass);
    }

    /**
     * 启动循环任务
     *
     * @param runBody      任务参数
     * @param sum          总执行次数
     * @param timeInterval 周期时间间隔
     * @param isAccumulate 是否累加延迟
     * @param parameters   扩展参数
     */
    @Override
    public void run(T runBody, Integer sum, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters) {
        if (runBody == null) {
            log.error("任务参数不能为空");
            return;
        }

        if (sum == null || sum <= 0) {
            sum = 1; // 默认执行1次
        }

        if (timeInterval == null || timeInterval < 0) {
            timeInterval = 0; // 默认不延迟
        }

        if (isAccumulate == null) {
            isAccumulate = false; // 默认不累加
        }

        CycleEvent event = new CycleEvent();
        event.setRunBody(runBody);
        event.setSum(sum);
        event.setNum(0);
        event.setTimeInterval(timeInterval);
        event.setIsAccumulate(isAccumulate);
        event.setReserve(parameters);
        execution(event);
    }

    /**
     * 执行单次任务调度
     *
     * @param event 循环事件对象
     */
    public void execution(CycleEvent event) {
        if (event == null || event.getRunBody() == null) {
            log.error("循环事件对象或任务参数为空");
            return;
        }

        T runBody;
        try {
            runBody = toBean(event.getRunBody());
        } catch (Exception e) {
            log.error("转换任务参数失败", e);
            error(null, event.getReserve());
            return;
        }

        try {
            boolean success = handler(runBody, event.getReserve());

            if (!success) {
                handleFailure(event, runBody);
            } else {
                // 执行成功，回调成功处理
                ok(runBody, event.getReserve());
            }
        } catch (Throwable e) {
            log.error("调度异常", e);
            // 执行异常时，若还有重试次数则尝试失败处理（发送延迟事件），否则直接触发 error 回调
            if (event.getNum() < event.getSum()) {
                handleFailure(event, runBody);
            } else {
                error(runBody, event.getReserve());
            }
        }
    }

    /**
     * 处理执行失败的情况
     * <p>修复：彻底移除递归调用，当发送延迟消息连续失败时，触发 more 回调告知任务终止</p>
     */
    private void handleFailure(CycleEvent event, T runBody) {
        int nextNum = event.getNum() + 1;
        if (nextNum <= event.getSum()) {
            // 计算延迟时间
            int delay = event.getIsAccumulate() ? event.getTimeInterval() * nextNum : event.getTimeInterval();
            event.setNum(nextNum);
            addCounter(runBody, event.getReserve(), nextNum);

            // 获取当前 Bean 名称（优先使用 BeanNameAware 注入的，其次动态查找）
            String currentBeanName = getCurrentBeanName();
            if (currentBeanName == null) {
                log.error("无法获取当前 Bean 名称，循环任务终止");
                more(runBody, event.getReserve());
                return;
            }
            event.setBeanName(currentBeanName);

            // 发送延迟事件，若发送失败则进行有限次重试（最多3次）
            int maxSendRetry = 3;
            for (int retryCount = 0; retryCount < maxSendRetry; retryCount++) {
                try {
                    eventBusService.push(event, delay, TimeUnit.SECONDS);
                    break;
                } catch (AmqpException e) {
                    log.error("发送延迟事件失败，第{}次重试", retryCount + 1, e);
                    if (retryCount == maxSendRetry - 1) {
                        // 发送彻底失败，任务无法继续，触发 more 回调告知业务方
                        log.error("发送延迟事件彻底失败，任务终止，触发 more 回调");
                        more(runBody, event.getReserve());
                        return;
                    }
                    // 短暂休眠后重试
                    try {
                        Thread.sleep(100L * (retryCount + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("重试休眠被中断");
                    }
                }
            }
            // 发送成功，等待延迟消息触发下一次执行
        } else {
            // 已达最大重试次数，执行最终失败处理
            more(runBody, event.getReserve());
        }
    }

    /**
     * 获取当前 Bean 在 Spring 容器中的真实名称
     * <p>优化：优先使用 BeanNameAware 自动注入的名称，降级时才动态查找，提升可靠性和性能</p>
     * <p>修复：动态查找时正确处理 AOP 代理对象，确保比较的是真实 Bean 实例</p>
     *
     * @return Bean 名称，若获取失败返回 null
     */
    private String getCurrentBeanName() {
        // 优先使用通过 BeanNameAware 设置的真实名称（准确且无性能损耗）
        if (StrUtil.isNotBlank(beanName)) {
            return beanName;
        }

        // 降级：尝试从容器中动态查找（仅当 BeanNameAware 因特殊原因未生效时）
        try {
            Class<?> targetClass = AopUtils.getTargetClass(this);
            String[] names = applicationContext.getBeanNamesForType(targetClass);
            for (String name : names) {
                Object bean = applicationContext.getBean(name);
                // 处理可能的 AOP 代理，获取最终目标对象进行比较
                Object targetBean = AopProxyUtils.getSingletonTarget(bean);
                if (targetBean == this) {
                    beanName = name;
                    return beanName;
                }
            }
        } catch (Exception e) {
            log.warn("动态查找 Bean 名称失败", e);
        }

        // 最终降级：使用类名首字母小写（仅用于日志，实际可能不精确）
        String fallbackName = StrUtil.lowerFirst(this.getClass().getSimpleName());
        log.debug("使用降级 Bean 名称: {}", fallbackName);
        return fallbackName;
    }

    /**
     * 实现 BeanNameAware 接口，自动获取当前 Bean 在 Spring 容器中的真实名称
     *
     * @param name Spring 容器中的 beanName
     */
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }
}