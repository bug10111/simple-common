package com.simple.common.mp.interceptor;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.SimpleTypeRegistry;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collection;
import java.util.Map;

/**
 * 插入固定字段兜底填充拦截器
 *
 * <p>MyBatis-Plus 3.5.9 的 {@code MybatisParameterHandler} 仅在 {@code TableInfo.isWithInsertFill()=true}
 * 时调用容器内 {@link MetaObjectHandler#insertFill(MetaObject)}；未声明 fill 注解的实体因此拿不到
 * 固定字段兜底填充。本拦截器在 {@link Executor#update(MappedStatement, Object)} 执行前识别 INSERT
 * 语句的单实体参数，主动调用容器内处理器补齐固定字段，弥补该门控缺口。</p>
 *
 * <p>职责边界：仅处理 INSERT 且参数为单个 MyBatis-Plus 实体的场景；Map（含 {@code @Param} 参数表）、
 * 集合、数组、简单类型与非 MP 实体一律不介入。</p>
 *
 * <p>与 MyBatis-Plus 原生填充的关系：原生 {@code MybatisParameterHandler} 在 BoundSql 生成之后才执行
 * 插入填充，而 MP 的 INSERT 列清单在 BoundSql 生成时按字段策略确定——未声明 fill 注解且为空的字段会被
 * 整列省略。因此本拦截器必须在 BoundSql 生成前补齐固定字段，已声明部分 fill 注解（如仅时间字段）的实体
 * 同样需要本拦截器补齐全量固定字段；原生链路随后的二次填充因处理器「仅空值补齐」语义为空操作。</p>
 *
 * @author qty
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }))
public class InsertFillInterceptor implements Interceptor {

    private final ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider;

    /**
     * 构造插入填充拦截器。
     *
     * @param metaObjectHandlerProvider 容器内元对象填充处理器提供者
     */
    public InsertFillInterceptor(ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider) {
        this.metaObjectHandlerProvider = metaObjectHandlerProvider;
    }

    /**
     * 拦截执行器更新调用：INSERT 语句在参数进入 SQL 生成前完成固定字段兜底填充。
     *
     * @param invocation MyBatis 调用上下文
     * @return 原执行结果
     * @throws Throwable 原执行异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];

        // 仅 INSERT 语句需要插入填充，其余语句原样放行
        fillInsertEntity(mappedStatement, parameter);

        return invocation.proceed();
    }

    /**
     * 对 INSERT 的单实体参数执行插入填充。
     *
     * @param mappedStatement 映射语句
     * @param parameter       语句参数
     */
    @SuppressWarnings("deprecation")
    private void fillInsertEntity(MappedStatement mappedStatement, Object parameter) {
        if (SqlCommandType.INSERT != mappedStatement.getSqlCommandType()) {
            return;
        }

        Object entity = resolveSingleEntity(parameter);
        if (entity == null) {
            return;
        }

        TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());

        // 非 MyBatis-Plus 实体不参与固定字段兜底填充
        if (tableInfo == null) {
            return;
        }

        MetaObjectHandler metaObjectHandler = metaObjectHandlerProvider.getIfAvailable();

        // 容器内无处理器时安全跳过，由业务自行保证固定字段
        if (metaObjectHandler == null) {
            return;
        }

        // 处理器显式关闭插入填充时尊重其开关语义，新旧开关口径与 MyBatis-Plus 原生链路一致
        if (!metaObjectHandler.openInsertFill() || !metaObjectHandler.openInsertFill(mappedStatement)) {
            return;
        }

        Configuration configuration = mappedStatement.getConfiguration();
        MetaObject metaObject = configuration.newMetaObject(entity);
        metaObjectHandler.insertFill(metaObject);
    }

    /**
     * 解析单实体参数：Map、集合、数组、简单类型均不属于单实体场景。
     *
     * @param parameter 语句参数
     * @return 单实体对象，非单实体返回 null
     */
    private Object resolveSingleEntity(Object parameter) {
        if (parameter == null) {
            return null;
        }
        if (parameter instanceof Map || parameter instanceof Collection || parameter.getClass().isArray()) {
            return null;
        }
        if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
            return null;
        }
        return parameter;
    }
}
