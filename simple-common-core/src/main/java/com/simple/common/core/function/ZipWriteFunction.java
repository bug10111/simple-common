package com.simple.common.core.function;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA
 * Description: zip写入条目回调（名称通用化：回调能力与Excel无关，可写任意条目）
 * <p>流契约：zipOut 由调用方创建并负责 close/finish，实现方<b>不得关闭</b>该流，
 * 也不得自行调用 finish()（由调用方统一收口）；实现方只负责按条目调用
 * {@code putNextEntry}/{@code write}（可用 {@code ZipUtils.write} 快捷写入）。</p>
 *
 * @author qty
 */
@FunctionalInterface
public interface ZipWriteFunction {

    /**
     * 向zip输出流写入条目
     *
     * @param zipOut    调用方持有的zip输出流，实现方不得关闭
     * @param entryName zip内条目名（相对路径，如 dir/a.txt）
     * @throws IOException 写入失败
     */
    void addToZip(ZipOutputStream zipOut, String entryName) throws IOException;

}
