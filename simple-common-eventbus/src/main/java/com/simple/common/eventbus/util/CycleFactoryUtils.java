package com.simple.common.eventbus.util;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.eventbus.common.service.AbsEventCycleService;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 自动重试任务工厂实现
 *
 * @author qty
 */
public class CycleFactoryUtils {

    private static volatile Map<String, AbsEventCycleService> map = null;

    /**
     * 获取执行bean
     * @param name bean名称
     */
    public static AbsEventCycleService getCycleService(String name) {
        if (map == null) {
            synchronized (CycleFactoryUtils.class) {
                if (map == null) {
                    map = SpringUtil.getBeansOfType(AbsEventCycleService.class);
                }
            }
        }
        return map.get(name);
    }

}
