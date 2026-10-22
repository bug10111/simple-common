package com.simple.common.eventbus.event;

import cn.hutool.core.util.StrUtil;
import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.eventbus.common.event.CycleEvent;
import com.simple.common.eventbus.common.service.AbsEventCycleService;
import com.simple.common.eventbus.util.CycleFactoryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 自动重试任务的事件实现
 *
 * @author qty
 */

@Slf4j
@Component
public class CycleEventHandler {

    @EventHandler
    public void on(CycleEvent event) {
        // 消息体反序列化结果可能为 null（body 为空），beanName 是服务定位必要字段；
        // 直接解引用会在同步模式向发布方抛 NPE、在 MQ 模式静默丢消息
        if (event == null || StrUtil.isBlank(event.getBeanName())) {
            log.error("CycleEvent 或 beanName 不能为空");
            return;
        }

        AbsEventCycleService<?> cycleService = CycleFactoryUtils.getCycleService(event.getBeanName());
        if (cycleService == null) {
            // 缓存初始化早于服务 bean 就绪（启动窗口期/上下文刷新）会造成误判缺失，
            // 先强制刷新一次服务缓存再重新解析，避免把「还没装载」当成「不存在」而永久丢弃任务
            CycleFactoryUtils.refreshCycleServiceCache();
            cycleService = CycleFactoryUtils.getCycleService(event.getBeanName());
        }

        if (cycleService == null) {
            // 服务确实不存在（本实例不承载该任务或 beanName 非法）：
            // 记录可定位的进度信息便于人工恢复；此时服务本身缺失，无法再走 more/error 业务回调
            log.error("没有找到CycleService有效实现，任务无法继续。beanName: {}, 已执行次数: {}/{}",
                    event.getBeanName(), event.getNum(), event.getSum());
        } else {
            cycleService.execution(event);
        }
    }

}
