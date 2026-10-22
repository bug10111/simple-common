package com.simple.common.core.common.service.lock;

import cn.hutool.core.util.StrUtil;
import com.simple.common.core.common.properties.LockProperties;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public abstract class AbsLockService implements LockService {

    /**
     * 锁命名空间配置（protected：子类需在同一生命周期内使用；由容器注入）
     */
    @Autowired
    protected LockProperties lockProperties;

    private static final String KEY_SEPARATOR = ":";

    /**
     * 获取锁的key
     * <p>格式：{@code [bag:]methodName:key}（bag受 {@link LockProperties#isBag()} 开关控制，与缓存切面行为对齐）。
     * methodName与key之间强制使用分隔符，杜绝 {@code ("a","bc")} 与 {@code ("ab","c")} 拼出同一锁键导致的互相阻塞。</p>
     *
     * @param methodName 方法名称（不能为空）
     * @param key        业务键（不能为空）
     * @return 完整锁键
     */
    protected String getKey(String methodName, String key) {
        if (StrUtil.isBlank(methodName)) {
            throw new IllegalArgumentException("锁的methodName不能为空");
        }
        if (StrUtil.isBlank(key)) {
            throw new IllegalArgumentException("锁的key不能为空");
        }
        String prefix = lockProperties.isBag() ? lockProperties.getDefaultBag() + KEY_SEPARATOR : "";
        return prefix + methodName + KEY_SEPARATOR + key;
    }

}
