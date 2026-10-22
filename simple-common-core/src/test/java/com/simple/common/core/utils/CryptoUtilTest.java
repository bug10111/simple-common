package com.simple.common.core.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CryptoUtil 对称加密测试，覆盖 GCM 认证加密、旧格式兼容与 CBC 回归
 *
 * @author qty
 */
class CryptoUtilTest {

    /**
     * GCM 初始向量长度（字节）
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * GCM 认证标签长度（位）
     */
    private static final int GCM_TAG_LENGTH_BITS = 128;

    @Test
    void aesGcmRoundtripWithKeyLengths() {
        String plain = "AES_GCM 中文往返测试 abc123";
        byte[] plainBytes = plain.getBytes(StandardCharsets.UTF_8);

        // 逐一验证 16/24/32 字节密钥均能正确加解密
        for (int keyLength : new int[]{16, 24, 32}) {
            byte[] key = buildKey(keyLength);
            byte[] encrypted = CryptoUtil.encrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, plainBytes);

            // 密文长度必须为 IV + 明文 + 认证标签
            assertEquals(GCM_IV_LENGTH + plainBytes.length + GCM_TAG_LENGTH_BITS / 8, encrypted.length,
                    "密文长度不符合 iv||ciphertext+tag 格式, keyLength=" + keyLength);

            byte[] decrypted = CryptoUtil.decrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, encrypted);
            assertEquals(plain, new String(decrypted, StandardCharsets.UTF_8), "往返结果不一致, keyLength=" + keyLength);
        }
    }

    @Test
    void aesGcmRoundtripWithEmptyAndLargeData() {
        byte[] key = buildKey(16);

        // 空串应产生仅含 IV 与认证标签的密文，并能还原为空串
        byte[] emptyEncrypted = CryptoUtil.encrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, new byte[0]);
        assertEquals(GCM_IV_LENGTH + GCM_TAG_LENGTH_BITS / 8, emptyEncrypted.length);
        assertEquals(0, CryptoUtil.decrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, emptyEncrypted).length);

        // 大文本验证认证加密对非分块数据完整还原
        byte[] largeData = buildLargeData(1024 * 1024);
        byte[] largeEncrypted = CryptoUtil.encrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, largeData);
        assertArrayEquals(largeData, CryptoUtil.decrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, largeEncrypted));
    }

    @Test
    void sm4GcmRoundtrip() {
        byte[] key = buildKey(16);
        String plain = "SM4_GCM 国密往返测试";
        byte[] plainBytes = plain.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = CryptoUtil.encrypt(CryptoUtil.SymmetricAlgorithmType.SM4_GCM, key, plainBytes);
        assertEquals(GCM_IV_LENGTH + plainBytes.length + GCM_TAG_LENGTH_BITS / 8, encrypted.length);

        byte[] decrypted = CryptoUtil.decrypt(CryptoUtil.SymmetricAlgorithmType.SM4_GCM, key, encrypted);
        assertEquals(plain, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    void gcmTamperedCiphertextMustFail() {
        byte[] key = buildKey(16);
        byte[] encrypted = CryptoUtil.encrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key,
                "篡改校验".getBytes(StandardCharsets.UTF_8));

        // 翻转密文末字节破坏认证标签，解密必须因认证失败抛异常且不返回明文
        encrypted[encrypted.length - 1] ^= 0x01;
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> CryptoUtil.decrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, encrypted));
        assertTrue(rootCause(exception) instanceof AEADBadTagException,
                "篡改密文解密失败根因应为 AEADBadTagException，实际为 " + rootCause(exception).getClass().getName());
    }

    @Test
    void decryptLegacyIvPrefixedCiphertext() throws Exception {
        byte[] key = buildKey(16);
        byte[] plain = "旧格式 IV||ciphertext+tag 兼容".getBytes(StandardCharsets.UTF_8);
        byte[] iv = buildSequentialData(GCM_IV_LENGTH);

        // 使用标准 JCE 手工构造 iv||ciphertext+tag 的旧格式密文
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plain);
        byte[] legacyData = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, legacyData, 0, iv.length);
        System.arraycopy(ciphertext, 0, legacyData, iv.length, ciphertext.length);

        assertArrayEquals(plain, CryptoUtil.decrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, legacyData));
    }

    @Test
    void encryptOutputIsIvPrefixedStandardGcmCiphertext() throws Exception {
        byte[] key = buildKey(32);
        byte[] plain = "反向校验 iv||ciphertext+tag".getBytes(StandardCharsets.UTF_8);

        // 加密结果前 12 字节必须为 IV，其余部分可被标准 JCE 解密
        byte[] encrypted = CryptoUtil.encrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, plain);
        byte[] iv = Arrays.copyOfRange(encrypted, 0, GCM_IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(encrypted, GCM_IV_LENGTH, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        assertArrayEquals(plain, cipher.doFinal(ciphertext));
    }

    @Test
    void decryptLegacyCiphertextProducedWithIvParameterSpec() throws Exception {
        byte[] key = buildKey(16);
        byte[] iv = buildSequentialData(GCM_IV_LENGTH);

        // 旧实现经 BouncyCastle 以 IvParameterSpec 加密，新实现必须保持可解密
        assertLegacyRoundtrip(CryptoUtil.SymmetricAlgorithmType.AES_GCM, "AES", key, iv);
        assertLegacyRoundtrip(CryptoUtil.SymmetricAlgorithmType.SM4_GCM, "SM4", key, iv);
    }

    @Test
    void aesCbcRoundtrip() {
        byte[] key = buildKey(16);
        String plain = "AES_CBC 兼容路径回归";

        // CBC 路径行为必须保持不变
        byte[] encrypted = CryptoUtil.encrypt(CryptoUtil.SymmetricAlgorithmType.AES_CBC, key,
                plain.getBytes(StandardCharsets.UTF_8));
        byte[] decrypted = CryptoUtil.decrypt(CryptoUtil.SymmetricAlgorithmType.AES_CBC, key, encrypted);
        assertEquals(plain, new String(decrypted, StandardCharsets.UTF_8));
    }

    private static void assertLegacyRoundtrip(CryptoUtil.SymmetricAlgorithmType algorithm, String keyAlgorithm,
                                              byte[] key, byte[] iv) throws Exception {
        byte[] plain = ("旧实现密文兼容-" + keyAlgorithm).getBytes(StandardCharsets.UTF_8);

        // 使用 BouncyCastle 与 IvParameterSpec 复刻旧实现的加密方式
        Provider bcProvider = new BouncyCastleProvider();
        Cipher cipher = Cipher.getInstance(algorithm.getFullAlgorithm(), bcProvider);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, keyAlgorithm), new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plain);

        byte[] legacyData = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, legacyData, 0, iv.length);
        System.arraycopy(ciphertext, 0, legacyData, iv.length, ciphertext.length);
        assertArrayEquals(plain, CryptoUtil.decrypt(algorithm, key, legacyData));
    }

    private static byte[] buildKey(int length) {
        return buildSequentialData(length);
    }

    private static byte[] buildSequentialData(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (i + 1);
        }
        return data;
    }

    private static byte[] buildLargeData(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (i % 251);
        }
        return data;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
