package com.simple.common.mp.common.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.simple.common.mp.common.annotation.DataScopeTable;
import com.simple.common.mp.common.handler.DataScopeSqlHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限 SQL 拦截器（MyBatis Plus InnerInterceptor）
 * <p>
 * 使用 JSQLParser 解析 SQL AST，自动识别标注了 {@link DataScopeTable} 的表，
 * 调用 {@link DataScopeSqlHandler} 获取 WHERE 条件并注入 SQL。
 * 若无 {@link DataScopeSqlHandler} 实现，拦截器不生效。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class DataScopeInnerInterceptor implements InnerInterceptor {

    private final Map<Class<?>, DataScopeTable> annotationCache = new ConcurrentHashMap<>();
    private final DataScopeSqlHandler handler;

    /**
     * 构造数据权限拦截器
     *
     * @param handler 数据权限 SQL 处理器，由 Spring 条件注入
     */
    public DataScopeInnerInterceptor(DataScopeSqlHandler handler) {
        this.handler = handler;
    }

    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpStatementHandler = PluginUtils.mpStatementHandler(statementHandler);
        MappedStatement mappedStatement = mpStatementHandler.mappedStatement();

        SqlCommandType commandType = mappedStatement.getSqlCommandType();
        if (commandType != SqlCommandType.SELECT
                && commandType != SqlCommandType.UPDATE
                && commandType != SqlCommandType.DELETE) {
            return;
        }

        BoundSql boundSql = mpStatementHandler.boundSql();
        String originalSql = boundSql.getSql();

        Class<?> entityClass = getEntityClass(mappedStatement);
        if (entityClass == null) {
            return;
        }

        DataScopeTable annotation = getAnnotation(entityClass);
        if (annotation == null) {
            return;
        }

        try {
            String processedSql = processSql(originalSql, annotation);
            if (!processedSql.equals(originalSql)) {
                PluginUtils.mpBoundSql(boundSql).sql(processedSql);
                if (log.isDebugEnabled()) {
                    log.debug("数据权限过滤已生效，表: {}, SQL: {}", entityClass.getSimpleName(), processedSql);
                }
            }
        } catch (Exception e) {
            log.error("数据权限 SQL 拦截处理失败，表: {}", entityClass.getSimpleName(), e);
        }
    }

    private Class<?> getEntityClass(MappedStatement mappedStatement) {
        try {
            if (mappedStatement.getResultMaps() != null && !mappedStatement.getResultMaps().isEmpty()) {
                Class<?> type = mappedStatement.getResultMaps().get(0).getType();
                if (type != null && type != Object.class) {
                    return type;
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("无法获取 MappedStatement 对应的实体类: {}", e.getMessage());
            return null;
        }
    }

    private DataScopeTable getAnnotation(Class<?> entityClass) {
        return annotationCache.computeIfAbsent(entityClass, cls -> cls.getAnnotation(DataScopeTable.class));
    }

    private String processSql(String originalSql, DataScopeTable annotation) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(originalSql);

        if (statement instanceof Select select) {
            // 处理 SELECT 的主表和 JOIN 表
            if (select.getPlainSelect() != null) {
                processPlainSelect(select.getPlainSelect(), annotation);
            } else if (select.getSetOperationList() != null) {
                for (Select s : select.getSetOperationList().getSelects()) {
                    if (s.getPlainSelect() != null) {
                        processPlainSelect(s.getPlainSelect(), annotation);
                    }
                }
            }
        } else if (statement instanceof Update update) {
            processTable(update.getTable(), annotation, update::setWhere, update.getWhere());
        } else if (statement instanceof Delete delete) {
            processTable(delete.getTable(), annotation, delete::setWhere, delete.getWhere());
        }

        return statement.toString();
    }

    private void processPlainSelect(PlainSelect ps, DataScopeTable annotation) {
        // 处理 FROM 主表
        FromItem fromItem = ps.getFromItem();
        if (fromItem instanceof Table table) {
            Expression condition = handler.buildCondition(resolveAlias(table), annotation);
            if (condition != null) {
                Expression where = ps.getWhere();
                ps.setWhere(where == null ? condition : new AndExpression(where, condition));
            }
        }

        // 处理 JOIN 表
        if (ps.getJoins() != null) {
            for (Join join : ps.getJoins()) {
                FromItem rightItem = join.getRightItem();
                if (rightItem instanceof Table table) {
                    Expression condition = handler.buildCondition(resolveAlias(table), annotation);
                    if (condition != null) {
                        Expression where = ps.getWhere();
                        ps.setWhere(where == null ? condition : new AndExpression(where, condition));
                    }
                }
            }
        }
    }

    private void processTable(Table table, DataScopeTable annotation,
                              java.util.function.Consumer<Expression> whereSetter, Expression existingWhere) {
        Expression condition = handler.buildCondition(resolveAlias(table), annotation);
        if (condition != null) {
            whereSetter.accept(existingWhere == null ? condition : new AndExpression(existingWhere, condition));
        }
    }

    private static String resolveAlias(Table table) {
        if (table.getAlias() != null) {
            return table.getAlias().getName();
        }
        return table.getName();
    }
}
