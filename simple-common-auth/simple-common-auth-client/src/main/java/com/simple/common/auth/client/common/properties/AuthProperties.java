package com.simple.common.auth.client.common.properties;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * <p>
 * auth配置类
 * </p>
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.auth")
public class AuthProperties {

    //服务端地址（生产必须 https：clientSecret 与下发密钥经此链路传输）
    private String serverUrl = "http://localhost:8000";

    //客户端获取服务端用户信息的超时时间
    private Integer timeOut = 1000 * 10;

    //单位时间内，登录失败的最大次数的key
    private String loginErrorKey = "loginError:";

    //单位时间内，登录失败的最大次数
    private int loginErrorNumber = 5;

    //登录失败计次的单位时间
    private int loginErrorTime = 60 * 60 * 24;

    //缓存类型，默认使用Redis缓存
    private CacheTypeEnum cacheType = CacheTypeEnum.REDIS;

    //权限缓存过期时间（单位：秒），默认24小时
    private Integer permissionCacheExpire = 60 * 60 * 24;

    //项目编码（必填），用于权限隔离。必须配置为当前服务所属的客户端标识（如 xiaoyue-web-client）
    private String projectCode;

    //超级管理员角色标识，拥有此角色的用户自动拥有所有权限，不参与权限校验。默认 admin
    private String superAdminRoleKey = "admin";

    //客户端凭证（实例注册自举用，即控制台项目客户端的 clientSecret；客户端模式必配）
    private String clientSecret;

    //客户端编码（实例注册自举用，即控制台项目客户端的 clientId；可选，未配置回落 spring.application.name；配置后须与 project-code、控制台登记 client_id 同值）
    private String clientId;

    //实例接收回调地址（授权中心投递引擎按此回调本实例，如 http://10.0.0.5:8080；客户端模式必配）
    private String instanceCallbackUrl;

    //实例本机 IP（缺省自动探测本机地址；容器部署建议显式配置 Pod IP）
    private String instanceIp;

    //内部同步端点共享令牌（可选纵深防御）：配置后 /internal/sync 回调必须携带 X-Internal-Token 且与该值恒等时间匹配；
    //未配置保持既有内网隔离形态并输出一次性告警
    private String internalSyncToken;

    /**
     * 启动期校验（void 生命周期方法；原 @PostConstruct 挂在 String 返回值的 getter 上，
     * 每次调用都重复校验且违反生命周期方法 void 约定）。
     */
    @PostConstruct
    public void validateProjectCode() {
        if (projectCode == null || projectCode.isBlank()) {
            throw new IllegalStateException("项目编码未配置，请在 application.yml 中配置 simple.auth.project-code（当前服务所属的客户端标识）");
        }
    }

    /**
     * 获取项目编码
     * <p>
     * 必须配置 simple.auth.project-code，否则抛出异常。
     * </p>
     *
     * @return 项目编码（即 client_id）
     */
    public String getProjectCode() {
        if (projectCode == null || projectCode.isBlank()) {
            throw new IllegalStateException("项目编码未配置，请在 application.yml 中配置 simple.auth.project-code（当前服务所属的客户端标识）");
        }
        return projectCode;
    }

    /**
     * 获取登录失败保存的key
     * <p>
     * 去除尾部悬空冒号分隔符歧义：key 形如 loginError:{key}:{ip}，由消费方拼装。
     * </p>
     */
    public String getKey(String key) {
        return loginErrorKey + key + ":";
    }

}
