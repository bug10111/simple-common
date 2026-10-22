package com.simple.common.eventbus.common.service;

import cn.hutool.core.util.StrUtil;
import com.simple.common.core.common.service.cycle.AbsCycleService;
import com.simple.common.eventbus.common.event.CycleEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.aop.support.AopUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于事件的计划任务抽象类
 * <p>计数口径与父类 {@link AbsCycleService} 严格对齐：sum 为<strong>总执行次数（含首次执行）</strong>，
 * 任务恰好执行 sum 次；addCounter 在每次执行前调用（首次在 {@link #run} 中，第 k 次重试在发送延迟事件成功后）。</p>
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

        // 与父类 addCounter 口径对齐：首次执行同样计入计数器（事件链路无调度线程包装，执行前直接计数）
        addCounter(runBody, parameters, 1);
        execution(event);
    }

    /**
     * 执行单次任务调度
     * <p>事件重入口径：event.num 为本次执行前已完成的执行次数（首次执行为 0），
     * 本次执行结束后累计完成 num+1 次，是否继续重试统一由 handleFailure 判定。</p>
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
            // 参数转换失败属终止性失败：无法还原出 T 类型，将原始参数对象交给 error 回调留痕，且不再重试
            safeError(rawBody(event), event.getReserve());
            return;
        }

        boolean success;
        try {
            // try 范围仅覆盖业务处理器：ok/more/error 回调自身的异常不允许被误判为任务失败而触发重试
            success = handler(runBody, event.getReserve());
        } catch (Throwable e) {
            log.error("调度异常", e);
            handleFailure(event, runBody, true);
            return;
        }

        if (!success) {
            handleFailure(event, runBody, false);
        } else {
            safeOk(runBody, event.getReserve());
        }
    }

    /**
     * 处理执行失败的情况
     * <p>业务返回 false 与业务抛异常统一由此判定重试边界：
     * 本次执行结束后累计完成 nextNum = num+1 次，仅当补执行一次不超过 sum（nextNum &lt; sum）时重试，
     * 任务以恰好 sum 次执行收尾。已达上限时：业务异常路径触发 error 回调，业务失败(false)路径触发 more 回调，
     * 与既有语义保持一致。</p>
     *
     * @param event       循环事件对象
     * @param runBody     已转换的任务参数
     * @param exceptional true=业务处理器抛出异常（终止回调走 error）；false=业务处理器返回 false（终止回调走 more）
     */
    private void handleFailure(CycleEvent event, T runBody, boolean exceptional) {
        int nextNum = event.getNum() + 1;
        // sum 为总执行次数（含首次），首次执行已计入，仅当补执行一次仍不超过 sum 时才继续重试
        if (nextNum < event.getSum()) {
            // 计算延迟时间
            int delay = event.getIsAccumulate() ? event.getTimeInterval() * nextNum : event.getTimeInterval();
            event.setNum(nextNum);

            // 获取当前 Bean 名称（优先使用 BeanNameAware 注入的，其次动态查找）
            String currentBeanName = getCurrentBeanName();
            if (currentBeanName == null) {
                log.error("无法获取当前 Bean 名称，循环任务终止");
                terminate(runBody, event.getReserve(), exceptional);
                return;
            }
            event.setBeanName(currentBeanName);

            // 发送延迟事件，发送失败立即重试（不做线程休眠，避免阻塞 MQ 消费/调度线程），彻底失败则终止任务
            int maxSendRetry = 3;
            boolean sent = false;
            for (int retryCount = 0; retryCount < maxSendRetry && !sent; retryCount++) {
                try {
                    eventBusService.push(event, delay, TimeUnit.SECONDS);
                    sent = true;
                } catch (Exception e) {
                    // 捕获全部异常：push 除 AmqpException 外还可能抛出断言/序列化/连接工厂等运行时异常，
                    // 只捕 AmqpException 会让其余异常逃逸回 execution 失败分支，导致 num 错加、延迟事件重复发布
                    log.error("发送延迟事件失败，第{}次尝试", retryCount + 1, e);
                }
            }
            if (!sent) {
                // 发送彻底失败，任务无法继续，触发终止回调告知业务方
                log.error("发送延迟事件彻底失败，任务终止");
                terminate(runBody, event.getReserve(), exceptional);
                return;
            }

            // 发送成功后本次补执行才计入计数器（计数口径与父类一致：执行前计数；发送失败的任务不会执行，不计数）
            addCounter(runBody, event.getReserve(), nextNum);
            // 等待延迟消息触发下一次执行
        } else {
            // 已达最大执行次数（sum 含首次执行），执行终止回调
            terminate(runBody, event.getReserve(), exceptional);
        }
    }

    /**
     * 任务终止收尾：业务异常路径回调 error，业务返回 false 路径回调 more（保持既有语义）
     */
    private void terminate(T runBody, Map<String, Object> reserve, boolean exceptional) {
        if (exceptional) {
            safeError(runBody, reserve);
        } else {
            safeMore(runBody, reserve);
        }
    }

    /**
     * 成功回调（回调异常仅记录日志，不触发任务重试）
     */
    private void safeOk(T runBody, Map<String, Object> reserve) {
        try {
            ok(runBody, reserve);
        } catch (Exception e) {
            log.error("执行成功回调 ok 异常", e);
        }
    }

    /**
     * 超次回调（回调异常仅记录日志）
     */
    private void safeMore(T runBody, Map<String, Object> reserve) {
        try {
            more(runBody, reserve);
        } catch (Exception e) {
            log.error("执行超次回调 more 异常", e);
        }
    }

    /**
     * 异常回调（回调异常仅记录日志）
     */
    private void safeError(T runBody, Map<String, Object> reserve) {
        try {
            error(runBody, reserve);
        } catch (Exception e) {
            log.error("执行异常回调 error 异常", e);
        }
    }

    /**
     * 取未转换的原始任务参数（转换失败场景下供 error 回调留痕，避免传入 null 使子类回调 NPE 丢失原始失败原因）
     */
    @SuppressWarnings("unchecked")
    private T rawBody(CycleEvent event) {
        return (T) event.getRunBody();
    }

    /**
     * 获取当前 Bean 在 Spring 容器中的真实名称
     * <p>优先使用 BeanNameAware 自动注入的名称，未注入时才从容器动态查找，减少不必要的容器遍历</p>
     * <p>动态查找时对容器 bean 统一解包单例代理目标后再比较，
     * 同时保留直接引用比较，兼容“容器 bean 是代理”的场景</p>
     *
     * @return Bean 名称；无法确定时返回 null（调用方会终止任务并触发终止回调）
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
                if (isSameTarget(bean, this)) {
                    beanName = name;
                    return beanName;
                }
            }
        } catch (Exception e) {
            log.warn("动态查找 Bean 名称失败", e);
        }

        // 无法确定真实 Bean 名称时必须返回 null，由调用方触发终止回调结束任务；
        // 若返回按类名猜测的 bean 名，事件中会写入容器中不存在的名称，消费侧找不到服务后任务被静默丢弃（无任何回调）
        log.warn("无法确定当前 Bean 名称，循环任务将终止, class: {}", this.getClass().getName());
        return null;
    }

    /**
     * 判断容器 bean 与当前执行实例是否指向同一目标对象（解包单例代理目标后比较）
     */
    private boolean isSameTarget(Object bean, Object current) {
        if (bean == current) {
            return true;
        }
        Object beanTarget = AopProxyUtils.getSingletonTarget(bean);
        return beanTarget != null && beanTarget == current;
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
