package com.simple.common.auth.client.common.annotation.aspect;

import com.simple.common.auth.client.common.annotation.Sign;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.properties.SignProperties;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.core.utils.SignUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
@Aspect
@Slf4j
public class SignAspect {

    @Autowired
    private SignProperties signProperties;

    @Autowired
    private SignManager signManager;

    @SneakyThrows
    @Around("@annotation(com.simple.common.auth.client.common.annotation.Sign)")
    public Object around(ProceedingJoinPoint joinPoint) {

        //判断当前请求是否是get
        HttpServletRequest request = HttpServletUtils.getRequest();
        String method1 = request.getMethod();
        AssertUtils.isTrue("POST".equals(method1) || "PUT".equals(method1), "接口请求方式只能是POST或者PUT");

        //获取注解先关参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Sign signAnnotation = method.getAnnotation(Sign.class);

        // 如果注解存在且禁用签名，直接放行
        if (signAnnotation != null && !signAnnotation.enabled()) {
            return joinPoint.proceed();
        }
        assert signAnnotation != null;

        //获取方法参数值数组
        Object[] args = joinPoint.getArgs();
        AssertUtils.isTrue(args.length == 1, "只允许一个参数，多个参数请创建请求对象");

        // 4. 从请求头获取签名相关字段
        String signHeader = request.getHeader(signProperties.getSign());
        String timestampHeader = request.getHeader(signProperties.getTimestamp());
        String nonceHeader = request.getHeader(signProperties.getNonce());

        AssertUtils.notEmpty(signHeader, "签名头不能为空");
        AssertUtils.notEmpty(timestampHeader, "时间戳头不能为空");
        AssertUtils.notEmpty(nonceHeader, "随机数头不能为空");

        // 5. 时效性校验
        if (signAnnotation.checkTimestamp()) {
            signManager.checkTimestamp(timestampHeader);
        }
        // 6. 防重放校验
        if (signAnnotation.checkNonce()) {
            signManager.checkNonce(nonceHeader);
        }

        // 7. 获取当前登录用户（
        String userId = LoginUserUtils.getUserTemporary().getUserId();
        AssertUtils.notEmpty(userId, "用户未登录，无法进行签名校验");

        // 8. 获取签名密钥
        String secretKey = signManager.getKey();
        AssertUtils.notEmpty(secretKey, "未找到签名密钥");

        // 9. 构建待签名字符串（包含业务参数）
        String businessStr = buildBusinessString(joinPoint, signAnnotation);
        String message = businessStr + "&" + signProperties.getTimestamp() + "=" + timestampHeader + "&" + signProperties.getNonce() + "=" + nonceHeader;

        // 10. 验证签名
        boolean isValid = SignUtils.verifyWeb(message, signHeader, secretKey);
        AssertUtils.isTrue(isValid, "签名校验失败");

        //执行请求
        return joinPoint.proceed(args);
    }

    /**
     * 构建业务参数字符串（使用 SignUtils 提供的对象转签名串方法）
     *
     * @param joinPoint     切点
     * @param signAnnotation 签名注解
     * @return 业务参数签名串（已排序并 URL 编码）
     */
    private String buildBusinessString(ProceedingJoinPoint joinPoint, Sign signAnnotation) {
        Object[] args = joinPoint.getArgs();
        // 简单策略：取第一个非 Web 相关参数作为业务对象（实际可扩展）
        for (Object arg : args) {
            if (arg != null && !(arg instanceof HttpServletRequest)) {
                // 排除排除字段
                String[] excludeFields = signAnnotation.excludeFields();
                return SignUtils.generateSignStr(arg, excludeFields);
            }
        }
        // 若没有业务对象，则返回空字符串（仅用时间戳和随机数签名）
        return "";
    }

}
