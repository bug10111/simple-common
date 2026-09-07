package com.simple.common.mp.generator;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.simple.common.core.utils.IdUtils;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
public class CustomIdGenerator implements IdentifierGenerator {

    /**
     * 雪花算法
     *
     * @param entity 实体
     */
    @Override
    public Number nextId(Object entity) {
        return IdUtil.getSnowflakeNextId();
    }

    /**
     * 雪花算法
     * <p>
     * nextUUID 实际返回雪花 ID 字符串（非 UUID），雪花 ID 具备顺序性；ASSIGN_UUID 策略下落库为雪花字符串。
     *
     * @param entity 实体
     */
    @Override
    public String nextUUID(Object entity) {
        return IdUtils.getSnowflakeNextIdStr();
    }
}
