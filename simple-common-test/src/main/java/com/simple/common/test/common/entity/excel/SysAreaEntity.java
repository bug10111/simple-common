package com.simple.common.test.common.entity.excel;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Created with Generator.
 * <p>
 * <p>
 * 注解@TableField(exist = false)，表示数据库没有这个字段
 */
@Data //提供读写属性, 此外还提供了 equals()、hashCode()、toString() 方法
@NoArgsConstructor //生成无参构造
@AllArgsConstructor //生成所有参数的构造
@TableName("sys_area")
public class SysAreaEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    //@TableField(exist = false)
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("parent_code")
    //@TableField(exist = false)
    private String parentCode;

    @TableField("ancestors")
    //@TableField(exist = false)
    private String ancestors;

    @TableField("area_name")
    //@TableField(exist = false)
    private String areaName;

    @TableField("area_code")
    //@TableField(exist = false)
    private String areaCode;

    @TableField("create_time")
    //@TableField(exist = false)
    private String createTime;

    @TableField("area_state")
    //@TableField(exist = false)
    private Integer areaState;

    @TableField("update_time")
    //@TableField(exist = false)
    private String updateTime;

    @TableField(exist = false)
    private List<SysAreaEntity> list;

}
