package com.simple.oauth.event;

import com.simple.common.core.utils.JsonUtils;
import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.sms.common.service.SmsService;
import com.simple.oauth.common.entity.sms.LowBalanceAlert;
import com.simple.oauth.common.event.prompt.LowBalanceAlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 设备低余额事件实现
 *
 * @author qty
 */
@Slf4j
@Component
public class LowBalanceAlertEventHandler {

    @Autowired
    private SmsService smsService;

    @EventHandler
    public void on(LowBalanceAlertEvent event) {
        LowBalanceAlert alert = new LowBalanceAlert();
        alert.setAddress(event.getParkSourceSelectionStr().length() >= 30 ? event.getParkSourceSelectionStr().substring(0, 27) + "..." : event.getParkSourceSelectionStr());
        alert.setMsg(event.getDeviceType() + "：" + event.getNum());
        alert.setSum(event.getSum());

        //短信类型来自模板表
        smsService.sendTemplateParam(event.getPhone(), "PROMPT", JsonUtils.toJsonStr(alert), "127.0.0.1");
    }
}
