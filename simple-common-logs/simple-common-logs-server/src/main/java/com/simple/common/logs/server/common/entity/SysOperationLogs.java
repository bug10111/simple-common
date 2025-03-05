package com.simple.common.logs.server.common.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.mp.common.enums.DeleteState;
import com.simple.common.mp.common.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
import java.math.BigDecimal;

/**
 * 操作日志(sys_operation_logs)实体类
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
@TableName(value = "sys_operation_logs", autoResultMap = true)
@Schema(title = "操作日志(sys_operation_logs)实体类")
public class SysOperationLogs {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 方法名称
     */
    @TableField(value = "title")
    private String title;

    /**
     * 请求方式
     */
    @TableField(value = "method")
    private String method;

    /**
     * 请求URL
     */
    @TableField(value = "oper_url")
    private String operUrl;

    /**
     * 主机地址
     */
    @TableField(value = "oper_ip")
    private String operIp;

    /**
     * 操作地点
     */
    @TableField(value = "oper_location")
    private String operLocation;

    /**
     * 操作人员id
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 操作人员
     */
    @TableField(value = "nickname")
    private String nickname;

    /**
     * 请求参数
     */
    @TableField(value = "oper_param")
    private String operParam;

    /**
     * 操作状态
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 错误消息
     */
    @TableField(value = "error_msg")
    private String errorMsg;

    /**
     * 错误消息详情
     */
    @TableField(value = "error_data")
    private String errorData;

    /**
     * 接口耗时
     */
    @TableField(value = "request_time")
    private Integer requestTime;

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

