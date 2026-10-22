package com.simple.common.auth.client.common.entity.login;

import com.simple.common.auth.client.common.enums.DataScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.Set;

/**
 * 数据权限信息
 * 用于存储用户的数据权限信息，包含租户ID、数据权限类型、部门ID集合等
 *
 * @author qty
 */
@Data
@EqualsAndHashCode(of = {"tenantId", "permissionScope"})
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

    /**
     * 部门ID集合（只读视图）。
     * <p>
     * 数据权限是授权上下文：绕过 setter 直接增删部门 ID 等于越权放大，
     * 此处返回不可变视图；需要变更时通过 {@link #setDepartmentIds(Set)} 整体替换。
     * </p>
     *
     * @return 部门ID集合（无值返回空集合，不返回 null）
     */
    public Set<String> getDepartmentIds() {
        return departmentIds == null ? Collections.emptySet() : Collections.unmodifiableSet(departmentIds);
    }
}
