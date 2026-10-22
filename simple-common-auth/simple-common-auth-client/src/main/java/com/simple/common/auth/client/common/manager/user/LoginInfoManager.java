package com.simple.common.auth.client.common.manager.user;

import java.util.Map;
import java.util.Set;

/**
 * 登录用户信息管理器接口。
 * <p>
 * 用于管理当前登录用户的临时信息和身份标识,包括用户信息、权限信息、Token关联等。
 * 默认实现 {@link com.simple.common.auth.client.common.manager.user.ClientLoginInfoManager}
 * 基于 ThreadLocal 与 Redis 存储,支持高性能的用户信息获取。
 * </p>
 *
 * <h3>扩展方式：</h3>
 * <p>
 * 如需自定义用户信息存储方式(如使用其他缓存中间件),可实现此接口并替换默认实现。
 * </p>
 *
 * <h3>返回值契约：</h3>
 * <p>
 * {@link #getUserInfo}、{@link #getAuthorities}、{@link #getUserToken} 不返回 null：
 * 无数据返回空 Map/Set；实现方必须遵守，避免调用方遍历时 NPE。
 * </p>
 *
 * @author qty
 */
public interface LoginInfoManager {

    /**
     * 客户端登录信息管理器 bean 名（保持既有小写下划线常量名以兼容既有 @Component/SpringUtil 用法）
     */
    String client_manager_name = "clientLoginInfoManager";

    /**
     * 服务端登录信息管理器 bean 名（auth-server 模块装配，client-only 部署不存在）
     */
    String server_manager_name = "serverLoginInfoManager";

    /**
     * 获取 Token 内省的用户信息
     *
     * @param key Token 的 jti（JWT 唯一标识）；不接收 userId——两者是不同标识，
     *            传 userId 会静默得到错误/空数据
     * @return 用户信息Map,包含 userId、username、nickname 等字段,不存在返回空 Map（不返回 null）
     */
    Map<Object, Object> getUserInfo(String key);

    /**
     * 获取登录用户权限信息
     * <p>
     * 根据用户角色获取对应的权限映射关系。
     * 权限信息通常包括菜单权限、按钮权限、数据权限等。
     * projectCode 取本服务配置（simple.auth.project-code），租户维度取当前请求租户上下文。
     * </p>
     *
     * @param loginRole 用户登录的角色集合
     * @return 权限信息Map,key为角色,value为该角色的权限Map；不存在返回空 Map（不返回 null）
     */
    Map<Object, Map<Object, Object>> getAuthorities(Set<String> loginRole);

    /**
     * 获取登录用户权限信息（支持项目维度）
     * <p>
     * 根据用户角色和项目编码获取对应的权限映射关系。
     * 用于按需加载权限场景，调用方显式传入 projectCode。
     * </p>
     * <p>
     * <b>默认实现不提供「忽略 projectCode 的回落」</b>：静默忽略项目维度会把 B 项目的权限
     * 错误地返回给请求 A 项目的调用方（越权放大），必须显式实现项目级查询。
     * </p>
     *
     * @param loginRole   用户登录的角色集合
     * @param projectCode 项目编码（client_id），如 "xiaoyue-web-client"，不允许为空
     * @return 权限信息Map,key为角色,value为该角色的权限Map；不存在返回空 Map（不返回 null）
     * @throws UnsupportedOperationException 未覆写本方法时抛出（禁止隐式回落到项目级查询）
     */
    default Map<Object, Map<Object, Object>> getAuthoritiesByProjectCode(Set<String> loginRole, String projectCode) {
        throw new UnsupportedOperationException(
                "getAuthoritiesByProjectCode 未实现：实现方必须按 projectCode 显式过滤权限，禁止回落到全项目聚合查询");
    }

    /**
     * 获取用户关联的所有 Token
     * <p>
     * 查询指定用户ID关联的所有有效 Token,用于单点登录、强制下线等场景。
     * </p>
     *
     * @param userId 用户ID
     * @return Token 集合(jti列表),不存在返回空 Set（不返回 null）
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
     * Set<String> roles = new HashSet<>();
     * roles.add("admin");
     * boolean hasAuth = loginInfoManager.hasAuth(roles, new String[]{"user:read"});
     * if (!hasAuth) {
     *     throw new SecurityException("权限不足");
     * }
     * }</pre>
     *
     * @param loginRole 用户登录的角色集合
     * @param authority 需要校验的权限标识数组（语义为 ANY-of：任一权限命中即通过）
     * @return true 表示拥有权限,false 表示没有权限；实现返回 Boolean 时调用方须按
     *         {@code Boolean.TRUE.equals(...)} 判定（null 视为无权限）
     */
    Boolean hasAuth(Set<String> loginRole, String[] authority);

}
