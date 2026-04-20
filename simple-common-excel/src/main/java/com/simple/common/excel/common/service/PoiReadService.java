package com.simple.common.excel.common.service;

import lombok.SneakyThrows;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * POI Excel读取服务接口。
 * <p>
 * 提供基于Apache POI的Excel文件读取功能，支持自定义处理器处理每行数据。
 * 默认实现 {@link com.simple.common.excel.service.DefaultPoiReadService} 提供了
 * 标准的Excel读取流程。
 * </p>
 *
 * @author qty
 */
public interface PoiReadService {

    /**
     * 根据路径读取Excel数据
     *
     * @param filename             文件完整路径
     * @param sheetContentsHandler 执行器
     */
    void read(String filename, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler);

    /**
     * 根据文件流读取excel
     * 支持遍历同一个excel文件下多个sheet的解析
     *
     * @param inputStream          文件流
     * @param sheetContentsHandler 执行器
     */
    void read(InputStream inputStream, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler);

    /**
     * 根据上传文件读取
     * 支持遍历同一个excel文件下多个sheet的解析
     *
     * @param xlsxFile             上传对象
     * @param sheetContentsHandler 执行器
     */
    @SneakyThrows
    default void read(MultipartFile xlsxFile, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler) {
        read(xlsxFile.getInputStream(), sheetContentsHandler);
    }
}