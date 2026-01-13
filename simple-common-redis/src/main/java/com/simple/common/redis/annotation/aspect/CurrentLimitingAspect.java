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
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Aspect
@Slf4j
@Component
public class CurrentLimitingAspect {

    @Autowired
    private CurrentLimitingManager<CurrentLimiting> currentLimitingManager;

    @Before("@annotation(com.simple.common.redis.annotation.CurrentLimiting)")
    public void around(JoinPoint joinPoint) {

        //获取注解先关参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CurrentLimiting currentLimiting = method.getAnnotation(CurrentLimiting.class);

        Boolean execute = currentLimitingManager.execute(currentLimiting);
        if (!execute) {
            String str = currentLimiting.waitingTimeErrorStr();
            if (StrUtil.isNotEmpty(str)) {
                AssertUtils.error(str);
            } else {
                AssertUtils.error(CurrentLimitingErrorEnum.ERROR);
            }
        }
    }
}
