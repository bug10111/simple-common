package com.simple.common.sms.common.entity.sysSmsTemplate;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.mp.common.enums.DeleteState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 短信模板(sys_sms_template)实体类
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
@Schema(title = "短信模板(sys_sms_template)实体类")
@Data //提供读写属性, 此外还提供了 equals()、hashCode()、toString() 方法
@JsonIgnoreProperties(ignoreUnknown = true) //json转换时，字段少了也可以转换
@Accessors(chain = true) //开启链式调用
@TableName(value = "sys_sms_template", autoResultMap = true)
public class SysSmsTemplate {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 短信类型
     */
    @TableField(value = "send_type")
    private String sendType;

    /**
     * 短信签名
     */
    @TableField(value = "sign_name")
    private String signName;

    /**
     * 模板code
     */
    @TableField(value = "template_code")
    private String templateCode;

    /**
     * 数据状态
     */
    @TableLogic
    @TableField(value = "deleted")
    private DeleteState deleted;

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

