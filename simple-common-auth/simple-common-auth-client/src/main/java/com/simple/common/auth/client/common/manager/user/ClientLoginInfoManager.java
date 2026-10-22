package com.simple.common.auth.client.common.manager.user;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.login.DataPermission;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.permission.PermissionAutoLoader;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.exchange.http.AuthCenterHttpClient;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.common.service.jwt.CoreLoginUserService;
import com.simple.common.core.common.service.lock.LockService;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.function.ReturnValueFunction;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户端登录信息管理器。
 *
 * @author qty
 */
@Slf4j
@Component(value = LoginInfoManager.client_manager_name)
public class ClientLoginInfoManager implements LoginInfoManager, CoreLoginUserService {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private LockService lockService;

    @Autowired
    private AuthCenterHttpClient authCenterHttpClient;

    @Autowired
    private AuthProperties authProperties;

    /**
     * 权限自动加载器（可选）
     * 业务方实现后，当 auth-server 返回的权限数据不存在时，从本地数据库加载
     */
    @Autowired(required = false)
    private PermissionAutoLoader permissionAutoLoader;

    @Override
    public Map<Object, Object> getUserInfo(String key) {
        String userInfoKey = TokenConstant.getUserInfoKey(key);

        // 一级缓存命中直接返回，避免加锁开销
        Map<Object, Object> cached = cacheManager.hashGetAll(userInfoKey);
        if (!cached.isEmpty()) {
            return cached;
        }

        // 未命中时加锁从 auth-server 拉取，防止并发重复请求
        return lockAndFetch(userInfoKey, key);
    }

    /**
     * 加锁后二次检查缓存，若仍为空则从 auth-server 拉取用户信息并缓存。
     *
     * @param userInfoKey 用户信息缓存 key
     * @param jti         JWT Token 唯一标识
     * @return 用户信息 Map（不存在/获取失败时抛业务异常，不返回 null）
     */
    private Map<Object, Object> lockAndFetch(String userInfoKey, String jti) {
        ReturnValueFunction lockFunction = () -> {
            // 二次检查缓存，可能已被其他线程填充
            Map<Object, Object> userInfo = cacheManager.hashGetAll(userInfoKey);
            if (!userInfo.isEmpty()) {
                return userInfo;
            }

            // 调用 auth-server 获取用户信息并缓存
            return fetchAndCacheFromAuthServer(jti, userInfoKey);
        };

        Object locked = lockService.lockHaveValue(jti, lockFunction);
        if (locked == null) {
            // 锁回调未返回数据（获取/刷新失败）：明确业务异常而非把 null 抛给调用方 NPE
            AssertUtils.error(LoginException.LOGIN_EXPIRED, "用户信息缓存刷新失败");
            return null; // 不可达：error 必然抛出
        }
        @SuppressWarnings("unchecked")
        Map<Object, Object> result = (Map<Object, Object>) locked;
        return result;
    }

    /**
     * 从 auth-server 拉取用户信息、token 关联、权限数据，写入本地缓存。
     * <p>
     * 一次 HTTP 调用完成三类数据同步，减少网络往返。
     * 写入纪律：先完成全部解析与校验，再写缓存；每份数据写入后立即设置 TTL，
     * 避免中途异常留下「无过期时间的半成品缓存」被 getUserInfo 永久命中。
     * </p>
     *
     * @param jti         JWT Token 唯一标识
     * @param userInfoKey 用户信息缓存 key
     * @return 用户信息 Map
     */
    private Map<Object, Object> fetchAndCacheFromAuthServer(String jti, String userInfoKey) {
        // 调用 auth-server 内省接口（响应必须关闭，防止连接泄漏）
        int status;
        String body;
        try (HttpResponse httpResponse = authCenterHttpClient.getUserInfo()) {
            status = httpResponse.getStatus();
            body = httpResponse.body();
        }
        AssertUtils.notEmpty(body, LoginException.LOGIN_EXPIRED);

        // 先校验 HTTP 状态，再解析 body：非 200 的错误页/网关文本不应以 JSONException 形式逃逸
        R<?> r = null;
        try {
            r = JsonUtils.toJsonObj(body, R.class);
        } catch (Exception e) {
            log.warn("auth-server 内省响应非合法 JSON：status={}", status);
        }
        AssertUtils.isTrue(r != null && status == DefaultExceptionEnum.OK.getCodeInt(),
                LoginException.LOGIN_EXPIRED, r == null ? "auth-server 返回非 JSON 响应, status=" + status : r.getMessage());

        Object dataObj = r.getData();
        AssertUtils.notEmpty(dataObj, LoginException.LOGIN_EXPIRED, "auth-server 返回的 data 为空");
        Map<Object, Object> responseMap = JsonUtils.toJsonObj(dataObj.toString(), Map.class);

        // 解析 userInfo（全部解析完成并通过校验后才开始写缓存）
        Object userInfoObj = responseMap.get(TokenConstant.userInfoName);
        AssertUtils.notEmpty(userInfoObj, LoginException.LOGIN_EXPIRED, "auth-server 返回的 userInfo 为空");
        Map<Object, Object> userInfo = JsonUtils.toJsonObj(userInfoObj.toString(), Map.class);
        AssertUtils.notEmpty(userInfo, LoginException.LOGIN_EXPIRED, "auth-server 返回的 userInfo 为空对象");

        // userId 反序列化后可能是 Integer/Long：禁止强转 String（ClassCastException）
        Object userIdObj = userInfo.get(TokenConstant.userIdKey);
        AssertUtils.notEmpty(userIdObj, LoginException.LOGIN_EXPIRED, "auth-server 返回的 userId 为空");
        String userId = String.valueOf(userIdObj);

        // 过期时间先于任何写入解析（解析失败时不留下无 TTL 的半成品缓存）
        long expireSeconds = parseRetExpire(userInfo);

        // 写入用户信息缓存并立即设置过期时间
        cacheManager.hashPutAll(userInfoKey, userInfo);
        cacheManager.expire(userInfoKey, expireSeconds);

        // 解析 userToken 关联并缓存（写入后立即设置过期时间）
        String userTokenKey = TokenConstant.getUserTokenKey(userId);
        cacheUserTokens(responseMap, userTokenKey);
        cacheManager.expire(userTokenKey, expireSeconds);

        // 加载权限数据
        cacheAuthorities(responseMap, userInfo, expireSeconds);

        return userInfo;
    }

    /**
     * 从 auth-server 响应中解析 userToken 列表并写入缓存。
     *
     * @param responseMap  auth-server 响应数据
     * @param userTokenKey 用户 token 关联缓存 key
     */
    private void cacheUserTokens(Map<Object, Object> responseMap, String userTokenKey) {
        Object userTokenObj = responseMap.get(TokenConstant.userTokenName);
        AssertUtils.notEmpty(userTokenObj, LoginException.LOGIN_EXPIRED, "auth-server 返回的 userToken 为空");
        List<String> tokenList = JsonUtils.toList(userTokenObj.toString(), String.class);
        AssertUtils.notEmpty(tokenList, LoginException.LOGIN_EXPIRED, "auth-server 返回的 userToken 为空列表");
        cacheManager.setAdd(userTokenKey, tokenList.toArray(new String[0]));
    }

    /**
     * 加载权限数据：优先使用 auth-server 返回的权限，
     * 若为空则通过 PermissionAutoLoader 从本地数据库加载。
     *
     * @param responseMap   auth-server 响应数据
     * @param userInfo      用户信息 Map
     * @param expireSeconds 缓存过期时间（秒）
     */
    private void cacheAuthorities(Map<Object, Object> responseMap, Map<Object, Object> userInfo, long expireSeconds) {
        String projectCode = authProperties.getProjectCode();

        // 解析登录用户租户上下文（权限缓存键的租户维度来源）
        String tenantId = resolveTenantId(userInfo);
        // 优先使用 auth-server 返回的权限数据
        boolean hasAuthFromServer = loadAuthFromServerResponse(responseMap, projectCode, tenantId, expireSeconds);

        // auth-server 无权限数据时，从本地数据库加载
        if (!hasAuthFromServer && permissionAutoLoader != null) {
            loadAuthFromLocal(userInfo, projectCode, tenantId, expireSeconds);
        }
    }

    /**
     * 从 auth-server 响应中解析并缓存权限数据。
     *
     * @param responseMap   auth-server 响应数据
     * @param projectCode   项目编码
     * @param tenantId      租户ID（可能为 null）
     * @param expireSeconds 缓存过期时间
     * @return true 表示成功加载了权限，false 表示无权限数据
     */
    private boolean loadAuthFromServerResponse(Map<Object, Object> responseMap, String projectCode, String tenantId, long expireSeconds) {
        if (!responseMap.containsKey(TokenConstant.userAuthName)) {
            return false;
        }
        Object userAuthObj = responseMap.get(TokenConstant.userAuthName);
        if (userAuthObj == null) {
            return false;
        }
        Map<Object, Object> authMap = JsonUtils.toJsonObj(userAuthObj.toString(), Map.class);
        if (authMap == null || authMap.isEmpty()) {
            return false;
        }

        // 按角色维度缓存权限
        for (Map.Entry<Object, Object> entry : authMap.entrySet()) {
            // 角色权限为 null（无权限角色）是合法数据：跳过而非让 NPE 中断整个加载
            if (entry.getKey() == null || entry.getValue() == null) {
                log.warn("auth-server 返回的角色权限数据存在 null 键/值，已跳过：{}", entry.getKey());
                continue;
            }
            Map<Object, Object> auth = JsonUtils.toJsonObj(entry.getValue().toString(), Map.class);
            if (auth == null || auth.isEmpty()) {
                continue;
            }
            String authKey = TokenConstant.getAuthKey(entry.getKey().toString(), projectCode, tenantId);
            cacheManager.hashPutAll(authKey, auth);
            cacheManager.expire(authKey, expireSeconds);
        }

        log.info("已从 auth-server 加载并缓存 [{}] 下 {} 个角色的权限", projectCode, authMap.size());
        return true;
    }

    /**
     * 通过 PermissionAutoLoader 从本地数据库加载权限。
     *
     * @param userInfo      用户信息 Map
     * @param projectCode   项目编码
     * @param tenantId      租户ID（可能为 null）
     * @param expireSeconds 缓存过期时间
     */
    private void loadAuthFromLocal(Map<Object, Object> userInfo, String projectCode, String tenantId, long expireSeconds) {
        log.info("auth-server 未返回权限数据，尝试通过 PermissionAutoLoader 从本地数据库加载 projectCode=[{}]", projectCode);
        try {
            Object loginRoleObj = userInfo.get(TokenConstant.loginRole);
            if (loginRoleObj == null) {
                return;
            }
            Set<String> loginRoles = parseLoginRoles(loginRoleObj);
            for (String roleKey : loginRoles) {
                Map<String, String> perms = permissionAutoLoader.loadPermissions(roleKey, projectCode, tenantId);
                if (perms == null || perms.isEmpty()) {
                    continue;
                }
                String authKey = TokenConstant.getAuthKey(roleKey, projectCode, tenantId);
                cacheManager.hashPutAll(authKey, new HashMap<>(perms));
                cacheManager.expire(authKey, expireSeconds);
                log.debug("已通过 PermissionAutoLoader 缓存角色 [{}] 在项目 [{}] 租户 [{}] 下的 {} 个权限",
                        roleKey, projectCode, tenantId, perms.size());
            }
        } catch (Exception e) {
            log.error("通过 PermissionAutoLoader 加载权限失败", e);
        }
    }

    /**
     * 解析用户角色数据，兼容 Collection 与 JSON 字符串两种格式。
     *
     * @param loginRoleObj 角色数据对象
     * @return 角色标识集合（元素统一转 String，null 元素过滤）
     */
    @SuppressWarnings("unchecked")
    private Set<String> parseLoginRoles(Object loginRoleObj) {
        if (loginRoleObj instanceof Collection) {
            return ((Collection<?>) loginRoleObj).stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
        }
        List<String> roleList = JsonUtils.toList(loginRoleObj.toString(), String.class);
        if (roleList == null) {
            return Collections.emptySet();
        }
        return roleList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 从登录用户信息中解析租户上下文（权限缓存键的租户维度来源；无租户归属返回 null 走旧键格式）
     * <p>
     * dataPermission 的载体形态随缓存实现不同（Local 存对象 / Redis 存 JSON 字符串）：
     * 兼容 DataPermission 对象、Map、JSON 字符串三种形态，避免 Redis 实现下
     * {@code {tenantId=1}} 之类的 toString 产物解析失败导致静默丢失租户维度。
     * </p>
     *
     * @param userInfo 登录用户信息 Map
     * @return 租户 ID（可能为 null）
     */
    private String resolveTenantId(Map<Object, Object> userInfo) {
        Object dataPermissionObj = userInfo.get(TokenConstant.dataPermissionKey);
        if (dataPermissionObj == null) {
            return null;
        }
        try {
            Object tenantIdObj;
            if (dataPermissionObj instanceof DataPermission dataPermission) {
                tenantIdObj = dataPermission.getTenantId();
            } else if (dataPermissionObj instanceof Map) {
                tenantIdObj = ((Map<?, ?>) dataPermissionObj).get("tenantId");
            } else {
                Map<Object, Object> dataPermissionMap = JsonUtils.toJsonObj(dataPermissionObj.toString(), Map.class);
                tenantIdObj = dataPermissionMap == null ? null : dataPermissionMap.get("tenantId");
            }
            return tenantIdObj == null ? null : tenantIdObj.toString();
        } catch (Exception e) {
            // 解析失败不能静默吞掉租户维度：降级为 null 会导致写/读两侧键不一致（权限缓存跨租户错位），直接失败暴露
            throw new IllegalStateException("解析用户 dataPermission 获取 tenantId 失败", e);
        }
    }

    /**
     * 当前请求的租户上下文（权限缓存键的租户维度来源；无租户归属返回 null 走旧键格式）
     * <p>
     * 与 {@link #resolveTenantId(Map)} 保持同一租户解析来源链：ThreadLocal 中的
     * dataPermission 与 auth-server 返回的 dataPermission 同源（同一 token 载荷），
     * 双侧解析器均兼容对象/Map/JSON 字符串形态，保证写读键一致。
     * </p>
     *
     * @return 租户 ID（可能为 null）
     */
    private String currentTenantId() {
        DataPermission dataPermission = LoginUserUtils.getUserTemporary().getDataPermission();
        return dataPermission == null ? null : dataPermission.getTenantId();
    }

    @Override
    public Map<Object, Map<Object, Object>> getAuthorities(Set<String> loginRole) {
        String projectCode = authProperties.getProjectCode();

        Map<Object, Map<Object, Object>> map = new HashMap<>();
        if (loginRole != null) {
            String tenantId = currentTenantId();
            for (String role : loginRole) {
                Map<Object, Object> entries = cacheManager.hashGetAll(TokenConstant.getAuthKey(role, projectCode, tenantId));
                if (ObjUtil.isNotEmpty(entries)) {
                    map.put(role, entries);
                }
            }
        }
        return map;
    }

    @Override
    public Set<String> getUserToken(String userId) {
        return cacheManager.setMembers(TokenConstant.getUserTokenKey(userId));
    }

    @Override
    public String getUserId() {
        return LoginUserUtils.getUserTemporary().getUserId();
    }

    @Override
    public Boolean hasAuth(Set<String> loginRole, String[] authority) {
        if (authority == null || authority.length == 0) {
            return true;
        }

        String projectCode = authProperties.getProjectCode();

        // 直接通过 CacheManager 查询权限（由配置决定使用 Redis 或 Local）
        if (loginRole != null) {
            String tenantId = currentTenantId();
            for (String role : loginRole) {
                // 每个角色只取一次权限 Hash（原实现为 角色×权限 双层缓存往返）
                Map<Object, Object> roleAuth = cacheManager.hashGetAll(TokenConstant.getAuthKey(role, projectCode, tenantId));
                if (ObjUtil.isEmpty(roleAuth)) {
                    continue;
                }
                for (String perm : authority) {
                    if (roleAuth.containsKey(perm)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 从用户信息中解析缓存过期时间（秒）。
     * 若 auth-server 响应未携带 ret 字段，或取值非法（非数字/<=0），使用默认值 3600 秒。
     *
     * @param userInfo 用户信息 Map
     * @return 过期时间（秒，恒为正）
     */
    private long parseRetExpire(Map<Object, Object> userInfo) {
        Object retObj = userInfo.get(TokenConstant.rEtKey);
        if (retObj == null) {
            return 3600L;
        }
        try {
            long ret = Long.parseLong(retObj.toString().trim());
            // 0/负数会让 expire 立即删除缓存（登录数据永久失效）或语义不明，钳制回默认值
            return ret > 0 ? ret : 3600L;
        } catch (NumberFormatException e) {
            log.warn("auth-server 返回的 ret 非法，使用默认过期时间 3600s: {}", retObj);
            return 3600L;
        }
    }
}
