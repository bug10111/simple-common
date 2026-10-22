package com.simple.common.auth.server.common.entity;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.utils.BeanUtils;
import com.simple.common.core.utils.IdUtils;
import com.simple.common.core.utils.JsonUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 记录token数据的对象
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "记录token的数据对象")
public class TokenData {

    @Schema(description = "token数据对象")
    private Map<String, Object> accessTokenMap = new HashMap<>();

    @Schema(description = "刷新token数据对象")
    private Map<String, Object> refreshTokenMap = new HashMap<>();

    @Schema(description = "内省数据对象")
    private Map<Object, Object> saveInfoMap = new HashMap<>();

    /**
     * 获取token载荷
     *
     * @param clientDetails  客户端信息
     * @param absUserDetails 用户信息
     */
    public void create(ClientDetails clientDetails, AbsUserDetails absUserDetails) {

        String jti = IdUtils.getFastUUID();
        String ati = IdUtils.getFastUUID();

        TokenPayload accessTokenPayload = new TokenPayload().setJti(jti)
                                                            .setExp(DateUtil.date().offset(DateField.SECOND, clientDetails.getAccessTokenValidity()).getTime())
                                                            .setAud(clientDetails.getClientId());
        accessTokenMap = BeanUtils.toMap(accessTokenPayload);

        TokenPayload refreshTokenPayload = new TokenPayload().setAti(ati)
                                                             .setExp(DateUtil.date().offset(DateField.SECOND, clientDetails.getRefreshTokenValidity()).getTime())
                                                             .setJti(jti)
                                                             .setAud(clientDetails.getClientId());
        refreshTokenMap = BeanUtils.toMap(refreshTokenPayload);

        TokenPayload saveInfoPayload = new TokenPayload().setClientId(clientDetails.getClientId())
                                                         .setClientName(clientDetails.getClientName())
                                                         .setWxAppId(clientDetails.getWxAppId())
                                                         .setAppNames(clientDetails.getResourceIds())
                                                         .setScopes(clientDetails.getScope())
                                                         .setLoginRole(absUserDetails.getLoginRole())
                                                         .setUserId(absUserDetails.getUserId())
                                                         .setNickname(absUserDetails.getNickname())
                                                         .setLoginKey(absUserDetails.getLoginKey())
                                                         .setRet(clientDetails.getRefreshTokenValidity())
                                                         .setEt(clientDetails.getAccessTokenValidity())
                                                         .setExtension(JsonUtils.toJsonStr(absUserDetails.getExtension()))
                                                         .setDataPermission(JsonUtils.toJsonStr(absUserDetails.getDataPermission()));

        Map<String, Object> payloadMap = BeanUtils.toMap(saveInfoPayload);
        for (Map.Entry<String, Object> entry : payloadMap.entrySet()) {
            saveInfoMap.put(entry.getKey(), entry.getValue());
        }
        saveInfoMap.put(jti, "jti");
        saveInfoMap.put(ati, "ati");
    }

    /**
     * 更新数据（刷新载荷）
     *
     * @param saveInfo 内省数据
     * @param jtiOld   jti
     * @param atiOld   ati
     */
    /**
     * 更新数据（刷新载荷）
     * <p>
     * 载荷拷贝采用类型保序策略：保留值原始类型（本地缓存回读为真实对象、Redis 回读为 JSON 串），
     * 键/值为 null 的条目跳过。此前对每个值强转 toString 会把 List/Set 压成 Java toString 形态
     * （非 JSON），刷新后回写缓存即污染下游 JSON 解析，且缓存存储形态随缓存后端实现漂移。
     * </p>
     *
     * @param saveInfo 内省数据
     * @param jtiOld   旧 jti
     * @param atiOld   旧 ati
     */
    public void refresh(Map<Object, Object> saveInfo, String jtiOld, String atiOld) {
        // 类型保序、跳过 null 的载荷拷贝
        for (Map.Entry<Object, Object> entry : saveInfo.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                saveInfoMap.put(entry.getKey().toString(), entry.getValue());
            }
        }

        // 必需载荷契约校验：缺失/非法一律受控地以登录失效失败，不落入 NPE/NumberFormatException 500
        Object etObj = saveInfoMap.get(TokenConstant.etKey);
        Object retObj = saveInfoMap.get(TokenConstant.rEtKey);
        Object clientIdObj = saveInfoMap.get(TokenConstant.clientIdKey);
        AssertUtils.notEmpty(etObj, LoginException.RE_LOGIN_EXPIRED, "token数据缺失et信息");
        AssertUtils.notEmpty(retObj, LoginException.RE_LOGIN_EXPIRED, "token数据缺失ret信息");
        AssertUtils.notEmpty(clientIdObj, LoginException.RE_LOGIN_EXPIRED, "token数据缺失clientId信息");
        int et;
        int ret;
        try {
            et = Integer.parseInt(etObj.toString());
            ret = Integer.parseInt(retObj.toString());
        } catch (NumberFormatException e) {
            throw new DefaultException(LoginException.RE_LOGIN_EXPIRED, "token有效期数据非法");
        }

        String jti = IdUtils.getFastUUID();
        String ati = IdUtils.getFastUUID();

        TokenPayload accessTokenPayload = new TokenPayload().setJti(jti)
                                                            .setExp(DateUtil.date().offset(DateField.SECOND, et).getTime())
                                                            .setAud(clientIdObj.toString());
        accessTokenMap = BeanUtils.toMap(accessTokenPayload);

        TokenPayload refreshTokenPayload = new TokenPayload().setAti(ati)
                                                             .setExp(DateUtil.date().offset(DateField.SECOND, ret).getTime())
                                                             .setJti(jti)
                                                             .setAud(clientIdObj.toString());
        refreshTokenMap = BeanUtils.toMap(refreshTokenPayload);

        saveInfoMap.put(jti, "jti");
        saveInfoMap.put(ati, "ati");
        saveInfoMap.remove(jtiOld);
        saveInfoMap.remove(atiOld);
    }
}
