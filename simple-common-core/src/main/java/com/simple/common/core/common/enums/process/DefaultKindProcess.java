package com.simple.common.core.common.enums.process;

/**
 * 责任链处理器类型定义接口。
 * <p>
 * 用于定义责任链模式中处理器的执行顺序和类型映射。
 * 每个枚举值对应一个具体的处理器实现类，通过 order 字段控制执行顺序。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public enum AuthInterceptorKindProcess implements DefaultKindProcess {
 *     CHECK_TOKEN(1, "Token校验", CheckTokenAuthProcess.class),
 *     CHECK_ROLE(2, "角色校验", CheckRoleAuthProcess.class),
 *     CHECK_SCOPE(3, "授权范围校验", CheckScopeAuthProcess.class);
 *
 *     private final int order;
 *     private final String msg;
 *     private final Class<? extends AuthProcess> processClass;
 *
 *     // 构造函数和getter方法...
 * }
 * }</pre>
 *
 * <h3>执行顺序控制：</h3>
 * <p>
 * order值越小，执行顺序越靠前。框架会按照order从小到大的顺序依次执行处理器。
 * </p>
 *
 * @author qty
 */
public interface DefaultKindProcess {

    /**
     * 流程是否执行
     */
    boolean isExecute();

    /**
     * 流程执行顺序
     */
    Integer getOrdered();

    /**
     * 说明
     */
    String getMsg();

    default String getCode(){
        return getMsg();
    }


}