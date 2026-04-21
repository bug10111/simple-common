package com.simple.common.oauth.start.service;

import com.simple.common.core.utils.IPUtils;
import com.simple.common.oauth.start.common.dto.CheckSmsRequest;
import com.simple.common.oauth.start.common.dto.SendSmsRequest;
import com.simple.common.oauth.start.common.manager.SmsManager;
import com.simple.common.oauth.start.common.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultSmsService implements SmsService {

    @Autowired
    private SmsManager smsManager;

    @Override
    public void send(SendSmsRequest request) {
        request.setIp(IPUtils.getIpAddr());
        smsManager.send(request);
    }

    @Override
    public void checkSms(CheckSmsRequest request) {
        smsManager.checkSms(request);
    }
}
