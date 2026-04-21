package com.simple.oauth.controller.sys.sysClientDetails;

import com.simple.common.core.response.R;
import com.simple.common.core.utils.Base64Utils;
import com.simple.common.core.utils.CryptoUtil;
import com.simple.common.core.utils.DateUtils;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.service.sysClientDetails.OauthClientDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

/**
 * 加密工具控制器
 * 提供RSA和AES加密解密功能，以及公钥获取
 *
 * @author qty
 */
@Slf4j
@Tag(name = "加密工具")
@RequestMapping("auth/key")
@RestController
@AllArgsConstructor
public class EncryptionController {

    private final Map<String, KeyPair> rsaKeyCache = new java.util.concurrent.ConcurrentHashMap<>();

    private final Map<String, String> aesKeyCache = new java.util.concurrent.ConcurrentHashMap<>();

    private final OauthProperties oauthProperties;

    private final OauthClientDetailsService clientDetailsService;

    /**
     * 获取RSA公钥
     * 前端登录时需要先调用此接口获取公钥用于加密密码
     *
     * @return Base64编码的公钥
     */
    @GetMapping
    @Operation(summary = "获取RSA公钥")
    public R<String> getPublicKey() {
        // 使用默认客户端ID获取密钥对
        String clientId = "default";
        KeyPair keyPair = rsaKeyCache.get(clientId);
        if (keyPair == null) {
            // 如果不存在，生成新的密钥对
            keyPair = CryptoUtil.generateKeyPair(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP);
            rsaKeyCache.put(clientId, keyPair);
            log.info("为客户端 {} 生成新的RSA密钥对", clientId);
        }
        PublicKey publicKey = keyPair.getPublic();
        // 将公钥转换为Base64编码
        String publicKeyBase64 = Base64Utils.encode(publicKey.getEncoded());
        return R.ok(publicKeyBase64);
    }

    /**
     * RSA加密
     *
     * @param clientId   客户端ID
     * @param encryptStr 待加密字符串
     * @return Base64编码的加密结果
     */
    @GetMapping("rsa/encrypt")
    @Operation(summary = "RSA加密")
    public R<String> rsaEncrypt(@RequestParam String clientId, @RequestParam String encryptStr) {
        // 添加时间戳防止重放攻击
        encryptStr = encryptStr + oauthProperties.getDecryptSplitStr() + DateUtils.parse(DateUtils.getNetworkDate()).getTime();

        // 获取客户端的密钥对
        KeyPair keyPair = rsaKeyCache.get(clientId);
        if (keyPair == null) {
            return R.error("未找到客户端密钥信息");
        }

        PublicKey publicKey = keyPair.getPublic();
        // 使用CryptoUtil进行RSA加密
        byte[] encrypted = CryptoUtil.encrypt(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP, publicKey, encryptStr.getBytes());
        // 使用Base64Utils编码
        return R.ok(Base64Utils.encode(encrypted));
    }

    /**
     * RSA解密
     *
     * @param clientId   客户端ID
     * @param encryptStr Base64编码的加密字符串
     * @return 解密后的原始字符串
     */
    @GetMapping("rsa/decrypt")
    @Operation(summary = "RSA解密")
    public R<String> rsaDecrypt(@RequestParam String clientId, @RequestParam String encryptStr) {
        // 获取客户端的密钥对
        KeyPair keyPair = rsaKeyCache.get(clientId);
        if (keyPair == null) {
            return R.error("未找到客户端密钥信息");
        }

        PrivateKey privateKey = keyPair.getPrivate();
        // 使用Base64Utils解码，然后使用CryptoUtil解密
        byte[] decrypted = CryptoUtil.decrypt(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP, privateKey, Base64Utils.decode(encryptStr));
        return R.ok(new String(decrypted));
    }

    /**
     * AES加密
     *
     * @param clientId   客户端ID
     * @param encryptStr 待加密字符串
     * @return Base64编码的加密结果
     */
    @GetMapping("aes/encrypt")
    @Operation(summary = "AES加密")
    public R<String> aesEncrypt(@RequestParam String clientId, @RequestParam String encryptStr) {
        // 添加时间戳
        encryptStr = encryptStr + oauthProperties.getDecryptSplitStr() + DateUtils.parse(DateUtils.getNetworkDate()).getTime();

        // 获取客户端的AES密钥
        String aesKey = aesKeyCache.get(clientId);
        if (aesKey == null || aesKey.isEmpty()) {
            return R.error("未找到客户端AES密钥信息");
        }

        // 使用CryptoUtil进行AES加密
        byte[] encrypted = CryptoUtil.encrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, aesKey.getBytes(), encryptStr.getBytes());
        // 使用Base64Utils编码
        return R.ok(Base64Utils.encode(encrypted));
    }

    /**
     * AES解密
     *
     * @param clientId   客户端ID
     * @param encryptStr Base64编码的加密字符串
     * @return 解密后的原始字符串
     */
    @GetMapping("aes/decrypt")
    @Operation(summary = "AES解密")
    public R<String> aesDecrypt(@RequestParam String clientId, @RequestParam String encryptStr) {
        // 获取客户端的AES密钥
        String aesKey = aesKeyCache.get(clientId);
        if (aesKey == null || aesKey.isEmpty()) {
            return R.error("未找到客户端AES密钥信息");
        }

        // 使用Base64Utils解码，然后使用CryptoUtil解密
        byte[] decrypted = CryptoUtil.decrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, aesKey.getBytes(), Base64Utils.decode(encryptStr));
        return R.ok(new String(decrypted));
    }

    /**
     * 获取客户端Authorization
     * 用于前端登录时获取客户端凭证
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return Base64编码的Authorization (clientId:clientSecret)
     */
    @GetMapping("client/token")
    @Operation(summary = "获取客户端Authorization")
    public R<String> getToken(@RequestParam String clientId, @RequestParam String clientSecret) {
        // 使用Base64Utils编码 clientId:clientSecret
        String authorization = Base64Utils.encode(clientId + ":" + clientSecret);
        return R.ok(authorization);
    }

}