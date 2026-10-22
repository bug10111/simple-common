package com.simple.common.core.common.enums.process;

/**
 * 责任链处理器类型定义接口。
 * <p>
 * 用于定义责任链模式中处理器的执行顺序和类型映射。
 * 每个枚举值对应一个具体的处理器实现，通过 order 字段控制执行顺序。
 * </p>
 * <p>契约：{@link #getCode()} 作为配置key与缓存key前缀使用，必须稳定且唯一；
 * 默认返回枚举常量名（name()），禁止用含本地化语义的说明文案当key——
 * 实现方如需自定义key请显式覆盖 getCode()。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public enum AuthInterceptorKindProcess implements DefaultKindProcess {
 *     // (说明, 是否执行, 执行顺序) —— 顺序越小越先执行
 *     CHECK_TOKEN("Token校验", true, 1),
 *     CHECK_ROLE("角色校验", true, 2);
 *
 *     private final String msg;
 *     private final boolean execute;
 *     private final int order;
 *
 *     AuthInterceptorKindProcess(String msg, boolean execute, int order) {
 *         this.msg = msg;
 *         this.execute = execute;
 *         this.order = order;
 *     }
 *
 *     @Override
 *     public boolean isExecute() {
 *         return this.execute;
 *     }
 *
 *     @Override
 *     public int getOrdered() {
 *         return this.order;
 *     }
 *
 *     @Override
 *     public String getMsg() {
 *         return this.msg;
 *     }
 *     // getCode() 使用默认实现（枚举常量名），保证配置/缓存key稳定
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
     * 流程是否执行（false 仅跳过当前节点，不影响链上其余节点继续执行）
     */
    boolean isExecute();

    /**
     * 流程执行顺序（必须为正数且在同一责任链内唯一，值小者先执行）
     *
     * @return 执行顺序
     */
    int getOrdered();

    /**
     * 说明文案（仅用于展示，禁止作为配置/缓存key使用）
     */
    String getMsg();

    /**
     * 流程唯一编码（用作配置key与缓存key前缀，必须稳定且唯一）
     * <p>默认实现：实现方为枚举时返回枚举常量名（稳定、唯一）；非枚举实现请必须覆盖本方法。</p>
     */
    default String getCode() {
        if (this instanceof Enum) {
            return ((Enum<?>) this).name();
        }
        // 非枚举实现无稳定标识可用，退回说明文案并要求实现方覆盖
        return getMsg();
    }

}
