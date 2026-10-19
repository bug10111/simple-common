package com.simple.mpfill.control;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 门控事实对照处理器：注册进对照组容器，用于实证 MyBatis-Plus 3.5.9 对
 * {@code TableInfo.isWithInsertFill()=false} 的实体不会调用 {@link #insertFill(MetaObject)}——
 * 处理器在场却零调用，才是门控事实本身，而非「容器内没有处理器」的弱对照。
 *
 * @author qty
 */
@Component
public class ControlGateMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入填充调用次数，对照组应为零
     */
    private final AtomicInteger insertFillCount = new AtomicInteger();

    /**
     * 插入填充：仅记录调用次数，用于验证门控未放行。
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        insertFillCount.incrementAndGet();
    }

    /**
     * 更新填充：对照组不涉及更新填充语义。
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
    }

    /**
     * 重置插入填充调用次数。
     */
    public void resetInsertFillCount() {
        insertFillCount.set(0);
    }

    /**
     * 获取插入填充调用次数。
     *
     * @return 调用次数
     */
    public int getInsertFillCount() {
        return insertFillCount.get();
    }
}
