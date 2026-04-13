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
        // 判断当前请求是否是 POST 或 PUT
        HttpServletRequest request = HttpServletUtils.getRequest();
        String method = request.getMethod();
        AssertUtils.isTrue("POST".equals(method) || "PUT".equals(method), "接口请求方式只能是 POST 或者 PUT");

        // 获取注解相关参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method methodObj = signature.getMethod();
        Sign signAnnotation = methodObj.getAnnotation(Sign.class);

        // 如果注解存在且禁用签名，直接放行
        if (signAnnotation != null && !signAnnotation.enabled()) {
            return joinPoint.proceed();
        }
        AssertUtils.notNull(signAnnotation, "签名注解不能为空");

        // 获取方法参数值数组
        Object[] args = joinPoint.getArgs();
        AssertUtils.isTrue(args.length == 1, "只允许一个参数，多个参数请创建请求对象");

        // 从请求头获取签名相关字段
        String signHeader = request.getHeader(signProperties.getSign());
        String timestampHeader = request.getHeader(signProperties.getTimestamp());
        String nonceHeader = request.getHeader(signProperties.getNonce());

        AssertUtils.notEmpty(signHeader, "签名不能为空");
        AssertUtils.notEmpty(timestampHeader, "时间戳不能为空");
        AssertUtils.notEmpty(nonceHeader, "随机数不能为空");

        // 时效性校验
        if (signAnnotation.checkTimestamp()) {
            signManager.checkTimestamp(timestampHeader);
        }
        // 防重放校验
        if (signAnnotation.checkNonce()) {
            signManager.checkNonce(nonceHeader);
        }

        // 获取当前登录用户
        String userId = LoginUserUtils.getUserTemporary().getUserId();
        AssertUtils.notEmpty(userId, "用户未登录，无法进行签名校验");

        // 获取签名密钥
        String secretKey = signManager.getKey();
        AssertUtils.notEmpty(secretKey, "未找到签名密钥");

        // 构建待签名字符串（包含业务参数）
        String businessStr = buildBusinessString(joinPoint, signAnnotation);
        String message = businessStr + "&" + signProperties.getTimestamp() + "=" + timestampHeader + "&" + signProperties.getNonce() + "=" + nonceHeader;

        // 验证签名
        boolean isValid = SignUtils.verifyWeb(message, signHeader, secretKey);
        AssertUtils.isTrue(isValid, "签名校验失败");

        // 执行请求
        return joinPoint.proceed(args);
    }

    /**
     * 构建业务参数字符串（使用 SignUtils 提供的对象转签名串方法）
     *
     * @param joinPoint      切点
     * @param signAnnotation 签名注解
     * @return 业务参数签名串（已排序并 URL 编码）
     */
    private String buildBusinessString(ProceedingJoinPoint joinPoint, Sign signAnnotation) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null && !(arg instanceof HttpServletRequest)) {
                String[] excludeFields = signAnnotation.excludeFields();
                return SignUtils.generateSignStr(arg, excludeFields);
            }
        }
        // 若没有业务对象，则返回空字符串
        return "";
    }
}