package com.simple.common.excel.common.service;

import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * POI Excel 读取服务接口。
 * <p>
 * 提供基于 Apache POI 的 Excel 文件读取功能,支持事件驱动模式(SAX),适合大文件读取。
 * 默认实现 {@link com.simple.common.excel.service.DefaultPoiReadService} 提供了
 * 标准的 Excel 读取流程,支持从文件路径、输入流、MultipartFile 等多种方式读取。
 * </p>
 *
 * <h3>与 EasyExcel 的区别：</h3>
 * <ul>
 *   <li>POI Read：基于 SAX 解析,内存占用更低,但 API 较复杂</li>
 *   <li>EasyExcel Read：封装更友好,推荐使用</li>
 * </ul>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>超大文件读取：GB 级别的 Excel 文件</li>
 *   <li>多 Sheet 处理：需要遍历多个工作表</li>
 *   <li>自定义解析逻辑：需要精细控制解析过程</li>
 * </ul>
 *
 * @author qty
 */
public interface PoiReadService {

    /**
     * 从文件路径读取 Excel
     * <p>
     * 根据文件完整路径读取 Excel 数据,通过 SheetContentsHandler 逐行处理。
     * 支持遍历同一个 Excel 文件下的多个 Sheet。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * readService.read("/data/large-file.xlsx", new XSSFSheetXMLHandler.SheetContentsHandler() {
     *     @Override
     *     public void startRow(int rowNum) {
     *         // 开始处理某一行
     *     }
     *     
     *     @Override
     *     public void endRow(int rowNum) {
     *         // 结束处理某一行
     *     }
     *     
     *     @Override
     *     public void cell(String cellReference, String formattedValue, XSSFComment comment) {
     *         // 处理单元格
     *         System.out.println("单元格: " + cellReference + ", 值: " + formattedValue);
     *     }
     * });
     * }</pre>
     *
     * @param filename             Excel 文件完整路径
     * @param sheetContentsHandler Sheet 内容处理器,用于处理每一行和每个单元格
     * @throws RuntimeException 当文件不存在或格式错误时抛出异常
     */
    void read(String filename, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler);

    /**
     * 从输入流读取 Excel
     * <p>
     * 从 InputStream 中读取 Excel 数据,通过 SheetContentsHandler 逐行处理。
     * 支持遍历同一个 Excel 文件下的多个 Sheet。
     * </p>
     *
     * @param inputStream          Excel 文件输入流
     * @param sheetContentsHandler Sheet 内容处理器
     * @throws RuntimeException 当输入流为空或格式错误时抛出异常
     */
    void read(InputStream inputStream, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler);
    /**
     * 从 MultipartFile 读取 Excel(便捷方法)
     * <p>
     * 直接从 Spring MVC 上传的文件对象中读取 Excel 数据。
     * 支持遍历同一个 Excel 文件下的多个 Sheet。
     * </p>
     * <p>流所有权：本方法内部使用 try-with-resources 关闭上传文件流，实现方不得假定调用结束后流仍可用。</p>
     *
     * @param xlsxFile             Spring MVC 上传的文件对象,不能为 null 且不能为空文件
     * @param sheetContentsHandler Sheet 内容处理器
     * @throws IllegalArgumentException 当文件对象为 null 或无内容时抛出
     * @throws UncheckedIOException     当读取上传文件内容失败时抛出(原始 IOException 为其原因)
     * @throws RuntimeException         当文件格式错误或解析失败时抛出
     * @see #read(InputStream, XSSFSheetXMLHandler.SheetContentsHandler)
     */
    default void read(MultipartFile xlsxFile, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler) {
        if (xlsxFile == null || xlsxFile.isEmpty()) {
            throw new IllegalArgumentException("Excel 文件不能为空");
        }
        try (InputStream inputStream = xlsxFile.getInputStream()) {
            read(inputStream, sheetContentsHandler);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read excel file: " + xlsxFile.getOriginalFilename(), e);
        }
    }
}