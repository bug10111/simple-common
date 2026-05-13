package com.simple.common.auth.server.common.entity;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.simple.common.auth.client.common.constant.TokenConstant;
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
     * 更新数据
     *
     * @param saveInfo 内省数据
     * @param jtiOld   jti
     * @param atiOld   ati
     */
    public void refresh(Map<Object, Object> saveInfo, String jtiOld, String atiOld) {
        for (Object key : saveInfo.keySet()) {
            saveInfoMap.put(key.toString(), saveInfo.get(key).toString());
        }

        String jti = IdUtils.getFastUUID();
        String ati = IdUtils.getFastUUID();

        TokenPayload accessTokenPayload = new TokenPayload().setJti(jti)
                                                            .setExp(DateUtil.date().offset(DateField.SECOND, Integer.parseInt(saveInfoMap.get(TokenConstant.etKey).toString())).getTime())
                                                            .setAud(saveInfoMap.get(TokenConstant.clientIdKey).toString());
        accessTokenMap = BeanUtils.toMap(accessTokenPayload);

        TokenPayload refreshTokenPayload = new TokenPayload().setAti(ati)
                                                             .setExp(DateUtil.date().offset(DateField.SECOND, Integer.parseInt(saveInfoMap.get(TokenConstant.rEtKey).toString())).getTime())
                                                             .setJti(jti)
                                                             .setAud(saveInfoMap.get(TokenConstant.clientIdKey).toString());
        refreshTokenMap = BeanUtils.toMap(refreshTokenPayload);

        saveInfoMap.put(jti, "jti");
        saveInfoMap.put(ati, "ati");
        saveInfoMap.remove(jtiOld);
        saveInfoMap.remove(atiOld);
    }
}