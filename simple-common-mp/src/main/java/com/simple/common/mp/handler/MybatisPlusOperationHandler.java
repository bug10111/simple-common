package com.simple.common.mp.handler;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
public class MybatisPlusOperationHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {

        //属性名
        this.strictInsertFill(metaObject, "createTime", Date.class, DateTime.now());
        this.strictInsertFill(metaObject, "updateTime", Date.class, DateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        //属性名
        this.strictInsertFill(metaObject, "updateTime", Date.class, DateTime.now());
    }
}
