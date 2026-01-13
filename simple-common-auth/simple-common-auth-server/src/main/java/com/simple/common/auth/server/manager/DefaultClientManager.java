package com.simple.common.auth.server.manager;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.simple.common.auth.server.common.enums.ClientAttribute;
import com.simple.common.auth.server.common.manager.client.ClientManager;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultClientManager implements ClientManager {

    @Override
    public String encrypt(String clientId, String clientSecret) {
        return Base64.encode(clientId + ":" + clientSecret);
    }

    @Override
    public Map<ClientAttribute, String> decryptStr(String header) {
        // `Basic ` 后面开始截取 clientId:clientSecret
        String base64Token = header.trim().substring(6);

        //还原token
        String token = Base64.decodeStr(base64Token);

        //分割，获取数据组
        boolean contains = StrUtil.contains(token, ":");
        AssertUtils.isTrue(contains, "请求头无效", "请求头无效 ==> [{}]", header);

        String[] split = token.split(":");
        AssertUtils.isTrue(split.length == 2, "请求头无效", "请求头无效 ==> [{}]", header);

        Map<ClientAttribute, String> map = new HashMap<>();
        map.put(ClientAttribute.ClientId, split[0]);
        map.put(ClientAttribute.ClientSecret, split[1]);
        return map;
    }
}
