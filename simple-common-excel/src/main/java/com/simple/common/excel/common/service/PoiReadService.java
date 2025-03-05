package com.simple.common.excel.common.service;

import lombok.SneakyThrows;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA
 * Description: poi excel导入
 *
 * @author 兄台丶请冷静
 */
public interface PoiReadService {

    /**
     * 根据路径读取Excel数据
     *
     * @param filename             文件完整路径
     * @param sheetContentsHandler 执行器
     */
    void read(String filename, SheetContentsHandler sheetContentsHandler);

    /**
     * 根据文件流读取excel
     * 支持遍历同一个excel文件下多个sheet的解析
     *
     * @param inputStream          文件流
     * @param sheetContentsHandler 执行器
     */
    void read(InputStream inputStream, SheetContentsHandler sheetContentsHandler);

    /**
     * 根据上传文件读取
     * 支持遍历同一个excel文件下多个sheet的解析
     *
     * @param xlsxFile             上传对象
     * @param sheetContentsHandler 执行器
     */
    @SneakyThrows
    default void read(MultipartFile xlsxFile, SheetContentsHandler sheetContentsHandler) {
        read(xlsxFile.getInputStream(), sheetContentsHandler);
    }
}
