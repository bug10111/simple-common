package com.simple.common.annex.common.enums;

/**
 * Created with IntelliJ IDEA
 * Description: 内容摘要算法类型,仅用于文件指纹/去重(非加密、非安全用途),限定暂不支持扩展。
 * 持久化或反序列化得到的枚举值无法识别时,调用方应按未知算法处理(跳过指纹计算),禁止假设其非空。
 *
 * @author qty
 */
public enum Algorithm {

    /**
     * MD5 摘要。仅用于文件指纹/去重,存在碰撞风险,禁止用于完整性校验或安全场景。
     */
    MD5("MD5"),

    /**
     * SHA-256 摘要(推荐)。抗碰撞性优于 MD5,同样仅用于文件指纹/去重。
     */
    SHA_256("SHA-256");

    /**
     * JCE 标准摘要算法名称,可直接用于 MessageDigest.getInstance
     */
    private final String digestName;

    Algorithm(String digestName) {
        this.digestName = digestName;
    }

    public String getDigestName() {
        return digestName;
    }
}
