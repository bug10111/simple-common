package com.simple.mpfill.positive;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.simple.common.mp.common.enums.Status;
import com.simple.common.mp.interceptor.InsertFillInterceptor;
import com.simple.mpfill.shared.entity.AnnotatedFillProbeEntity;
import com.simple.mpfill.shared.entity.FillProbeEntity;
import com.simple.mpfill.shared.mapper.AnnotatedFillProbeMapper;
import com.simple.mpfill.shared.mapper.FillProbeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 插入固定字段兜底填充集成测试：无 fill 注解实体经真实 BaseMapper.insert 走 H2 内存库，
 * 验证拦截器按 {@code @Primary} 处理器补齐固定字段、显式值不被覆盖、非 INSERT 不触发插入填充。
 *
 * @author qty
 */
@SpringBootTest(classes = FillPositiveTestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:mpfill_positive;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:fill-test-schema.sql",
        "simple.mp.db-type=H2"
})
class InsertFillInterceptorIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private FillProbeMapper fillProbeMapper;

    @Autowired
    private AnnotatedFillProbeMapper annotatedFillProbeMapper;

    @Autowired
    private InsertFillInterceptor insertFillInterceptor;

    @Autowired
    private MetaObjectHandler metaObjectHandler;

    @Autowired
    private PrimaryFillMetaObjectHandler primaryFillMetaObjectHandler;

    @Test
    void interceptorAndPrimaryHandlerAreWired() {
        // 拦截器与 @Primary 处理器均由容器装配
        assertThat(insertFillInterceptor).isNotNull();
        assertThat(metaObjectHandler).isInstanceOf(PrimaryFillMetaObjectHandler.class);
        assertThat(applicationContext.getBeanNamesForType(InsertFillInterceptor.class)).hasSize(1);
    }

    @Test
    void fillsAllFixedFieldsWhenAbsent() {
        FillProbeEntity entity = new FillProbeEntity();
        entity.setName("auto-fill-probe");

        // 真实 BaseMapper.insert 走 H2 内存库
        int rows = fillProbeMapper.insert(entity);

        assertThat(rows).isEqualTo(1);

        // 插入参数对象在进入 SQL 生成前已补齐固定字段
        assertThat(entity.getId()).isNotBlank();
        assertThat(entity.getTenantId()).isEqualTo(PrimaryFillMetaObjectHandler.TENANT_ID);
        assertThat(entity.getCreateUserId()).isEqualTo(PrimaryFillMetaObjectHandler.CREATE_USER_ID);
        assertThat(entity.getCreateTime()).isNotNull();
        assertThat(entity.getUpdateTime()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(Status.ON);
        assertThat(entity.getReserve()).isEmpty();
        assertThat(entity.getRemark()).isEmpty();

        // 库内落库结果与填充语义一致
        FillProbeEntity stored = fillProbeMapper.selectById(entity.getId());
        assertThat(stored).isNotNull();
        assertThat(stored.getTenantId()).isEqualTo(PrimaryFillMetaObjectHandler.TENANT_ID);
        assertThat(stored.getCreateUserId()).isEqualTo(PrimaryFillMetaObjectHandler.CREATE_USER_ID);
        assertThat(stored.getCreateTime()).isNotNull();
        assertThat(stored.getUpdateTime()).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(Status.ON);
        assertThat(stored.getReserve()).isEmpty();
        assertThat(stored.getRemark()).isEmpty();
    }

    @Test
    void keepsExplicitValuesWhenPresent() {
        Date explicitCreateTime = Date.from(Instant.parse("2026-01-02T03:04:05Z"));
        FillProbeEntity entity = new FillProbeEntity();
        entity.setId("explicit-probe-id");
        entity.setTenantId("explicit-tenant");
        entity.setRemark("explicit-remark");
        entity.setCreateTime(explicitCreateTime);
        entity.setStatus(Status.OFF);
        entity.setName("explicit-probe");

        fillProbeMapper.insert(entity);

        FillProbeEntity stored = fillProbeMapper.selectById("explicit-probe-id");
        assertThat(stored).isNotNull();

        // 显式赋值字段不被覆盖
        assertThat(stored.getId()).isEqualTo("explicit-probe-id");
        assertThat(stored.getTenantId()).isEqualTo("explicit-tenant");
        assertThat(stored.getRemark()).isEqualTo("explicit-remark");
        assertThat(stored.getCreateTime().getTime()).isEqualTo(explicitCreateTime.getTime());
        assertThat(stored.getStatus()).isEqualTo(Status.OFF);

        // 未赋值固定字段仍被补齐
        assertThat(stored.getCreateUserId()).isEqualTo(PrimaryFillMetaObjectHandler.CREATE_USER_ID);
        assertThat(stored.getUpdateTime()).isNotNull();
        assertThat(stored.getReserve()).isEmpty();
    }

    @Test
    void insertFillNotInvokedForUpdateStatement() {
        primaryFillMetaObjectHandler.resetInsertFillCount();

        FillProbeEntity entity = new FillProbeEntity();
        entity.setName("before-update");
        fillProbeMapper.insert(entity);
        int countAfterInsert = primaryFillMetaObjectHandler.getInsertFillCount();
        assertThat(countAfterInsert).isEqualTo(1);

        // UPDATE 语句不触发插入填充
        entity.setName("after-update");
        fillProbeMapper.updateById(entity);

        assertThat(primaryFillMetaObjectHandler.getInsertFillCount()).isEqualTo(countAfterInsert);
    }

    @Test
    void annotatedFillEntityUsesNativeFillOnce() {
        primaryFillMetaObjectHandler.resetInsertFillCount();

        AnnotatedFillProbeEntity entity = new AnnotatedFillProbeEntity();
        entity.setName("annotated-probe");

        annotatedFillProbeMapper.insert(entity);

        // 兜底拦截器在 BoundSql 生成前填充一次；MP 原生 ParameterHandler 随后二次调用，按「仅空值补齐」为空操作
        assertThat(primaryFillMetaObjectHandler.getInsertFillCount()).isEqualTo(2);
        assertThat(entity.getId()).isNotBlank();
        assertThat(entity.getTenantId()).isEqualTo(PrimaryFillMetaObjectHandler.TENANT_ID);
        assertThat(entity.getCreateTime()).isNotNull();
        assertThat(entity.getUpdateTime()).isNotNull();

        AnnotatedFillProbeEntity stored = annotatedFillProbeMapper.selectById(entity.getId());
        assertThat(stored).isNotNull();
        assertThat(stored.getCreateUserId()).isEqualTo(PrimaryFillMetaObjectHandler.CREATE_USER_ID);
        assertThat(stored.getStatus()).isEqualTo(Status.ON);
        assertThat(stored.getReserve()).isEmpty();
        assertThat(stored.getRemark()).isEmpty();
    }
}
