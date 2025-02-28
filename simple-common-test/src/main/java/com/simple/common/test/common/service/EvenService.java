package com.simple.common.test.common.service;

import com.simple.common.test.common.event.EventTestRequest;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public interface EvenService {

    void testSend(EventTestRequest request);

    void testSend(EventTestRequest request, int time, TimeUnit timeUnit);

}
