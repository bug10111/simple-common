package com.simple.common.core.common.config;

import com.simple.common.core.common.service.thread.ThreadService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA
 * Description: 线程池调度
 *
 * @author 兄台丶请冷静
 */
@Configuration
public class DefaultThreadConfig {

    @Autowired
    private ThreadService threadService;

    @PreDestroy
    public void destroy() {
        threadService.shutdown();
    }

}
