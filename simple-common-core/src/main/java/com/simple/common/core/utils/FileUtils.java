package com.simple.common.core.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Created with IntelliJ IDEA
 * Description: 文件相关帮助类
 * <p>安全说明：所有对外路径入参统一做规范化与目录穿越校验，包含 {@code ..} 段的相对路径一律拒绝，
 * 防止路径穿越写任意文件。</p>
 *
 * @author qty
 */
public class FileUtils extends FileUtil {

    private FileUtils() {
    }

    /**
     * 将输入流写入目标文件，会自动关闭输入流（含创建文件失败的路径）
     *
     * @param inputStream 输入流
     * @param fileName    完整文件URL和名字格式
     */
    public static void write(InputStream inputStream, String fileName) {
        assertSafePath(fileName);
        try (InputStream in = inputStream) {
            File file = createFile(fileName);
            writeFromStream(in, file);
        } catch (Exception e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("写入文件失败: " + fileName, e);
        }
    }

    /**
     * 创建新的空白文件（父目录不存在时自动创建；同名文件直接复用由后续写入覆盖，
     * 不再先删后建，避免创建失败时丢失原文件与并发写同名文件的竞态数据丢失）
     *
     * @param fileName 完整文件URL和名字格式
     * @return 已就绪的文件句柄
     */
    public static File createFile(String fileName) {
        assertSafePath(fileName);
        File file = new File(fileName);
        // 先确保父目录存在，避免嵌套输出路径必然失败
        mkParentDirs(file);
        try {
            if (!file.exists() && !file.createNewFile()) {
                AssertUtils.error("无法创建的文件[{}]", fileName);
            }
        } catch (java.io.IOException e) {
            AssertUtils.error("无法创建的文件[{}]: {}", fileName, e.getMessage());
        }
        return file;
    }

    /**
     * 根据URL获取Resources下文件的文件流
     *
     * @param templatePath 文件URL
     */
    public static InputStream getResourcesFileInputStream(String templatePath) {
        return ResourceUtil.getStream(templatePath);
    }

    /**
     * 获取Resource根目录（解码后的本地路径）
     */
    public static String getPath() {
        URL url = FileUtils.class.getResource("/");
        AssertUtils.notNull(url, "无法获取Resource根目录");
        // 经File/URI规范化转换，替代URL.getPath()的URL编码与前导斜杠问题
        return FileUtil.file(url).getAbsolutePath();
    }

    /**
     * 获取Resource目录下指定相对路径（自动拼接并做目录穿越校验）
     */
    public static String getPath(String path) {
        assertSafePath(path);
        return new File(getPath(), path).getPath();
    }

    /**
     * 路径安全校验：拒绝包含 {@code ..} 目录穿越段的路径（相对与绝对路径均校验）
     */
    private static void assertSafePath(String path) {
        if (path == null) {
            AssertUtils.error("文件路径不能为null");
        }
        // 逐段检查..段（分隔符兼容 \\ 与 /），任何穿越段都拒绝
        for (String segment : path.replace('\\', '/').split("/")) {
            if ("..".equals(segment)) {
                AssertUtils.error("非法文件路径[{}]：不允许包含目录穿越段", path);
            }
        }
    }

}
