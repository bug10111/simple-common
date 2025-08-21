package com.simple.common.websocket.common.attribute;

import cn.hutool.system.UserInfo;
import io.netty.util.AttributeKey;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public class ChannelAttributes {
    // ✅ 用户ID
    public static final AttributeKey<String> cli_key = AttributeKey.valueOf("userId");
}

