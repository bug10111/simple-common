package com.simple.common.core.function;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA
 * Description: zip添加文件函数
 *
 * @author 兄台丶请冷静
 */
@FunctionalInterface
public interface ZipWriteFunction {

    void addExcelToZip(ZipOutputStream zipOut, String fileName) throws IOException;

}
