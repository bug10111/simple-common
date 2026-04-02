package com.simple.oauth.common.manager.init;

/**
 * Created with IntelliJ IDEA
 * Description: OauthInit接口
 *
 * @author 兄台丶请冷静
 */
public interface OauthInitManager {

    /**
     * 初始化服务配置
     */
    void loadingSysConfig();

    /**
     * 初始化客户端密钥
     */
    void loadingSecret();

    /**
     * 初始化用户和角色
     */
    void loadingUserAndRole();

    /**
     * 校准权限
     */
    void auth();

}
