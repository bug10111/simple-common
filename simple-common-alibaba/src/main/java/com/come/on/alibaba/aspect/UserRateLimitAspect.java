package com.come.on.alibaba.aspect;

import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.simple.common.auth.client.util.LoginUserUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 统一用户限流 Aspect
 * <p>
 * 自动拦截所有子服务 Controller 方法，从 LoginUserUtils 获取 userId，
 * 调用 Sentinel 的 {@code SphU.entry()} 进行两级参数限流：
 * <ol>
 *   <li><b>接口级</b>（资源名 {@code {Controller}:{method}}）— 按 userId 维度，逐接口配置</li>
 *   <li><b>全局</b>（资源名 {@code user:rate:limit}）— 按 userId 维度，所有接口合计</li>
 * </ol>
 * 优先级：接口级 > 全局（Nacos 中配置了哪个就生效哪个，两者可叠加）。
 * IP 限流由 Gateway 层 {@code RealIpExtractionGlobalFilter} + Sentinel gw_flow 处理，此处不涉及。
 * </p>
 *
 * @author qty
 */
@Aspect
@Slf4j
@Component
public class UserRateLimitAspect {

    /** 全局统一资源名 — 按 userId 维度，所有接口合计限流 */
    private static final String RESOURCE_GLOBAL = "user:rate:limit";

    /**
     * 拦截所有子服务 Controller 方法，执行两级 userId 参数限流。
     * <p>
     * 优先级：接口级 > 全局。
     * 仅对已认证用户执行限流，匿名访问直接放行。
     * </p>
     */
    @Around("execution(* com.simple.*.controller.*.*(..))")
    public Object aroundControllerMethod(ProceedingJoinPoint pjp) throws Throwable {
        String userId = LoginUserUtils.getUserTemporary().getUserId();

        // 匿名访问，跳过限流
        if (StrUtil.isBlank(userId)) {
            return pjp.proceed();
        }

        String apiResource = buildApiResource(pjp);

        // 两级限流：接口级 > 全局（按优先级排列）
        // try-with-resources 确保每条 entry.close() 自动释放 Sentinel 上下文
        //noinspection unused
        try (Entry e1 = SphU.entry(apiResource, EntryType.IN, 1, userId);
             Entry e2 = SphU.entry(RESOURCE_GLOBAL, EntryType.IN, 1, userId)) {
            return pjp.proceed();
        } catch (BlockException e) {
            log.warn("用户 {} 触发限流, resource={}", userId, apiResource);
            throw new RuntimeException("请求已被限流，请稍后重试");
        }
    }

    private static String buildApiResource(ProceedingJoinPoint pjp) {
        return pjp.getTarget().getClass().getSimpleName() + ":" + pjp.getSignature().getName();
    }
}
