package com.simple.common.mp.handler;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 填充语义：insert 侧为 strict 填充（仅补空值，不覆盖显式赋值）；update 侧无条件刷新 updateTime。
 *
 * @author qty
 */
@Component
public class MybatisPlusOperationHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {

        //属性名：同一次插入的 createTime 与 updateTime 表达同一时刻，时间戳只取一次，避免毫秒级漂移
        Date now = DateTime.now();
        this.strictInsertFill(metaObject, "createTime", Date.class, now);
        this.strictInsertFill(metaObject, "updateTime", Date.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        //属性名：updateTime 必须无条件刷新——strict 填充仅在字段为 null 时生效，
        //而更新实体通常已携带旧值（读出后再改的场景），strictUpdateFill/strictInsertFill 都会导致
        //更新时间永不刷新；此处使用 setFieldValByName 覆盖为当前时间（insert 侧仍保持 strict 不覆盖显式值）
        this.setFieldValByName("updateTime", DateTime.now(), metaObject);
    }
}
