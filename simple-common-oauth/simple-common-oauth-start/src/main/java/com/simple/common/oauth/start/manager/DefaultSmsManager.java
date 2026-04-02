package com.simple.common.oauth.start.manager;

import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.core.utils.HttpUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.oauth.start.common.dto.CheckSmsRequest;
import com.simple.common.oauth.start.common.dto.SendSmsRequest;
import com.simple.common.oauth.start.common.enums.OauthUrl;
import com.simple.common.oauth.start.common.manager.SmsManager;
import com.simple.common.oauth.start.common.properties.OauthStartProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Component
public class DefaultSmsManager implements SmsManager {

    @Autowired
    private OauthStartProperties oauthStartProperties;

    @Override
    public void send(SendSmsRequest request) {
        try {
            Optional<R> post = HttpUtils.post(oauthStartProperties.getUrl(OauthUrl.SMS_SEND) , getHead(), JsonUtils.toJsonStr(request), oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "发送验证码失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(), R.class).getMessage(), "", e);
        }

    }

    @Override
    public void checkSms(CheckSmsRequest request) {
        try {
            Optional<R> post = HttpUtils.post(oauthStartProperties.getUrl(OauthUrl.SMS_CHECK) , getHead(), JsonUtils.toJsonStr(request), oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "校验验证码失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(), R.class).getMessage(), "", e);
        }

    }

    private Map<String, String> getHead() {
        String header = HttpServletUtils.getRequest().getHeader("Authorization");
        Map<String, String> map = new HashMap<>();
        map.put("Authorization", header);
        return map;
    }
}
