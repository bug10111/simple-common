package com.simple.common.oauth.start.manager;

import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.core.utils.HttpUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.oauth.start.common.dto.SysUserByRoleKeyResponse;
import com.simple.common.oauth.start.common.entity.CreateUserRequest;
import com.simple.common.oauth.start.common.entity.RestUserRequest;
import com.simple.common.oauth.start.common.entity.SysUserInfoResponse;
import com.simple.common.oauth.start.common.entity.UpdateUserRequest;
import com.simple.common.oauth.start.common.enums.OauthUrl;
import com.simple.common.oauth.start.common.manager.UserManager;
import com.simple.common.oauth.start.common.properties.OauthStartProperties;
import com.simple.common.oauth.start.utils.HeadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
public class DefaultUserManager implements UserManager {

    @Autowired
    private OauthStartProperties oauthStartProperties;

    @Override
    public String create(CreateUserRequest createUserRequest) {
        Optional<R> post = Optional.empty();
        try {
            post = HttpUtils.post(oauthStartProperties.getUrl(OauthUrl.CREATE_USER), HeadUtils.getHead(), JsonUtils.toJsonStr(createUserRequest),
                                  oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "添加用户失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(),R.class).getMessage(), "", e);
        }
        return post.get().getData().toString();
    }

    @Override
    public SysUserInfoResponse findById(String id) {
        Optional<R> post = Optional.empty();
        try {
            post = HttpUtils.get(oauthStartProperties.getUrl(OauthUrl.SELECT_USER) + "/" + id, HeadUtils.getHead(), null, oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "查询用户失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(),R.class).getMessage(), "", e);
        }

        return JsonUtils.toJsonObj(post.get().getData().toString(), SysUserInfoResponse.class);
    }

    @Override
    public String findByName(String name) {
        Optional<R> post = Optional.empty();
        try {
            post = HttpUtils.get(oauthStartProperties.getUrl(OauthUrl.SELECT_USER_BY_NAME) + "/" + name, null, null, oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "查询用户失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(),R.class).getMessage(), "", e);
        }

        return post.get().getData().toString();
    }

    @Override
    public List<SysUserByRoleKeyResponse> findOneByRoleKey(String roleKey) {
        Optional<R> post = Optional.empty();
        try {
            post = HttpUtils.get(oauthStartProperties.getUrl(OauthUrl.SELECT_USER_BY_ROLE_KEY) + "/" + roleKey, HeadUtils.getHead(), null, oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "查询用户失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(),R.class).getMessage(), "", e);
        }

        return JsonUtils.toList(post.get().getData().toString(), SysUserByRoleKeyResponse.class);
    }

    @Override
    public void update(UpdateUserRequest updateSysUserRequest) {
        try {
            Optional<R> post = HttpUtils.post(oauthStartProperties.getUrl(OauthUrl.UPDATE_USER) + "/" + updateSysUserRequest.getId(), HeadUtils.getHead(),
                                              JsonUtils.toJsonStr(updateSysUserRequest), oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "修改用户失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(),R.class).getMessage(), "", e);
        }
    }

    @Override
    public void restPwd(RestUserRequest restUserRequest) {
        try {
            Optional<R> post = HttpUtils.post(oauthStartProperties.getUrl(OauthUrl.REST_USER) + "/" + restUserRequest.getId(), HeadUtils.getHead(),
                                              JsonUtils.toJsonStr(restUserRequest), oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "重置密码失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(),R.class).getMessage(), "", e);
        }

    }

    @Override
    public void delete(List<String> ids) {
        try {
            Optional<R> post = HttpUtils.delete(oauthStartProperties.getUrl(OauthUrl.DELETE_USER), HeadUtils.getHead(), JsonUtils.toJsonStr(ids),
                                                oauthStartProperties.getTimeOut(), R.class);
            post.orElseThrow(() -> new DefaultException(DefaultExceptionEnum.ERROR, "查询用户失败，没有正确的返回信息"));
        } catch (DefaultException e) {
            AssertUtils.error(JsonUtils.toJsonObj(e.getMessage(),R.class).getMessage(), "", e);
        }

    }


}
