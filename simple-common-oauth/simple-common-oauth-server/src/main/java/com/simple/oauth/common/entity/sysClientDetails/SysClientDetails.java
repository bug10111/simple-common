package com.simple.oauth.common.entity.sysClientDetails;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.mp.common.enums.Status;
import com.simple.oauth.common.enums.ServerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 客户端信息(sys_client_details)实体类
 * 注解@JSONField(serialize = false)，表示不返回这个字段
 * 注解@TableField，用于标志属性
 * value = "数据库字段"，用于标志数据库对应字段
 * exist = false，表示数据库没有这个字段
 * typeHandler = JacksonTypeHandler.class，表示对象JSON转化为实例，需要类上开启@TableName(autoResultMap = true)
 * fill = FieldFill.INSERT，表示添加操作时要做的事情
 * 注解@TableLogic添加在属性上，结合配置文件，可设置逻辑删除
 * 注解@EqualsAndHashCode是为类生成Equals和HashCode方法
 * callSuper = false 代表方法不调用父类继承的属性，只匹配子类本身是否相同
 * callSuper = true 代表方法需要调用父类继承的属性，同时匹配本身和父类的属性
 *
 * @author 兄台丶请冷静
 */
@Data //提供读写属性, 此外还提供了 equals()、hashCode()、toString() 方法
@JsonIgnoreProperties(ignoreUnknown = true) //json转换时，字段少了也可以转换
@Accessors(chain = true) //开启链式调用
@TableName(value = "sys_client_details", autoResultMap = true)
@Schema(title = "客户端信息(sys_client_details)实体类")
public class SysClientDetails {

    /**
     * 客户端信息id
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 服务
     */
    @TableField(value = "server")
    private String server;

    /**
     * 服务类型
     */
    @TableField(value = "server_type")
    private ServerType serverType;

    /**
     * 客户端名称
     */
    @TableField(value = "client_name")
    private String clientName;

    /**
     * 客户端（如：xiaoyue_client）
     */
    @TableField(value = "client_id")
    private String clientId;

    /**
     * 客户端密码（要加密后存储)，即秘钥
     */
    @TableField(value = "client_secret")
    private String clientSecret;

    /**
     * 预留字段，客户端能访问的资源id集合（微服务名称），多个用逗号分隔
     */
    @TableField(value = "resource_ids")
    private String resourceIds;

    /**
     * 作用域all,write,read
     */
    @TableField(value = "scope", typeHandler = JacksonTypeHandler.class)
    private List<String> scope;

    /**
     * （可选）token有效时间（单位秒），不填默认(60 * 60 * 12, 12小时)
     */
    @TableField(value = "access_token_validity")
    private Integer accessTokenValidity;

    /**
     * （可选）刷新令牌的有效时间（单位秒），不填默认(60 * 60 * 24 * 30, 30天)
     */
    @TableField(value = "refresh_token_validity")
    private Integer refreshTokenValidity;

    /**
     * 32位密钥字符串
     */
    @TableField(value = "hs_key")
    private String hsKey;

    /**
     * RSA公钥
     */
    @TableField(value = "rsa_public")
    private String rsaPublic;

    /**
     * RSA私钥
     */
    @TableField(value = "rsa_private")
    private String rsaPrivate;

    /**
     * 是否有微信小程序
     */
    @TableField(value = "has_wx")
    private Boolean hasWx;

    /**
     * appid
     */
    @TableField(value = "wx_app_id")
    private String wxAppId;

    /**
     * wxAppSecret
     */
    @TableField(value = "wx_app_secret")
    private String wxAppSecret;

    /**
     * 预留字段，若填写建议为map（字典code，值）的json
     */
    @TableField(value = "reserve", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> reserve;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 状态：1-正常，0-停用
     */
    @TableField(value = "status")
    private Status status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}

