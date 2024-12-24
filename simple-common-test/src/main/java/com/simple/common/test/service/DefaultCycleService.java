package com.simple.common.test.service;

import com.simple.common.core.common.service.cycle.AbsCycleService;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.test.common.entity.cycle.DataDemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Component
public class DefaultCycleService extends AbsCycleService<DataDemo> {

    private static final Logger log = LoggerFactory.getLogger(DefaultCycleService.class);

    public DefaultCycleService() {
        super(DataDemo.class);
    }

    @Override
    protected Boolean handler(DataDemo runBody, Map<String, String> parameters) {
        //todo 模拟http查单请求
        log.debug("查询中。。。。");
        AssertUtils.error("业务异常");
        return false;
    }

    @Override
    protected void ok(DataDemo runBody, Map<String, String> parameters) {
        log.debug("正在执行查询成功逻辑");
    }

    @Override
    protected void more(DataDemo runBody, Map<String, String> parameters) {
        log.debug("查询已达最大次数");
    }

    @Override
    protected void error(DataDemo runBody, Map<String, String> parameters) {
        log.debug("业务异常，查询失败");
    }
}
