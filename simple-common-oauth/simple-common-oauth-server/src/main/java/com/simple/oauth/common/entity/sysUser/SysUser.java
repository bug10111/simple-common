package com.simple.oauth.common.entity.sysUser;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.Map;

/**
 * 用户(sys_user)实体类
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
 * @author qty
 */
@Data //提供读写属性, 此外还提供了 equals()、hashCode()、toString() 方法
@JsonIgnoreProperties(ignoreUnknown = true) //json转换时，字段少了也可以转换
@Accessors(chain = true) //开启链式调用
@TableName(value = "sys_user", autoResultMap = true)
@Schema(title = "用户(sys_user)实体类")
public class SysUser {

    /**
     * 用户id
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 用户名称
     */
    @TableField(value = "nickname")
    private String nickname;

    /**
     * 用户账号
     */
    @TableField(value = "username")
    private String username;

    /**
     * 手机号码
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 帐户是否过期：1-未过期，0-已过期
     */
    @TableField(value = "is_account_non_expired")
    private Integer isAccountNonExpired;

    /**
     * 帐户是否被锁定：1-未锁定，0-已锁定
     */
    @TableField(value = "is_account_non_locked")
    private Integer isAccountNonLocked;

    /**
     * 密码是否过期：1-未过期，0-已过期
     */
    @TableField(value = "is_credentials_non_expired")
    private Integer isCredentialsNonExpired;

    /**
     * 帐户是否可用：1-可用，0-禁用
     */
    @TableField(value = "is_enabled")
    private Integer isEnabled;

    /**
     * 扩展
     */
    @TableField(value = "reserve", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> reserve;

    //    /**
    //     * 删除：1-已删除，0-未删除
    //     */
    //    @TableLogic
    //    @TableField(value = "deleted")
    //    private DeleteState deleted;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}

