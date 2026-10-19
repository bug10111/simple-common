package com.simple.mpfill.positive;

import com.simple.common.core.utils.IdUtils;
import com.simple.common.mp.common.enums.Status;
import com.simple.common.mp.handler.MybatisPlusOperationHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试用宿主固定字段填充处理器：模拟宿主 {@code HostMetaObjectHandler} 的
 * 「仅空值补齐、不覆盖显式值」语义，并以 {@code @Primary} 验证拦截器按主处理器选择。
 *
 * @author qty
 */
@Primary
@Component("mpFillTestPrimaryMetaObjectHandler")
public class PrimaryFillMetaObjectHandler extends MybatisPlusOperationHandler {

    /**
     * 测试租户ID
     */
    public static final String TENANT_ID = "test-tenant";

    /**
     * 测试创建人ID
     */
    public static final String CREATE_USER_ID = "test-user";

    /**
     * 插入填充调用次数，用于验证非 INSERT 语句不触发插入填充
     */
    private final AtomicInteger insertFillCount = new AtomicInteger();

    /**
     * 插入填充：先执行基座审计时间填充，再补齐缺失的固定字段。
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        super.insertFill(metaObject);

        insertFillCount.incrementAndGet();

        Date now = new Date();
        fillStringIfAbsent(metaObject, "id", "filled-" + IdUtils.getSnowflakeNextIdStr());
        fillStringIfAbsent(metaObject, "tenantId", TENANT_ID);
        fillStringIfAbsent(metaObject, "createUserId", CREATE_USER_ID);
        fillDateIfAbsent(metaObject, "createTime", now);
        fillDateIfAbsent(metaObject, "updateTime", now);
        fillStatusIfAbsent(metaObject);
        fillStringIfAbsent(metaObject, "reserve", "");
        fillStringIfAbsent(metaObject, "remark", "");
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

    private void fillStringIfAbsent(MetaObject metaObject, String fieldName, String value) {
        if (!metaObject.hasSetter(fieldName) || !metaObject.hasGetter(fieldName)) {
            return;
        }
        if (metaObject.getSetterType(fieldName) != String.class || metaObject.getValue(fieldName) != null) {
            return;
        }
        metaObject.setValue(fieldName, value);
    }

    private void fillDateIfAbsent(MetaObject metaObject, String fieldName, Date value) {
        if (!metaObject.hasSetter(fieldName) || !metaObject.hasGetter(fieldName)) {
            return;
        }
        if (!Date.class.isAssignableFrom(metaObject.getSetterType(fieldName)) || metaObject.getValue(fieldName) != null) {
            return;
        }
        metaObject.setValue(fieldName, value);
    }

    private void fillStatusIfAbsent(MetaObject metaObject) {
        if (!metaObject.hasSetter("status") || !metaObject.hasGetter("status")) {
            return;
        }
        if (metaObject.getSetterType("status") != Status.class || metaObject.getValue("status") != null) {
            return;
        }
        metaObject.setValue("status", Status.ON);
    }
}
