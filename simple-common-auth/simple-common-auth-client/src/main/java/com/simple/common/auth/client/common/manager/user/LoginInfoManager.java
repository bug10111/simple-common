package com.simple.common.auth.client.common.manager.user;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 登录用户信息管理器接口。
 * <p>
 * 用于管理当前登录用户的临时信息和身份标识,包括用户信息、权限信息、Token关联等。
 * 默认实现 {@link com.simple.common.auth.client.common.manager.user.ClientLoginInfoManager} 
 * 基于 ThreadLocal 和 Redis 存储,支持高性能的用户信息获取。
 * </p>
 *
 * <h3>扩展方式：</h3>
 * <p>
 * 如需自定义用户信息存储方式(如使用其他缓存中间件),可实现此接口并替换默认实现。
 * </p>
 *
 * @author qty
 */
public interface LoginInfoManager {

    String client_manager_name = "clientLoginInfoManager";
    String server_manager_name = "serverLoginInfoManager";

    /**
     * 获取 Token 内省的用户信息
     * <p>
     * 根据 Token 的 jti (唯一标识) 从缓存中获取用户详细信息。
     * </p>
     *
     * @param key Token 的 jti 或用户ID
     * @return 用户信息Map,包含 userId、username、nickName 等字段,不存在返回空 Map
     */
    Map<Object, Object> getUserInfo(String key);

    /**
     * 获取登录用户权限信息
     * <p>
     * 根据用户角色获取对应的权限映射关系。
     * 权限信息通常包括菜单权限、按钮权限、数据权限等。
     * </p>
     *
     * @param loginRole 用户登录的角色集合
     * @return 权限信息Map,key为角色,value为该角色的权限Map
     */
    Map<Object, Map<Object,Object>> getAuthorities(HashSet<String> loginRole);

    /**
     * 获取登录用户权限信息（支持项目维度）
     * <p>
     * 根据用户角色和项目编码获取对应的权限映射关系。
     * 用于按需加载权限场景，客户端调用时传入 projectCode。
     * </p>
     *
     * @param loginRole   用户登录的角色集合
     * @param projectCode 项目编码（client_id），如 "xiaoyue-web-client"
     * @return 权限信息Map,key为角色,value为该角色的权限Map
     */
    default Map<Object, Map<Object, Object>> getAuthoritiesByProjectCode(HashSet<String> loginRole, String projectCode) {
        // 默认实现：使用当前配置的 projectCode
        return getAuthorities(loginRole);
    }

    /**
     * 获取用户关联的所有 Token
     * <p>
     * 查询指定用户ID关联的所有有效 Token,用于单点登录、强制下线等场景。
     * </p>
     *
     * @param userId 用户ID
     * @return Token 集合(jti列表),不存在返回空 Set
     */
    Set<String> getUserToken(String userId);

    /**
     * 判断当前用户是否拥有指定权限
     * <p>
     * 检查用户的角色是否具有指定的权限标识。
     * 常用于接口级别的权限校验。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 检查用户是否有 "user:read" 权限
     * HashSet<String> roles = new HashSet<>();
     * roles.add("admin");
     * boolean hasAuth = loginInfoManager.hasAuth(roles, new String[]{"user:read"});
     * if (!hasAuth) {
     *     throw new SecurityException("权限不足");
     * }
     * }</pre>
     *
     * @param loginRole 用户登录的角色集合
     * @param authority 需要校验的权限标识数组
     * @return true 表示拥有权限,false 表示没有权限
     */
    Boolean hasAuth(HashSet<String> loginRole, String[] authority);

}