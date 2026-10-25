package com.simple.common.auth.client.common.process;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.entity.login.DataPermission;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.enums.process.AuthInterceptorKindProcess;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import com.simple.common.auth.client.exchange.http.AuthCenterHttpClient;
import com.simple.common.auth.client.util.LoginInfoManagerUtils;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.BeanUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.core.utils.SignUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Token 合法性校验处理器。
 * <p>
 * <b>【重要安全提醒】</b>：此处理器包含一个内部服务间用户信息透传的机制。
 * 当请求来自可信网关（如Spring Cloud Gateway）时，允许通过请求头 {@code X-User-Context} 和 {@code X-User-Signature}
 * 直接传递用户信息，从而绕过JWT解析，提升性能。
 * <p>
 * 此机制的启用与否以及信任来源的判断逻辑，<b>必须由集成方根据自身部署架构来实现</b>。
 * 当前代码中的内部头校验部分是开放的，集成方需：
 * <ol>
 *   <li>替换 {@code SignManager} 为共享密钥实现，确保网关与微服务使用同一密钥。</li>
 * </ol>
 * 若未正确实施安全策略而直接使用，存在认证绕过风险。
 *
 * @author qty
 */
@Slf4j
@Component
public class CheckTokenAuthProcess implements AuthProcess {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private SignManager signManager;

    @Autowired
    private AuthCenterHttpClient authCenterHttpClient;

    @Override
    public DefaultKindProcess getProcess() {
        return AuthInterceptorKindProcess.CHECK_TOKEN;
    }

    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response, String token, String path, String ipAddr) {
        String encoded = request.getHeader(TokenConstant.userHead);
        String sign = request.getHeader(TokenConstant.userSignHead);
        // 密钥未加载时 getKey 抛出 IllegalStateException（部署错误必须显式失败，不允许未校验透传）
        String key = signManager.getKey();

        // 内部透传快速通道：恒定时间验签（防时序侧信道），解码/解析失败一律回落到常规 Token 校验（fail-closed）
        if (ObjUtil.isNotEmpty(encoded) && ObjUtil.isNotEmpty(sign)) {
            if (SignUtils.verifyWeb(encoded, sign, key)) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(encoded);
                    String userJson = new String(decoded, StandardCharsets.UTF_8);
                    UserTemporary user = JsonUtils.toJsonObj(userJson, UserTemporary.class);
                    // 仅在拿到合法用户对象时才短路；解析失败回落常规校验，不允许 null 进入登录上下文
                    if (user != null && ObjUtil.isNotEmpty(user.getUserId())) {
                        LoginUserUtils.add(user);
                        return;
                    }
                    log.warn("内部透传用户上下文不完整，回落到常规 Token 校验");
                } catch (RuntimeException e) {
                    log.warn("内部透传用户上下文解析失败，回落到常规 Token 校验", e);
                }
            }
        }

        Map<String, Object> payload = tokenManager.check(token, false);

        // 代管行事令牌分支（D2 同通道传递）：载荷行事域标识非空=delegation 签发的自包含行事令牌。
        // 行事令牌无认证域会话（login:info 内省恒空），按「吊销态判定+载荷自包含构造用户上下文」放行，
        // 业务应用经同一 Bearer 通道零感知；普通令牌（载荷无行事域标识）恒走既有内省链路，零回归。
        if (isActingToken(payload)) {
            loginActingUser(payload, token, path);
            return;
        }

        LoginInfoManager loginInfoManager;
        if (clientAuthInfo.getClient()) {
            loginInfoManager = LoginInfoManagerUtils.getClientLoginInfoManager();
        } else {
            loginInfoManager = LoginInfoManagerUtils.getServerLoginInfoManager();
        }
        AssertUtils.notNull(loginInfoManager, "LoginInfoManager 未正确初始化");

        // jti 单次提取并校验（载荷可能不含 jti，或载荷缺失被实现方容忍）
        Object jtiObj = payload.get(TokenConstant.jtiKey);
        AssertUtils.notEmpty(jtiObj, LoginException.LOGIN_EXPIRED, "Token载荷缺少 jti");
        String jti = jtiObj.toString();

        Map<Object, Object> userInfo = loginInfoManager.getUserInfo(jti);
        AssertUtils.notEmpty(userInfo, LoginException.LOGIN_EXPIRED);

        UserTemporary userTemporary = BeanUtils.fillBeanWithMap(userInfo, UserTemporary.class);
        userTemporary.setJti(jti);
        userTemporary.setPath(path);

        // 权限范围（List 元素逐个 String 化，缓存反序列化出 Integer 等类型时不产生 ClassCastException）
        if (ObjUtil.isNotEmpty(userInfo.get(TokenConstant.scopesKey))) {
            Object scopesObj = userInfo.get(TokenConstant.scopesKey);
            List<String> scopes;
            if (scopesObj instanceof Collection) {
                // 本地缓存或直接对象：已经是集合
                scopes = ((Collection<?>) scopesObj).stream().map(String::valueOf).collect(Collectors.toList());
            } else {
                // Redis 缓存：JSON 字符串格式 ["ALL"]
                scopes = JsonUtils.toList(scopesObj.toString(), String.class);
            }
            if (scopes != null) {
                userTemporary.setScopes(scopes);
            }
        }

        // 登录角色（Collection 与 JSON 字符串双形态兼容；元素逐个 String 化）
        if (ObjUtil.isNotEmpty(userInfo.get(TokenConstant.loginRole))) {
            Object loginRoleObj = userInfo.get(TokenConstant.loginRole);
            List<String> loginRole;
            if (loginRoleObj instanceof Collection) {
                // 本地缓存或直接对象：已经是集合
                loginRole = ((Collection<?>) loginRoleObj).stream().map(String::valueOf).collect(Collectors.toList());
            } else {
                // Redis 缓存：JSON 字符串格式 ["admin","user"]
                loginRole = JsonUtils.toList(loginRoleObj.toString(), String.class);
            }
            if (loginRole != null) {
                userTemporary.setLoginRole(loginRole);
            }
        }

        // 数据权限（Local 实现缓存的是对象、Redis 实现缓存的是 JSON 字符串：按形态分派，避免 toString 出 {tenantId=1} 伪 JSON）
        Object dataPermissionObj = userInfo.get(TokenConstant.dataPermissionKey);
        if (dataPermissionObj != null) {
            DataPermission dataPermission;
            if (dataPermissionObj instanceof DataPermission) {
                dataPermission = (DataPermission) dataPermissionObj;
            } else if (dataPermissionObj instanceof Map) {
                dataPermission = BeanUtils.fillBeanWithMap((Map<?, ?>) dataPermissionObj, DataPermission.class);
            } else {
                dataPermission = JsonUtils.toJsonObj(dataPermissionObj.toString(), DataPermission.class);
            }
            if (dataPermission != null) {
                userTemporary.setDataPermission(dataPermission);
            } else {
                log.warn("用户 dataPermission 解析结果为空，本次请求未加载数据权限：userId={}", userTemporary.getUserId());
            }
        }

        LoginUserUtils.add(userTemporary);
    }

    // ==================== 代管行事令牌分支（D2 同通道传递） ====================

    /**
     * 行事令牌识别：载荷行事域标识非空即为 delegation 签发的代管行事令牌。
     * <p>普通令牌（租户域/平台域登录令牌）载荷不含行事域标识（TokenPayload 载荷白名单
     * 按 null 属性剔除），恒返回 false 走既有会话内省链路。</p>
     *
     * @param payload 令牌载荷
     * @return true=代管行事令牌
     */
    private boolean isActingToken(Map<String, Object> payload) {
        Object actingDomain = payload.get(TokenConstant.actingDomainKey);
        return actingDomain != null && !actingDomain.toString().isBlank();
    }

    /**
     * 行事令牌登录：吊销态判定通过后，自载荷构造业务端用户上下文并放行后续链路。
     * <p>本地验签已在 {@code tokenManager.check} 完成（与登录令牌同密钥体系）；行事企业标识经
     * 数据权限槽位（dataPermission.tenantId）透传，RLS 通道按行事企业自然绑租户，消费点零改动。</p>
     *
     * @param payload 令牌载荷（行事三要素+数据权限槽位已随 JWT 自包含）
     * @param token   原始令牌（吊销态判定入参，不入日志）
     * @param path    请求路径
     */
    private void loginActingUser(Map<String, Object> payload, String token, String path) {
        // 吊销态判定：经授权中心既有 delegation 校验端点（签发域吊销键空间权威），fail-closed
        assertActingTokenValid(token);

        UserTemporary userTemporary = new UserTemporary()
                .setUserId(stringValue(payload, TokenConstant.userIdKey))
                .setNickname(stringValue(payload, TokenConstant.nicknameKey))
                .setLoginKey(stringValue(payload, TokenConstant.loginKey))
                .setJti(stringValue(payload, TokenConstant.jtiKey))
                .setPath(path)
                .setClientId(stringValue(payload, TokenConstant.clientIdKey))
                .setClientName(stringValue(payload, TokenConstant.clientNameKey))
                .setAppNames(stringValue(payload, TokenConstant.appNamesKey))
                .setWxAppId(stringValue(payload, TokenConstant.wxAppIdKey))
                .setDomain(resolveActingLoginDomain(payload))
                .setAuditMarker(stringValue(payload, TokenConstant.auditMarkerKey))
                .setActingDomain(stringValue(payload, TokenConstant.actingDomainKey));
        userTemporary.setSuperAdmin(Boolean.parseBoolean(stringValue(payload, TokenConstant.superAdminKey)));
        userTemporary.setScopes(stringCollection(payload.get(TokenConstant.scopesKey)));
        userTemporary.setLoginRole(stringCollection(payload.get(TokenConstant.loginRole)));
        userTemporary.setDataPermission(resolveActingDataPermission(payload));

        // 载荷不变量防御（fail-closed）：身份/会话标识缺失即拒绝，禁半成品上下文进入登录态
        AssertUtils.notEmpty(userTemporary.getJti(), LoginException.LOGIN_EXPIRED, "行事令牌载荷缺少 jti");
        AssertUtils.notEmpty(userTemporary.getUserId(), LoginException.LOGIN_EXPIRED, "行事令牌载荷缺少用户标识");
        AssertUtils.notEmpty(userTemporary.getLoginRole(), LoginException.INSUFFICIENT_PERMISSIONS, "行事令牌载荷缺少行事岗位");

        LoginUserUtils.add(userTemporary);
    }

    /**
     * 行事令牌吊销态判定：调用授权中心既有 delegation 校验端点。
     * <p>端点自校验（验签+有效期+吊销键空间 delegation:acting:revoke:{jti|ati}，fail-closed），
     * 是行事令牌吊销态的唯一权威来源——吊销键空间归属签发域缓存（Redis 独立逻辑库/键空间），
     * 业务实例缓存（LOCAL 或独立逻辑库）不承载该键空间，禁止本地臆断；本调用每次实时判定，
     * 不缓存校验结果（缓存会延迟登出吊销的可见性）。令牌仅进请求体，不入日志。</p>
     *
     * @param token 代管行事令牌
     */
    private void assertActingTokenValid(String token) {
        int status;
        String body;
        try (HttpResponse httpResponse = authCenterHttpClient.validateActingToken(token)) {
            status = httpResponse.getStatus();
            body = httpResponse.body();
        } catch (Exception e) {
            // 传输层失败 fail-closed：吊销态不可判定=不放行（授权中心暂不可达时禁盲放行行事令牌）
            log.warn("行事令牌吊销态校验调用授权中心失败（fail-closed 拒绝）", e);
            AssertUtils.error(LoginException.LOGIN_EXPIRED, "行事令牌校验服务不可用，请稍后重试");
            return;
        }

        // 先校验 HTTP 状态再取数：非 200 的错误页/网关文本不应以 JSONException 形态逃逸
        R<?> r = null;
        try {
            r = JsonUtils.toJsonObj(body, R.class);
        } catch (Exception e) {
            log.warn("授权中心行事令牌校验响应非合法 JSON：status={}", status);
        }
        AssertUtils.isTrue(r != null && status == DefaultExceptionEnum.OK.getCodeInt(),
                LoginException.LOGIN_EXPIRED,
                r == null ? "授权中心行事令牌校验响应异常, status=" + status : r.getMessage());
        AssertUtils.notEmpty(r.getData(), LoginException.LOGIN_EXPIRED, "授权中心行事令牌校验无返回数据");

        Map<String, Object> result = JsonUtils.toJsonObj(r.getData().toString(), Map.class);
        boolean valid = result != null && Boolean.parseBoolean(String.valueOf(result.get("valid")));
        if (!valid) {
            Object actingStatus = result == null ? null : result.get("status");
            Object message = result == null ? null : result.get("message");
            log.info("行事令牌校验未通过：status={}, message={}", actingStatus, message);
            AssertUtils.error(LoginException.LOGIN_EXPIRED, "行事令牌已失效（" + actingStatus + "），请重新签发");
        }
    }

    /**
     * 行事令牌数据权限槽位解析（RLS 行事企业绑定锚点）。
     * <p>槽位缺失/解析失败/与行事企业标识不一致一律拒绝（fail-closed）：无租户锚点的行事上下文
     * 会导致 RLS 通道盲绑或拒绑，不允许进入登录态。</p>
     *
     * @param payload 令牌载荷
     * @return 数据权限（tenantId=行事企业）
     */
    private DataPermission resolveActingDataPermission(Map<String, Object> payload) {
        Object dataPermissionObj = payload.get(TokenConstant.dataPermissionKey);
        DataPermission dataPermission = dataPermissionObj == null
                ? null : JsonUtils.toJsonObj(dataPermissionObj.toString(), DataPermission.class);
        AssertUtils.isTrue(dataPermission != null && ObjUtil.isNotEmpty(dataPermission.getTenantId()),
                LoginException.LOGIN_EXPIRED, "行事令牌数据权限槽位缺失或非法");

        // 行事企业一致性防御：数据权限槽位租户与载荷行事企业标识同值（签发侧不变量）
        String actingTenantId = stringValue(payload, TokenConstant.actingTenantIdKey);
        AssertUtils.isTrue(actingTenantId != null && actingTenantId.equals(dataPermission.getTenantId()),
                LoginException.LOGIN_EXPIRED, "行事令牌数据权限槽位与行事企业标识不一致");
        return dataPermission;
    }

    /**
     * 行事令牌身份归属域：载荷 domain 字段优先（TokenPayload 白名单字段），
     * 行事签发不落 domain 时按行事域结构化推导为平台域（与平台控制台登录域标识同口径：固定值 platform）。
     *
     * @param payload 令牌载荷
     * @return 身份归属域
     */
    private String resolveActingLoginDomain(Map<String, Object> payload) {
        Object domain = payload.get(TokenConstant.domainKey);
        if (domain != null && !domain.toString().isBlank()) {
            return domain.toString();
        }
        return "platform";
    }

    /**
     * 载荷集合值解析（JWT 解析 List/字符串 JSON 双形态兼容，元素统一 String 化）。
     *
     * @param value 载荷值
     * @return 字符串集合（空值返回 null，由消费方按缺失处理）
     */
    private Collection<String> stringCollection(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
        }
        return JsonUtils.toList(value.toString(), String.class);
    }

    /**
     * 载荷值取字符串（null 安全）。
     *
     * @param payload 令牌载荷
     * @param key     载荷键
     * @return 字符串值（缺失返回 null）
     */
    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }
}
