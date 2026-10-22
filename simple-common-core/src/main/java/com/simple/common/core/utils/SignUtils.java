package com.simple.common.core.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.asymmetric.Sign;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA
 * Description: 签名工具类
 * <p>签名串契约：仅收集实例业务字段（排除 static/合成字段），null 值字段不参与签名，
 * 数组字段按 Arrays.deepToString 规范化；收发双方必须使用同一版本语义，否则验签不通过。</p>
 *
 * @author qty
 */
@Slf4j
public class SignUtils {

    /**
     * 生成API签名字符串（安全实现）
     *
     * @param t             目标对象（仅包含业务参数）
     * @param excludeFields 需要排除的字段名（如secret、password等）
     * @return 按参数名排序的签名字符串
     */
    public static <T> String generateSignStr(T t, String... excludeFields) {
        // 1. 获取需要参与签名的字段（排除指定字段）
        Map<String, String> params = getSignableFields(t, excludeFields);

        // 2. 按参数名ASCII码升序排序
        List<Map.Entry<String, String>> sortedParams = new ArrayList<>(params.entrySet());
        sortedParams.sort(Map.Entry.comparingByKey());

        // 3. 拼接成字符串
        return sortedParams.stream().map(e -> e.getKey() + "=" + urlEncode(e.getValue())).collect(Collectors.joining("&"));
    }

    /**
     * 获取可用于签名的字段
     *
     * @param t             对象
     * @param excludeFields 排除字段
     * @return 可参与签名的字段键值对（字段名 -> 字段值字符串；null值字段不参与签名）
     */
    private static <T> Map<String, String> getSignableFields(T t, String[] excludeFields) {
        Set<String> excludeSet = excludeFields == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(excludeFields));

        Map<String, String> params = new TreeMap<>(); // 自动按key排序

        // 获取当前类的所有字段
        Class<?> clazz = t.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                // static与编译器合成字段（this$0、$assertionsDisabled等）不属于业务参数，禁止进入签名
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic() || excludeSet.contains(field.getName())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(t);
                    // null值字段不参与签名（与""显式区分，防参数替换/签名碰撞）；数组按深度字符串规范化，避免identity hashCode导致对端无法复现
                    if (value != null) {
                        String strValue = value.getClass().isArray() ? arrayToString(value) : value.toString();
                        // 子类与父类同名字段时保留最派生值（先遍历到先占位）
                        params.putIfAbsent(field.getName(), strValue);
                    }
                } catch (ReflectiveOperationException e) {
                    // 签名字段不可读属于签名完整性问题：跳过会让收发双方签不同参数集，必须快速失败
                    throw new IllegalStateException("读取签名字段失败: " + field.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return params;
    }

    /**
     * 数组规范化字符串（支持嵌套数组），保证签名可复现
     */
    private static String arrayToString(Object array) {
        // 深度遍历数组内容（含多维数组），替代默认toString的 [I@hash 形式
        if (array instanceof Object[]) {
            return Arrays.deepToString((Object[]) array);
        }
        // 基本类型数组逐元素转字符串（deepToString不适用于基本类型数组）
        int length = java.lang.reflect.Array.getLength(array);
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(java.lang.reflect.Array.get(array, i));
        }
        return builder.append(']').toString();
    }

    /**
     * 签名，适用于服务端之间请求
     *
     * 计算哈希：对数据使用安全哈希算法生成摘要（当前加密方式均内置自动哈希计算）
     * 私钥签名：使用发送者的私钥对哈希值进行加密（签名）
     * 公钥验证：接收者使用发送者的公钥解密签名，得到哈希值
     * 比对验证：接收者计算数据的哈希值，与解密得到的哈希值比对
     *
     * @param algorithm  算法
     * @param privateKey 私钥
     * @param data       数据
     * @return 签名数据
     */
    public static byte[] sign(CryptoUtil.AsymmetricAlgorithmType algorithm, PrivateKey privateKey, byte[] data) {
        try {
            if (algorithm.isRsa()) {
                Sign sign = new Sign(algorithm.getSignAlgorithm());
                sign.setPrivateKey(privateKey);
                return sign.sign(data);
            } else if (algorithm == CryptoUtil.AsymmetricAlgorithmType.SM2) {
                SM2 sm2 = new SM2(privateKey, null);
                return sm2.sign(data);
            } else {
                throw new RuntimeException("不支持的签名算法: " + algorithm);
            }
        } catch (Exception e) {
            throw new RuntimeException("签名失败: " + algorithm, e);
        }
    }

    /**
     * 验签
     *
     * @param algorithm 方式
     * @param publicKey 公钥
     * @param data      数据
     * @param signData  签名数据
     * @return 验证结果
     */
    public static boolean verify(CryptoUtil.AsymmetricAlgorithmType algorithm, PublicKey publicKey, byte[] data, byte[] signData) {
        // 算法合法性在try外预检，避免被下方catch重新包装成"验签失败"混淆根因
        if (algorithm == null || (!algorithm.isRsa() && algorithm != CryptoUtil.AsymmetricAlgorithmType.SM2)) {
            throw new RuntimeException("不支持的验签算法: " + algorithm);
        }
        try {
            if (algorithm.isRsa()) {
                Sign sign = new Sign(algorithm.getSignAlgorithm());
                sign.setPublicKey(publicKey);
                return sign.verify(data, signData);
            }
            SM2 sm2 = new SM2(null, publicKey);
            return sm2.verify(data, signData);
        } catch (Exception e) {
            // 签名字节损坏等验签失败按false返回（供调用方按"签名无效"处理），算法/配置错误已在上方拦截
            log.warn("验签失败: {}", algorithm, e);
            return false;
        }
    }

    /**
     * 生成HMAC-SHA256签名，适用于web请求
     *
     * @param message   内容
     * @param secretKey 秘钥（可用没有“-”的uuid，建议每次请求更换秘钥，且和用户绑定）
     * @return 签名
     */
    public static String signWeb(String message, String secretKey) {
        return SecureUtil.hmacSha256(secretKey).digestBase64(message, true);
    }

    /**
     * 验证HMAC-SHA256签名
     * <p>
     * 使用常量时间比较，防止时序侧信道攻击逐字节猜测签名。
     * </p>
     *
     * @param message   内容
     * @param signature 签名字符串
     * @param secretKey 秘钥
     * @return 验签结果
     */
    public static boolean verifyWeb(String message, String signature, String secretKey) {
        // 空签名直接判定不通过，属正常入参校验
        if (signature == null) {
            return false;
        }
        String calculatedSignature = signWeb(message, secretKey);
        return MessageDigest.isEqual(calculatedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * URL编码（RFC 3986标准）
     * <p>
     * 对字符串进行URL编码，遵循RFC 3986标准，与主流API服务端保持一致。
     * 特殊处理：保留字符 ! ' ( ) ~ 不进行编码，空格编码为 %20 而非 +。
     * </p>
     *
     * <h3>编码规则：</h3>
     * <ul>
     *   <li>字母数字和 - _ . ~ 不编码</li>
     *   <li>空格编码为 %20（而非 +）</li>
     *   <li>保留字符 ! ' ( ) ~ 不编码</li>
     *   <li>其他特殊字符进行百分号编码</li>
     * </ul>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * String encoded = SignUtils.urlEncode("Hello World!");
     * // 结果: "Hello%20World!"
     * 
     * String encoded2 = SignUtils.urlEncode("test@example.com");
     * // 结果: "test%40example.com"
     * }</pre>
     *
     * @param value 待编码的字符串
     * @return URL编码后的字符串
     * @throws RuntimeException 当编码失败时抛出异常
     * @see java.net.URLEncoder#encode(String, String)
     */
    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20") // 保持与API服务端一致
                                      .replace("*", "%2A") // RFC 3986: * 属非保留字符的百分号编码口径，与对端规范化保持一致
                                      .replace("%21", "!").replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%7E", "~");
        } catch (Exception e) {
            throw new RuntimeException("URL编码失败", e);
        }
    }
}
