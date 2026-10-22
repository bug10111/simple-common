package com.come.on.alibaba.aspect;

import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.exception.DefaultException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 统一用户限流 Aspect
 * <p>
 * 拦截范围口径：凡被 {@code @RestController} 标注的 Controller 类，不限包名与子包深度，
 * 其全部方法均纳入限流切面；仅对已认证用户执行限流，匿名访问直接放行。
 * 从 LoginUserUtils 获取 userId，
 * 调用 Sentinel 的 {@code SphU.entry()} 进行两级参数限流：
 * <ol>
 *   <li><b>接口级</b>（资源名 {@code {Controller}:{method}(参数类型列表)}）— 按 userId 维度，逐接口配置</li>
 *   <li><b>全局</b>（资源名 {@code user:rate:limit}）— 按 userId 维度，所有接口合计</li>
 * </ol>
 * 优先级：接口级 > 全局（Nacos 中配置了哪个就生效哪个，两者可叠加）。
 * IP 限流由 Gateway 层 {@code RealIpExtractionGlobalFilter} + Sentinel gw_flow 处理，此处不涉及。
 * </p>
 *
 * <p><b>规则类型要求（重要）</b>：按 userId 维度隔离的前提是使用
 * <b>ParamFlowRule（热点参数规则）</b>且 {@code paramIdx = 0}（与 {@code SphU.entry}
 * 的 userId 入参绑定）。若按普通 FlowRule 配置，两条资源均为全体用户共享桶，
 * 单个用户的流量即可触发限流并拖累所有用户——请务必按 ParamFlowRule 口径下发规则。</p>
 *
 * @author qty
 */
@Aspect
@Slf4j
@Component
public class UserRateLimitAspect {

    /** 全局统一资源名 — 按 userId 维度，所有接口合计限流 */
    private static final String RESOURCE_GLOBAL = "user:rate:limit";

    /** 限流触发专用业务码，对齐 HTTP 429 语义，经 DefaultExceptionHandler 透出 */
    private static final String CODE_RATE_LIMITED = "429";

    /**
     * 拦截所有被 {@code @RestController} 标注的 Controller 方法（不限包名与子包深度），
     * 执行两级 userId 参数限流。
     * <p>
     * 优先级：接口级 > 全局。
     * 仅对已认证用户执行限流，匿名访问直接放行。
     * 切点使用注解匹配，避免包路径字面量漏配非标准包结构的 Controller。
     * </p>
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object aroundControllerMethod(ProceedingJoinPoint pjp) throws Throwable {
        String userId = LoginUserUtils.getUserTemporary().getUserId();

        // 匿名访问，跳过限流
        if (StrUtil.isBlank(userId)) {
            return pjp.proceed();
        }

        String apiResource = buildApiResource(pjp);

        // 两级限流：接口级 > 全局（按优先级排列）
        // try-with-resources 确保每条 entry.close() 自动释放 Sentinel 上下文；
        // 业务调用必须放在 catch(BlockException) 作用域之外：业务代码内部嵌套的
        // Sentinel 调用抛出的 BlockException 不应被本切面吞掉改写成 429——
        // 本切面只对「本切面的入口获取被拒」负责
        //noinspection unused
        try (Entry apiEntry = SphU.entry(apiResource, EntryType.IN, 1, userId);
             Entry globalEntry = SphU.entry(RESOURCE_GLOBAL, EntryType.IN, 1, userId)) {
            // 入口获取成功，业务调用移至作用域外执行
        } catch (BlockException e) {
            // 限流命中走框架统一异常出口,由 DefaultExceptionHandler 透出业务码与提示,避免被兜底为系统繁忙；
            // 记录实际命中的资源名（全局/接口级可区分），apiResource 仅为兜底展示
            String blockedResource = e.getRule() != null ? e.getRule().getResource() : apiResource;
            log.warn("用户 {} 触发限流, resource={}", userId, blockedResource);
            throw new DefaultException(CODE_RATE_LIMITED, "请求已被限流，请稍后重试");
        }

        return pjp.proceed();
    }

    /**
     * 构建接口级资源名
     * <p>
     * 取签名声明类型而非 {@code pjp.getTarget().getClass()}：后者在 CGLIB 代理下会返回
     * {@code XxxController$$EnhancerBySpringCGLIB$$xxx} 之类的不稳定类名，Nacos 规则将静默失配；
     * 拼接参数类型避免同名重载方法共享同一个限流桶。
     * </p>
     */
    private static String buildApiResource(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        return signature.getDeclaringType().getSimpleName() + ":" + signature.getName()
                + Arrays.stream(signature.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(",", "(", ")"));
    }
}
