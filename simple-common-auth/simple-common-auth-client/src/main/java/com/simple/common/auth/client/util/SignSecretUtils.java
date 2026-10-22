package com.simple.common.auth.client.util;

import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA
 * 签名密钥管理工具类
 * <p>
 * 线程安全契约：secretRef 为 AtomicReference，读写在进程内可见；
 * 保存前做 trim + 长度校验，防止空白字符进入 HMAC 密钥造成跨节点验签不一致。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class SignSecretUtils {

    private static final AtomicReference<String> secretRef = new AtomicReference<>(null);

    /**
     * 保存签名密钥
     *
     * @param secret 签名密钥（≥32位；首尾空白会被去除后入库，纯空白输入直接拒绝）
     */
    public static void saveSecret(String secret) {
        AssertUtils.notEmpty(secret, "签名密钥不能为空");
        // ObjUtil.isNotEmpty 只判 null/长度，空白串（如 32 个空格）会漏过——必须 trim 后校验
        String trimmedSecret = secret.trim();
        AssertUtils.isTrue(!trimmedSecret.isEmpty() && trimmedSecret.length() >= 32,
                "签名密钥长度至少为32位，当前长度: " + trimmedSecret.length());
        secretRef.set(trimmedSecret);
        // 密钥属于进程级安全状态：替换必须留痕，便于排查轮换/误覆盖
        log.debug("签名密钥已更新（长度: {}）", trimmedSecret.length());
    }

    /**
     * 获取当前签名密钥
     *
     * @return 签名密钥
     * @throws IllegalStateException 当密钥未加载时抛出异常
     */
    public static String getSecret() {
        String secret = secretRef.get();
        if (secret == null) {
            throw new IllegalStateException("签名密钥未加载，请先调用saveSecret初始化");
        }
        return secret;
    }
}
