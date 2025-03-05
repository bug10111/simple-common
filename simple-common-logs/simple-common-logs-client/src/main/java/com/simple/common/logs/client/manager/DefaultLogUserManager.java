package com.simple.common.logs.client.manager;

import com.simple.common.logs.client.common.manager.LogUserManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class DefaultLogUserManager implements LogUserManager {

    @Override
    public String loginNickName() {
        log.error("请实现LogUserManager提供用户名和ID");
        return "测试用户名";
    }

    @Override
    public String loginUserId() {
        log.error("请实现LogUserManager提供用户名和ID");
        return "测试用户ID";
    }
}
