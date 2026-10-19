package com.simple.mpfill.control;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.simple.common.mp.common.config.MybatisPlusConfig;
import com.simple.mpfill.shared.entity.FillProbeEntity;
import com.simple.mpfill.shared.mapper.FillProbeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 门控事实回归测试：排除兜底拦截器但保留 {@link ControlGateMetaObjectHandler} 的对照配置下，
 * 无 fill 注解实体的固定字段不被填充、处理器零调用，真实 BaseMapper.insert 因非空约束失败。
 *
 * @author qty
 */
@SpringBootTest(classes = FillControlTestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:mpfill_control;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:fill-test-schema.sql",
        "spring.autoconfigure.exclude=" + "com.simple.common.mp.common.config.MybatisPlusConfig"
})
class InsertFillGateControlTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider;

    @Autowired
    private ControlGateMetaObjectHandler controlGateMetaObjectHandler;

    @Autowired
    private FillProbeMapper fillProbeMapper;

    @Test
    void insertWithoutInterceptorLeavesFixedFieldsUnfilled() {
        // 对照配置自证：处理器在场、兜底拦截器缺席
        assertThat(metaObjectHandlerProvider.getIfAvailable()).isInstanceOf(ControlGateMetaObjectHandler.class);
        assertThat(applicationContext.getBeanNamesForType(MybatisPlusConfig.class)).isEmpty();
        controlGateMetaObjectHandler.resetInsertFillCount();

        FillProbeEntity entity = new FillProbeEntity();
        entity.setName("control-probe");

        // 真实 BaseMapper.insert 触发库层非空约束失败
        Throwable thrown = catchThrowable(() -> fillProbeMapper.insert(entity));

        assertThat(thrown).isNotNull();
        assertThat(rootCauseMessage(thrown)).containsIgnoringCase("NULL not allowed");

        // 门控事实：处理器在容器内但 isWithInsertFill()=false，MyBatis-Plus 零调用 insertFill
        assertThat(controlGateMetaObjectHandler.getInsertFillCount()).isZero();

        // 固定字段保持为空，证明未发生任何兜底填充
        assertThat(entity.getId()).isNull();
        assertThat(entity.getTenantId()).isNull();
        assertThat(entity.getCreateUserId()).isNull();
        assertThat(entity.getCreateTime()).isNull();
        assertThat(entity.getUpdateTime()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getReserve()).isNull();
        assertThat(entity.getRemark()).isNull();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
