package com.simple.common.auth.client.common.manager.sign;

/**
 * Created with IntelliJ IDEA
 * Description: 签名相关接口
 *
 * @author qty
 */
public interface SignManager {

    /**
     * 生成秘钥
     */
    void generated();

    /**
     * 更新秘钥
     *
     * @param key 秘钥
     */
    void putKey(String key);

    /**
     * 获取秘钥
     *
     * @return 秘钥
     */
    String getKey();

    /**
     * 校验时效
     *
     * @param timestamp 时间戳
     */
    void checkTimestamp(String timestamp);

    /**
     * 防重放校验
     *
     * @param nonce 标志字段
     */
    void checkNonce(String nonce);

    /**
     * 生成HMAC-SHA256签名，适用于web请求
     *
     * @param message 内容
     * @return 签名
     */
    String signWeb(String message);

    /**
     * 验证HMAC-SHA256签名
     *
     * @param message   内容
     * @param signature 签名字符串
     * @return 验签结果
     */
    boolean verifyWeb(String message, String signature);

}
