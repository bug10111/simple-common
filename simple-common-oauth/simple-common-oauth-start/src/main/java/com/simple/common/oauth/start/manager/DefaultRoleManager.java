package com.simple.common.oauth.start.manager;

import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.oauth.start.common.dto.SysRoleInfoResponse;
import com.simple.common.oauth.start.common.entity.SysUserInfoResponse;
import com.simple.common.oauth.start.common.enums.OauthUrl;
import com.simple.common.oauth.start.common.manager.RoleManager;
import com.simple.common.oauth.start.common.properties.OauthStartProperties;
import com.simple.common.oauth.start.utils.HeadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
public class DefaultRoleManager implements RoleManager {

    @Autowired
    private OauthStartProperties oauthStartProperties;

    @Override
    public SysRoleInfoResponse findById(String id) {
        Optional<R> post = Optional.empty();
        try {
            post = HttpUtils.get(oauthStartProperties.getUrl(OauthUrl.SELECT_ROLE_BY_ID) + "/" + id, HeadUtils.getHead(), null, oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "查询用户失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(), R.class).getMessage(), "", e);
        }
        return JsonUtils.toJsonObj(post.get().getData().toString(), SysRoleInfoResponse.class);
    }

}
