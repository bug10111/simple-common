package com.simple.oauth.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 跨站请求伪造配置
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.auth.oauth")
public class OauthProperties {

    //默认超级管理员角色和账号，拥有所有权限
    private String admin = "admin";

    //默认初始客户端、管理员密码
    private String adminPwd = "123456";

    //默认授权中心服务端
    private String oauth = "oauth";

    //默认授权中心客户端
    private String oauthClient = "oauth-web";

    //默认授权中心密码
    private String oauthPwd = "123456";

    //默认游客角色
    private String tourists = "tourists";

    //微信登录超时时间
    private int wxLoginTimeout = 10000;

    //是否缓存字典
    private Boolean dictCache = false;

    //字典缓存key
    private String dictKey = "dict:";

    //是否开启权限菜单同步到所有客户端
    private Boolean menuOpenAll = false;

    //用户名称缓存的key
    private String userNameCacheKey = "user:name";

    //解密字符串分割符
    private String decryptSplitStr = ":";

    /**
     * 获取字典key
     *
     * @param type type字典类型
     */
    public String getDictKey(String type) {
        return dictKey + type;
    }
}
