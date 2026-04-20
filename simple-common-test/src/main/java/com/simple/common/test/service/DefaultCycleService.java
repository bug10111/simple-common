package com.simple.common.test.service;

import com.simple.common.eventbus.common.service.AbsEventCycleService;
import com.simple.common.test.common.entity.cycle.DataDemo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultCycleService extends AbsEventCycleService<DataDemo> {

    @Override
    public Class<DataDemo> getEventClass() {
        return DataDemo.class;
    }

    @Override
    protected Boolean handler(DataDemo runBody, Map<String, Object> parameters) {
        //todo 模拟http查单请求
        log.debug("查询中。。。。");
        return false;
    }

    @Override
    protected void ok(DataDemo runBody, Map<String, Object> parameters) {
        log.debug("正在执行查询成功逻辑");
    }

    @Override
    protected void more(DataDemo runBody, Map<String, Object> parameters) {
        log.debug("查询已达最大次数");
    }

    @Override
    protected void error(DataDemo runBody, Map<String, Object> parameters) {
        log.debug("业务异常，查询失败");
    }

}
