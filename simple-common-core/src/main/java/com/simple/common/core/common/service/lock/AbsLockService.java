package com.simple.common.core.common.service.lock;

import com.simple.common.core.common.properties.LockProperties;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public abstract class AbsLockService implements LockService {

    @Autowired
    private LockProperties lockProperties;

    /**
     * 获取锁的key
     *
     * @param methodName 方法名称
     */
    protected String getKey(String methodName, String key) {
        return lockProperties.getDefaultBag() + ":" + methodName + key;
    }

}
