package com.simple.common.test.service;

import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.common.test.common.event.EventTestRequest;
import com.simple.common.test.common.service.EvenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultEvenService implements EvenService {

    @Autowired
    private EventBusService eventBusService;

    @Override
    public void testSend(EventTestRequest request) {
        eventBusService.push(request, EventTestRequest.class);
    }

    @Override
    public void testSend(EventTestRequest request, int time, TimeUnit timeUnit) {
        eventBusService.push(request, EventTestRequest.class, time, timeUnit);
    }
}
