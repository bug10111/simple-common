package com.simple.common.test.eventtest;

import com.simple.common.core.utils.JsonUtils;
import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.test.common.event.EventTestRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Component
public class EvenTestHandler {

    //员工A-发邮件
    @EventHandler
    public void test1(EventTestRequest event) {
        log.debug("test1开始执行内容：[{}]", JsonUtils.toJsonStr(event));
    }

    //员工B-发短信
    @EventHandler
    public void test11(EventTestRequest event) {
        log.debug("test11开始执行内容：[{}]", JsonUtils.toJsonStr(event));
    }
//
//    @EventHandler(value = "EventTestRequest")
//    public void test2(EventTestRequest event) {
//        log.debug("test2指定事件名称开始执行内容：[{}]", JsonUtils.toJsonStr(event));
//    }
//
//    @EventHandler(value = "EventTestRequest", applicationName = "simple-common-test")
//    public void test3(EventTestRequest event) {
//        log.debug("test3指定事件名称和服务名称开始执行内容：[{}]", JsonUtils.toJsonStr(event));
//    }
//
//    @EventHandler(value = "EventTestRequest", applicationName = "simple1")
//    public void test4(EventTestRequest event) {
//        log.debug("test3指定事件名称和错误的服务名称开始执行内容：[{}]", JsonUtils.toJsonStr(event));
//    }
//
//    @EventHandler(value = "EventTestRequest1", applicationName = "simple")
//    public void test5(EventTestRequest event) {
//        log.debug("test3指定错误的事件名称和正确的服务名称开始执行内容：[{}]", JsonUtils.toJsonStr(event));
//    }
//
//    @EventHandler(value = "simple", applicationName = "simple")
//    public void test6(EventTestRequest event) {
//        log.debug("test6指定事件名称和正确的服务名称开始执行内容：[{}]", JsonUtils.toJsonStr(event));
//    }

}
