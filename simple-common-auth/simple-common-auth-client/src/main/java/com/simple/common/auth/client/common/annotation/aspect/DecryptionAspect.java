package com.simple.common.auth.client.common.annotation.aspect;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import com.simple.common.auth.client.common.annotation.Decryption;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
@Aspect
@Slf4j
public class DecryptionAspect {

    @Autowired
    private AuthProperties authProperties;

    @SneakyThrows
    @Around("@annotation(com.simple.common.auth.client.common.annotation.Decryption)")
    public Object around(ProceedingJoinPoint joinPoint) {

        //判断当前请求是否是get
        HttpServletRequest request = HttpServletUtils.getRequest();
        String method1 = request.getMethod();
        AssertUtils.isTrue("POST".equals(method1) || "PUT".equals(method1), "接口请求方式只能是POST或者PUT");

        //获取注解先关参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Decryption decryption = method.getAnnotation(Decryption.class);

        //获取方法参数值数组
        Object[] args = joinPoint.getArgs();
        AssertUtils.isTrue(args.length == 1, "只允许一个参数，多个参数请创建请求对象");

        //参数非空,开始执行解密
        if (ObjUtil.isNotEmpty(args)) {

            //获取客户端名称
            String clientName = LoginUserUtils.getUserTemporary().getClientId();
            if (StrUtil.isEmpty(clientName)) {
                String header = request.getHeader(TokenConstant.Authorization);
                String base64Token = header.trim().substring(6);
                String token = Base64.decodeStr(base64Token);
                boolean contains = StrUtil.contains(token, ":");
                AssertUtils.isTrue(contains, "请求头无效", "请求头无效 ==> [{}]", header);

                String[] split = token.split(":");
                AssertUtils.isTrue(split.length == 2, "请求头无效", "请求头无效 ==> [{}]", header);
                clientName = split[0];
            }

            //获取需要的参数
            String[] parameter = decryption.value();
            Object object = args[0];
            Class<?> clazz = object.getClass();

            //是否需要验签
            if (decryption.sign()) {

                //获取请求的签名
                String header = request.getHeader(authProperties.getDecryptSign());
                AssertUtils.isTrue(ObjUtil.isNotEmpty(header), "请求失败", "没有携带sign请求头");

                //计算签名
                String signStr = SignUtils.getSignStr(object);
                String sign = AlgorithmUtils.md5Hex(signStr);

                AssertUtils.isTrue(header.equals(sign), "请求失败", "签名验证失败");
            }

            //寻找需要解密的参数
            for (String string : parameter) {
                Field field = ClassUtils.getField(clazz, string);
                if (field == null) {
                    continue;
                }

                AssertUtils.isTrueParams(ObjUtil.isNotNull(field), "解密属性[{}]不能为空", string);

                if(field.get(object) != null) {
                    // 私有属性必须设置访问权限
                    field.setAccessible(true);

                    //获取完整解密字符串
                    String decrypt = RsaUtils.decryptStr(clientName, field.get(object).toString(), KeyType.PrivateKey);

                    //获取真正字符串
                    decrypt = getDecrypt(decrypt);

                    field.set(object, decrypt);
                }
            }
        }
        //执行请求
        return joinPoint.proceed(args);
    }

    /**
     * 获取真实字符串
     *
     * @param decrypt 解密后的字符串
     */
    protected String getDecrypt(String decrypt) {
        String[] split = decrypt.split(authProperties.getDecryptSplitStr());
        AssertUtils.isTrue(split.length == 2, "请按标准携带NTP服务毫秒级时间戳");

        //校验有效时间
        if (authProperties.getDecryptCheckValidityPeriod()) {
            DateTime begin = DateTime.of(Long.parseLong(split[1]));
            DateTime end = DateUtils.parse(DateUtils.getNetworkDate());
            long between = DateUtils.between(begin, end, DateUnit.MINUTE);
            AssertUtils.isTrue(between < authProperties.getDecryptValidityPeriod(), "解密失败", "解密失败！加密字符串已过期：[{}]==>当前时间[{}]==>加密时间[{}]",
                               decrypt, end.toString(), begin.toString());
        }

        return split[0];
    }
}
