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
 * <p>职责边界：仅处理 INSERT 且参数为单个 MyBatis-Plus 实体的场景；集合、数组、简单类型与非 MP
 * 实体一律不介入。{@code @Param} 单实体入参（包装为 ParamMap）在剔除合成 paramN 键后仅剩单一
 * 候选时按单实体处理；多实体参数无法判定归属，安全跳过。</p>
 *
 * <p>与 MyBatis-Plus 原生填充的关系：原生 {@code MybatisParameterHandler} 在 BoundSql 生成之后才执行
 * 插入填充，而 MP 的 INSERT 列清单在 BoundSql 生成时按字段策略确定——未声明 fill 注解且为空的字段会被
 * 整列省略。因此本拦截器必须在 BoundSql 生成前补齐固定字段，已声明部分 fill 注解（如仅时间字段）的实体
 * 同样需要本拦截器补齐全量固定字段。</p>
 *
 * <p><b>处理器前置契约</b>：本拦截器与原生链路可能各调用一次 {@code insertFill}（本拦截器先于原生），
 * 处理器实现必须遵循「仅补空值」语义（如 strict 填充），禁止在 insertFill 中无条件生成或覆盖字段值
 * （如无条件取号、无条件置时间），否则同一行数据会被填充两次、且原生链路的第二次填充会覆盖本拦截器
 * 预填的值。仅空值补齐的处理器二次调用为幂等空操作。</p>
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

        // getIfUnique：候选不唯一（多处理器且未标 @Primary）时返回 null 而不是抛
        // NoUniqueBeanDefinitionException——此处位于每次 Executor.update 的热路径上，
        // 抛异常会导致所有 INSERT 失败，与「无处理器时安全跳过」的语义相悖
        MetaObjectHandler metaObjectHandler = metaObjectHandlerProvider.getIfUnique();

        // 容器内无处理器（或候选不唯一未消歧）时安全跳过，由业务自行保证固定字段
        if (metaObjectHandler == null) {
            return;
        }

        // 仅以语句感知的开关为准：openInsertFill(MappedStatement) 的默认实现委托给无参版本，
        // 只覆盖无参开关的处理器经委托语义同样生效；两关都查会把「仅按语句粒度开填充」的处理器误关
        if (!metaObjectHandler.openInsertFill(mappedStatement)) {
            return;
        }

        Configuration configuration = mappedStatement.getConfiguration();
        MetaObject metaObject = configuration.newMetaObject(entity);
        metaObjectHandler.insertFill(metaObject);
    }

    /**
     * 解析单实体参数：集合、数组、简单类型均不属于单实体场景；
     * {@code @Param} 单实体入参的 ParamMap 在剔除合成键后仅剩单一候选时按单实体处理。
     *
     * @param parameter 语句参数
     * @return 单实体对象，非单实体场景返回 null
     */
    private Object resolveSingleEntity(Object parameter) {
        if (parameter == null) {
            return null;
        }

        // @Param 入参包装为 MapperMethod.ParamMap（如 {"entity": x, "param1": x}）：
        // 剔除合成 paramN 键后仅剩单一候选时按单实体处理；0 个或多个候选无法判定归属，安全跳过
        if (parameter instanceof Map) {
            Object single = null;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) parameter).entrySet()) {
                if (entry.getKey() instanceof String && ((String) entry.getKey()).matches("param\\d+")) {
                    continue;
                }
                if (single != null) {
                    // 多于一个业务参数，无法判定填充归属
                    return null;
                }
                single = entry.getValue();
            }
            return single;
        }

        if (parameter instanceof Collection || parameter.getClass().isArray()) {
            return null;
        }
        if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
            return null;
        }
        return parameter;
    }
}
