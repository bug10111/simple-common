package com.simple.mpfill.unit;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.simple.common.mp.interceptor.InsertFillInterceptor;
import com.simple.mpfill.positive.PrimaryFillMetaObjectHandler;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 插入填充拦截器参数识别单元测试：验证 Map、集合、数组、简单类型与非 MP 实体被安全跳过，
 * 单实体按容器处理器填充，无处理器时安全跳过且不抛异常。
 *
 * @author qty
 */
class InsertFillInterceptorParameterTest {

    private MybatisConfiguration configuration;

    private PrimaryFillMetaObjectHandler primaryFillMetaObjectHandler;

    @BeforeEach
    void setUp() {
        configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, UnitProbeEntity.class);

        primaryFillMetaObjectHandler = new PrimaryFillMetaObjectHandler();
        primaryFillMetaObjectHandler.resetInsertFillCount();
    }

    @Test
    void fillsSingleEntityParameter() throws Throwable {
        InsertFillInterceptor interceptor = interceptorWithHandler();
        UnitProbeEntity entity = new UnitProbeEntity();

        invoke(interceptor, insertStatement(), entity);

        assertThat(primaryFillMetaObjectHandler.getInsertFillCount()).isEqualTo(1);
        assertThat(entity.getTenantId()).isEqualTo(PrimaryFillMetaObjectHandler.TENANT_ID);
        assertThat(entity.getCreateUserId()).isEqualTo(PrimaryFillMetaObjectHandler.CREATE_USER_ID);
    }

    @Test
    void skipsMapCollectionArrayAndSimpleParameters() throws Throwable {
        InsertFillInterceptor interceptor = interceptorWithHandler();
        MappedStatement insertStatement = insertStatement();

        invoke(interceptor, insertStatement, Map.of("name", "probe"));
        invoke(interceptor, insertStatement, List.of("probe"));
        invoke(interceptor, insertStatement, new String[] { "probe" });
        invoke(interceptor, insertStatement, "probe");

        assertThat(primaryFillMetaObjectHandler.getInsertFillCount()).isZero();
    }

    @Test
    void skipsNonMybatisPlusEntityParameter() throws Throwable {
        InsertFillInterceptor interceptor = interceptorWithHandler();

        invoke(interceptor, insertStatement(), new Object());

        assertThat(primaryFillMetaObjectHandler.getInsertFillCount()).isZero();
    }

    @Test
    void skipsNonInsertStatement() throws Throwable {
        InsertFillInterceptor interceptor = interceptorWithHandler();
        MappedStatement updateStatement = mock(MappedStatement.class);
        when(updateStatement.getSqlCommandType()).thenReturn(SqlCommandType.UPDATE);

        invoke(interceptor, updateStatement, new UnitProbeEntity());

        assertThat(primaryFillMetaObjectHandler.getInsertFillCount()).isZero();
    }

    @Test
    void skipsSafelyWhenNoHandlerPresent() throws Throwable {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ObjectProvider<MetaObjectHandler> provider = beanFactory.getBeanProvider(MetaObjectHandler.class);
        InsertFillInterceptor interceptor = new InsertFillInterceptor(provider);
        UnitProbeEntity entity = new UnitProbeEntity();

        Throwable thrown = catchThrowable(() -> invoke(interceptor, insertStatement(), entity));

        assertThat(thrown).isNull();
        assertThat(entity.getTenantId()).isNull();
    }

    private InsertFillInterceptor interceptorWithHandler() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("primaryFillMetaObjectHandler", primaryFillMetaObjectHandler);
        ObjectProvider<MetaObjectHandler> provider = beanFactory.getBeanProvider(MetaObjectHandler.class);
        return new InsertFillInterceptor(provider);
    }

    private MappedStatement insertStatement() {
        MappedStatement mappedStatement = mock(MappedStatement.class);
        when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.INSERT);
        when(mappedStatement.getConfiguration()).thenReturn(configuration);
        return mappedStatement;
    }

    private void invoke(InsertFillInterceptor interceptor, MappedStatement mappedStatement, Object parameter) throws Throwable {
        Executor executor = mock(Executor.class);
        Method method = Executor.class.getMethod("update", MappedStatement.class, Object.class);
        Invocation invocation = new Invocation(executor, method, new Object[] { mappedStatement, parameter });
        interceptor.intercept(invocation);
    }
}
