package com.simple.common.mp.generator;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.simple.common.core.utils.IdUtils;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 设计说明：nextId 与 nextUUID 复用 {@link IdUtils} 的同一雪花实例（workerId/datacenterId
 * 按内网 IP 派生），保证两种策略产出同一 ID 空间、彼此不碰撞。
 *
 * @author qty
 */
@Component
public class CustomIdGenerator implements IdentifierGenerator {

    /**
     * 雪花算法
     * <p>
     * 复用 IdUtils 的单例雪花实例，与 {@link #nextUUID(Object)} 共用同一 ID 空间。
     * 不使用 Hutool 全局默认实例：其 workerId 按 MAC/进程派生，容器多副本下可能重复，
     * 且与 IdUtils 实例之间无法保证互不碰撞。
     *
     * @param entity 实体
     */
    @Override
    public Number nextId(Object entity) {
        return Long.valueOf(IdUtils.getSnowflakeNextIdStr());
    }

    /**
     * 雪花算法
     * <p>
     * nextUUID 实际返回雪花 ID 字符串（非 UUID），雪花 ID 具备顺序性；ASSIGN_UUID 策略下落库为雪花字符串。
     * 这是本模块的显式设计契约：ASSIGN_UUID 实体的 id 列必须使用足够宽的字符类型（如 VARCHAR(32)），
     * 不要按 32 位标准 UUID 语义解析该值。
     *
     * @param entity 实体
     */
    @Override
    public String nextUUID(Object entity) {
        return IdUtils.getSnowflakeNextIdStr();
    }
}
