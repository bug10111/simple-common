package com.simple.common.core.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import lombok.SneakyThrows;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * Description: 文件相关帮助类
 *
 * @author qty
 */
public class FileUtils extends FileUtil {

    /**
     * 将输入流写入目标文件，会自动关闭输入流
     *
     * @param inputStream 输入流
     * @param fileName    完整文件URL和名字格式
     */
    public static void write(InputStream inputStream, String fileName) {
        File file = createFile(fileName);
        writeFromStream(inputStream, file);
    }

    /**
     * 创建新的空白文件，会自动删除旧的同名文件
     *
     * @param fileName 完整文件URL和名字格式
     */
    @SneakyThrows
    public static File createFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            // 如果文件已存在，则删除
            if (!file.delete()) {
                AssertUtils.errorParams("无法删除的文件[{}]", fileName);
            }
        }

        // 创建新文件
        if (!file.createNewFile()) {
            AssertUtils.errorParams("无法创建的文件[{}]", fileName);
        }
        return file;
    }

    /**
     * 根据URL获取Resources下文件的文件流
     *
     * @param templatePath 文件URL
     */
    @SneakyThrows
    public static InputStream getResourcesFileInputStream(String templatePath) {
        return ResourceUtil.getStream(templatePath);
    }

    /**
     * 获取Resource目录URL
     */
    public static String getPath() {
        return Objects.requireNonNull(FileUtils.class.getResource("/")).getPath();
    }

    /**
     * 获取Resource目录URL
     */
    public static String getPath(String path) {
        return Objects.requireNonNull(FileUtils.class.getResource("/")).getPath() + path;
    }

}
