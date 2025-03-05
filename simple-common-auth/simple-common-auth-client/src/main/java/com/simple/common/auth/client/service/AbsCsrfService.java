package com.simple.common.auth.client.service;

import com.simple.common.auth.client.common.service.CsrfService;
import com.simple.common.core.common.service.lock.LockService;
import com.simple.common.core.function.DefaultFunction;
import com.simple.common.core.utils.AssertUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public abstract class AbsCsrfService implements CsrfService {

    @Autowired
    private LockService lockService;

    @Override
    public void checkToken(String userId, String path, String token) {
        DefaultFunction function = () -> {
            String saveToken = getToken(userId, path);
            AssertUtils.isTrue(saveToken.equals(token), "非法操作", "用户[{}]==>[{}] CSRF 防护失败, token不一致", userId, path);

            removeToken(userId, path);
        };

        lockService.lock(userId, function);
    }
}
