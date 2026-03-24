package com.come.on.alibaba;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.core.utils.SignUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Configuration
public class FeignConfig implements RequestInterceptor {

    @Autowired
    private SignManager signManager;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        UserTemporary userTemporary = LoginUserUtils.getUserTemporary();
        if (userTemporary != null) {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {

                //传递token
                String token = requestAttributes.getRequest().getHeader(TokenConstant.Authorization);
                requestTemplate.header(TokenConstant.Authorization, token);

                //传递序序列化数据
                String userJson = JsonUtils.toJsonStr(userTemporary);
                String encoded = Base64.getEncoder().encodeToString(userJson.getBytes(StandardCharsets.UTF_8));
                requestTemplate.header(TokenConstant.userHead, encoded);

                //传递签名
                String key = signManager.getKey();
                String signNew = SignUtils.signWeb(encoded, key);
                requestTemplate.header(TokenConstant.userSignHead, signNew);
            }
        }

    }
}