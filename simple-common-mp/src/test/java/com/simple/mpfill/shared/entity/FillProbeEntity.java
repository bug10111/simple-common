package com.simple.mpfill.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.simple.common.mp.common.enums.Status;

import java.util.Date;

/**
 * 固定字段填充探针实体：不带任何 fill 注解，主键为 INPUT 策略（应用侧赋值）。
 *
 * <p>用于集成测试复现 MyBatis-Plus 在 {@code TableInfo.isWithInsertFill()=false} 时不触发
 * {@code MetaObjectHandler.insertFill} 的门控事实，并验证兜底拦截器的填充与不覆盖语义。</p>
 *
 * @author qty
 */
@TableName("t_fill_probe")
public class FillProbeEntity {

    /**
     * 主键，应用侧赋值（IdType.INPUT）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 创建人ID
     */
    private String createUserId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 状态
     */
    private Status status;

    /**
     * 扩展字段
     */
    private String reserve;

    /**
     * 备注
     */
    private String remark;

    /**
     * 业务名称
     */
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getReserve() {
        return reserve;
    }

    public void setReserve(String reserve) {
        this.reserve = reserve;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
