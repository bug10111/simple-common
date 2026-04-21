package com.simple.oauth.manager.userName;

import cn.hutool.core.util.ObjUtil;
import com.simple.oauth.common.manager.username.SysUserNameCacheManager;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.view.sysUser.SysUserView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
public class DefaultSysUserNameCacheManager implements SysUserNameCacheManager {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OauthProperties oauthProperties;

    @Autowired
    private SysUserView sysUserView;

    @Override
    public void put(String userId, String userName) {
        stringRedisTemplate.opsForHash().put(oauthProperties.getUserNameCacheKey(), userId, userName);
    }

    @Override
    public String get(String userId) {
        if (ObjUtil.isEmpty(userId)) {
            return "";
        }

        Object obj = stringRedisTemplate.opsForHash().get(oauthProperties.getUserNameCacheKey(), userId);
        if (obj == null) {
            synchronized (this) {
                obj = stringRedisTemplate.opsForHash().get(oauthProperties.getUserNameCacheKey(), userId);
                if (obj == null) {
                    com.simple.oauth.common.entity.sysUser.SysUser byId = sysUserView.findById(userId);
                    if (ObjUtil.isNotEmpty(byId)) {
                        put(userId, byId.getNickname());
                        obj = byId.getNickname();
                    } else {
                        put(userId, "");
                        obj = "";
                    }
                }
            }
        }

        return obj.toString();
    }
}
