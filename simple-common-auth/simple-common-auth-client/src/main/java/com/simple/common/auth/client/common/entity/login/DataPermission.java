package com.simple.common.auth.client.common.entity.login;

import com.simple.common.auth.client.common.enums.DataScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * 数据权限信息
 * 用于存储用户的数据权限信息，包含租户ID、数据权限类型、部门ID集合等
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(title = "数据权限信息")
public class DataPermission {

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "数据权限类型")
    private DataScopeEnum permissionScope;

    @Schema(description = "部门ID集合")
    private Set<String> departmentIds;

    /**
     * 判断是否为最高权限（全部数据权限）
     */
    public boolean isAll() {
        return permissionScope != null && permissionScope.isAll();
    }

    /**
     * 判断是否为最低权限（仅本人数据权限）
     */
    public boolean isSelf() {
        return permissionScope != null && permissionScope.isSelf();
    }

}