package com.simple.common.mp.generator;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.simple.common.core.utils.IdUtils;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
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
     *
     * @param entity 实体
     */
    @Override
    public String nextUUID(Object entity) {
        return IdUtil.getSnowflakeNextIdStr();
    }
}
