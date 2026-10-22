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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 签名校验切面。
 * <p>
 * 签名消息 = 业务参数串 + timestamp + nonce + userId + method + path：
 * 绑定用户身份与请求目标，防止跨用户重放与跨接口重放。
 * 签名顺序：先验签再消费 nonce，防止攻击者用非法签名消耗 nonce 池。
 * </p>
 *
 * @author qty
 */
@Component
@Aspect
@Slf4j
public class SignAspect {

    private static final AtomicBoolean DEFENSE_OFF_WARNED = new AtomicBoolean(false);

    @Autowired
    private SignProperties signProperties;

    @Autowired
    private SignManager signManager;

    @SneakyThrows
    @Around("@annotation(com.simple.common.auth.client.common.annotation.Sign)")
    public Object around(ProceedingJoinPoint joinPoint) {

        // 签名校验总开关：simple.sign.sign-defense=false 显式关闭（生效并告警一次，不再是无效果的死配置）
        if (!signProperties.isSignDefense()) {
            if (DEFENSE_OFF_WARNED.compareAndSet(false, true)) {
                log.warn("签名校验已通过 simple.sign.sign-defense=false 全局关闭，@Sign 接口不再验签");
            }
            return joinPoint.proceed();
        }

        HttpServletRequest request;
        try {
            request = HttpServletUtils.getRequest();
        } catch (NullPointerException e) {
            // 无请求上下文（异步线程/非Web调用）无法取头，签名校验无法成立，明确失败而非裸 NPE
            throw new IllegalStateException("当前上下文不是Web请求，无法进行签名校验");
        }

        // 获取注解相关参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method methodObj = signature.getMethod();
        Sign signAnnotation = methodObj.getAnnotation(Sign.class);

        // 注解存在且禁用签名：直接放行（先于请求方式断言，@Sign(enabled=false) 不受 POST/PUT 限制约束）
        if (signAnnotation != null && !signAnnotation.enabled()) {
            return joinPoint.proceed();
        }
        AssertUtils.notNull(signAnnotation, "签名注解不能为空");

        // 判断当前请求是否是 POST 或 PUT
        String method = request.getMethod();
        AssertUtils.isTrue("POST".equals(method) || "PUT".equals(method), "接口请求方式只能是 POST 或者 PUT");

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

        // 获取当前登录用户
        String userId = LoginUserUtils.getUserTemporary().getUserId();
        AssertUtils.notEmpty(userId, "用户未登录，无法进行签名校验");

        // 获取签名密钥
        String secretKey = signManager.getKey();
        AssertUtils.notEmpty(secretKey, "未找到签名密钥");

        // 构建待签名字符串（包含业务参数）
        String businessStr = buildBusinessString(joinPoint, signAnnotation);
        // 业务参数串为空 = 签名与业务数据无关，必须拒绝（防止空体绕过）
        AssertUtils.notEmpty(businessStr, "签名校验失败：业务参数签名为空");

        // 绑定用户身份、请求方式与请求目标，防止跨用户/跨接口重放（签名消息与 SignUtils 契约一致，客户端需同步升级签名串构造）
        String message = businessStr
                + "&" + signProperties.getTimestamp() + "=" + timestampHeader
                + "&" + signProperties.getNonce() + "=" + nonceHeader
                + "&userId=" + userId
                + "&method=" + method
                + "&path=" + request.getRequestURI();

        // 验证签名（必须先验签，再防重放，防止攻击者用错误签名消耗 nonce）
        boolean isValid = SignUtils.verifyWeb(message, signHeader, secretKey);
        AssertUtils.isTrue(isValid, "签名校验失败");

        // 签名校验通过后，再进行防重放校验
        if (signAnnotation.checkNonce()) {
            signManager.checkNonce(nonceHeader);
        }

        // 执行请求
        return joinPoint.proceed(joinPoint.getArgs());
    }

    /**
     * 构建业务参数字符串（使用 SignUtils 提供的对象转签名串方法）
     *
     * @param joinPoint      切点
     * @param signAnnotation 签名注解
     * @return 业务参数签名串（已排序并URL编码）
     * @throws IllegalArgumentException 若未找到有效业务参数对象
     */
    private String buildBusinessString(ProceedingJoinPoint joinPoint, Sign signAnnotation) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (Objects.nonNull(arg) && !(arg instanceof HttpServletRequest)) {
                String[] excludeFields = signAnnotation.excludeFields();
                return SignUtils.generateSignStr(arg, excludeFields);
            }
        }
        // 若没有有效的业务对象，则拒绝请求，防止签名绕过（空字符串会导致签名与业务数据无关）
        throw new IllegalArgumentException("签名校验失败：未找到有效的业务参数对象，请确保方法参数为非 HttpServletRequest 类型且不为 null");
    }
}
