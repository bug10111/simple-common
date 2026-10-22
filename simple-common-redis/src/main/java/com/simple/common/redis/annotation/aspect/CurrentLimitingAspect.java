package com.simple.common.redis.annotation.aspect;

import cn.hutool.core.util.StrUtil;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.redis.annotation.CurrentLimiting;
import com.simple.common.redis.common.enums.CurrentLimitingErrorEnum;
import com.simple.common.redis.common.manager.CurrentLimitingManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA
 * <p>
 * 限流切面：注解经切点绑定获取（而非 signature.getMethod() 反查），
 * 避免 JDK 代理下注解只标注在实现类时反查为 null 的 NPE；
 * Redis 异常时按 fail-open 语义降级放行，限流为辅助保护不反噬主链路。
 *
 * @author qty
 */
@Aspect
@Slf4j
@Component
public class CurrentLimitingAspect {

    @Autowired
    private CurrentLimitingManager currentLimitingManager;

    /**
     * 方法执行前限流检查
     * <p>
     * currentLimiting 由切点表达式绑定注入：接口代理场景下注解可能只标注在实现方法上，
     * 若按 signature.getMethod() 反射获取会拿到 null；绑定为 null 时（理论上不发生）安全放行。
     *
     * @param joinPoint        切点
     * @param currentLimiting  限流注解（切点绑定）
     */
    @Before("@annotation(currentLimiting)")
    public void before(JoinPoint joinPoint, CurrentLimiting currentLimiting) {

        //切点绑定兜底：注解不可见时放行，避免空引用中断业务请求
        if (currentLimiting == null) {
            return;
        }

        Boolean execute;
        try {
            execute = currentLimitingManager.execute(currentLimiting);
        } catch (Exception e) {
            //Redis/Redisson 不可用时 fail-open：放行请求保可用性，避免限流检查反噬主链路
            log.warn("限流检查异常，降级放行", e);
            return;
        }

        //实现返回 null（异常路径约定）时按未放行处理，避免拆箱 NPE
        if (!Boolean.TRUE.equals(execute)) {
            String str = currentLimiting.waitingTimeErrorStr();
            if (StrUtil.isNotBlank(str)) {
                AssertUtils.error(str);
            } else {
                AssertUtils.error(CurrentLimitingErrorEnum.ERROR);
            }
        }
    }
}
