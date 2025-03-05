package com.simple.common.auth.server.common.entity;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.simple.common.auth.client.common.constant.TokenConstant;
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
 * @author 兄台丶请冷静
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
    private Map<String, String> saveInfoMap = new HashMap<>();

    /**
     * 获取token载荷
     *
     * @param clientDetails  客户端信息
     * @param absUserDetails 用户信息
     */
    public void create(ClientDetails clientDetails, AbsUserDetails absUserDetails) {

        String jti = IdUtils.getFastUUID();
        String ati = IdUtils.getFastUUID();

        //构建生成accessToken的数据
        accessTokenMap.put(TokenConstant.jtiKey, jti);
        accessTokenMap.put(TokenConstant.expKey, DateUtil.date().offset(DateField.SECOND, clientDetails.getAccessTokenValidity()).getTime() + "");
        accessTokenMap.put(TokenConstant.audKey, clientDetails.getClientId());

        //构建生成refreshToken的数据
        refreshTokenMap.put(TokenConstant.atiKey, ati);
        refreshTokenMap.put(TokenConstant.expKey, DateUtil.date().offset(DateField.SECOND, clientDetails.getRefreshTokenValidity()).getTime() + "");
        refreshTokenMap.put(TokenConstant.jtiKey, jti);
        refreshTokenMap.put(TokenConstant.audKey, clientDetails.getClientId());

        //构建需要内省的数据
        saveInfoMap.put(jti, "jti");
        saveInfoMap.put(ati, "ati");
        saveInfoMap.put(TokenConstant.clientIdKey, clientDetails.getClientId());
        saveInfoMap.put(TokenConstant.clientNameKey, clientDetails.getClientName());
        saveInfoMap.put(TokenConstant.wxAppIdKey, clientDetails.getWxAppId());
        saveInfoMap.put(TokenConstant.appNamesKey, clientDetails.getResourceIds());
        saveInfoMap.put(TokenConstant.scopesKey, JsonUtils.toJsonStr(clientDetails.getScope()));
        saveInfoMap.put(TokenConstant.loginRole, JsonUtils.toJsonStr(absUserDetails.getLoginRole()));
        saveInfoMap.put(TokenConstant.userIdKey, absUserDetails.getUserId());
        saveInfoMap.put(TokenConstant.nicknameKey, absUserDetails.getNickname());
        saveInfoMap.put(TokenConstant.loginKey, absUserDetails.getLoginKey());
        saveInfoMap.put(TokenConstant.rEtKey, clientDetails.getRefreshTokenValidity() + "");
        saveInfoMap.put(TokenConstant.etKey, clientDetails.getAccessTokenValidity() + "");
        saveInfoMap.put(TokenConstant.extensionKey, JsonUtils.toJsonStr(absUserDetails.getExtension()));
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

        accessTokenMap.put(TokenConstant.jtiKey, jti);
        accessTokenMap.put(TokenConstant.expKey, DateUtil.date().offset(DateField.SECOND, Integer.parseInt(saveInfoMap.get(TokenConstant.etKey))).getTime() + "");
        accessTokenMap.put(TokenConstant.audKey, saveInfoMap.get(TokenConstant.clientIdKey));

        refreshTokenMap.put(TokenConstant.atiKey, ati);
        refreshTokenMap.put(TokenConstant.expKey,DateUtil.date().offset(DateField.SECOND, Integer.parseInt(saveInfoMap.get(TokenConstant.rEtKey))).getTime() + "");
        refreshTokenMap.put(TokenConstant.jtiKey, jti);
        refreshTokenMap.put(TokenConstant.audKey, saveInfoMap.get(TokenConstant.clientIdKey));

        saveInfoMap.put(jti, "jti");
        saveInfoMap.put(ati, "ati");
        saveInfoMap.remove(jtiOld);
        saveInfoMap.remove(atiOld);
    }
}
