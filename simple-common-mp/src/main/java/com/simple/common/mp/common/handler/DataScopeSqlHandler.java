package com.simple.common.mp.common.handler;

import com.simple.common.mp.common.annotation.DataScopeTable;
import net.sf.jsqlparser.expression.Expression;

/**
 * 数据权限 SQL 处理器接口
 * <p>
 * mp 模块通过此接口将解析后的表信息传递给业务方，由业务方决定追加什么 WHERE 条件。
 * 实现此接口并注册为 Spring Bean 后，mp 自动启用数据权限拦截。
 * 若无任何实现，拦截器不生效，不影响正常使用。
 * </p>
 *
 * <h3>实现示例（auth-client 中）：</h3>
 * <pre>{@code
 * @Component
 * public class LoginUserDataScopeHandler implements DataScopeSqlHandler {
 *     &#64;Override
 *     public Expression buildCondition(String tableAlias, DataScopeTable annotation) {
 *         DataPermission dp = LoginUserUtils.getUserTemporary().getDataPermission();
 *         // ... 构建 JSQLParser Expression 返回
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
@FunctionalInterface
public interface DataScopeSqlHandler {

    /**
     * 构建数据权限 WHERE 条件表达式
     * <p>
     * 该方法在 SQL 解析完成后被调用，参数已确定表名/别名和注解配置。
     * 返回的 Expression 将通过 AND 合并到 SQL 的 WHERE 子句中。
     * </p>
     *
     * @param tableAlias SQL 中该表的别名（无别名时使用表名）
     * @param annotation 实体类上的 {@link DataScopeTable} 注解
     * @return JSQLParser Expression 对象，返回 null 表示不追加条件
     */
    Expression buildCondition(String tableAlias, DataScopeTable annotation);
}
