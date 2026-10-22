package com.simple.common.annex.common.enums;

/**
 * Created with IntelliJ IDEA
 * Description: 文件存储类型。
 * <p>
 * 注意：常量名即外部配置契约——{@code simple.annex.type} 按 Spring 宽松绑定规则以枚举名取值,
 * MINIO/OSS 的常量名一旦改名或调整顺序将直接破坏既有配置,禁止改名。
 * </p>
 *
 * @author qty
 */
public enum AnnexType {
    MINIO, OSS
}
