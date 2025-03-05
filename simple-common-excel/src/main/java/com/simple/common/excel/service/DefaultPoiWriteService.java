package com.simple.common.excel.service;

import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.excel.common.function.PoiExportFunction;
import com.simple.common.excel.common.service.PoiWriteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Service
public class DefaultPoiWriteService implements PoiWriteService {

    @Override
    @SneakyThrows
    public <T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num,
                                                       ByteArrayOutputStream outputStream) {
        int withSize = width.length;

        if (withSize > 1) {
            AssertUtils.isTrue(head.length == width.length, "请为每一个列设置宽度！");
        }

        /*
        默认内存中只创建100个对象，超过的行会被写入磁盘。
        这是为了确保在处理大型 Excel 文件时，不会占用过多的内存，从而避免因内存溢出导致的应用崩溃。
        可以根据需要自行调整这个参数。
        */
        SXSSFWorkbook wb = new SXSSFWorkbook();

        //设置表格样式
        CellStyle cellStyle = this.setHeadStyle(wb);

        // 工作表对象
        Sheet sheet = null;

        // 行对象
        Row nRow;

        // 总行号
        int rowNo = 0;

        // 页行号
        int pageRowNo = 0;

        for (T c : list) {

            // 打印num条后切换到下个工作表
            if (rowNo % num == 0) {

                // 建立新的sheet对象
                wb.createSheet("第" + (rowNo / num + 1) + "个工单簿");

                // 动态指定当前的工作表
                sheet = wb.getSheetAt(rowNo / num);

                // 每当新建了工作表就将当前工作表的行号重置为1
                pageRowNo = 1;

                //定义表头
                nRow = sheet.createRow(0);
                for (int i = 0; i < head.length; i++) {
                    Cell cell = nRow.createCell(i);
                    cell.setCellStyle(cellStyle);
                    sheet.setColumnWidth(cell.getColumnIndex(), (withSize == 1 ? width[0] : width[i]));
                    cell.setCellValue(head[i]);
                }

                rowNo++;
            }

            //新建行对象
            nRow = sheet.createRow(pageRowNo++);

            //数据导入
            function.execute(nRow, c);

            rowNo++;
        }

        wb.write(outputStream);
        wb.close();
        outputStream.close();
        return outputStream;
    }

    @SneakyThrows
    @Override
    public <T> void exportResponse(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num, String excelName) {
        HttpServletResponse response = HttpServletUtils.getResponse();
        HttpServletRequest request = HttpServletUtils.getRequest();

        response.reset();
        response.setContentType("application/octet-stream");
        response.setHeader("content-disposition", "attachment; filename=" + URLEncoder.encode(excelName + ".xlsx", StandardCharsets.UTF_8));
        String originalURL = request.getHeader("Origin");
        if (originalURL != null) {
            response.addHeader("Access-Control-Allow-Origin", originalURL);
        }

        writeOutputStream(function, list, head, width, num).writeTo(response.getOutputStream());
    }

    /**
     * 设置表格样式
     *
     * @param workbook
     */
    protected CellStyle setHeadStyle(SXSSFWorkbook workbook) {

        // 字体样式
        XSSFFont xssfFont = (XSSFFont) workbook.createFont();

        // 加粗
        xssfFont.setBold(true);

        // 字体名称
        xssfFont.setFontName("楷体");

        // 字体大小
        xssfFont.setFontHeight(12);

        // 表头样式
        CellStyle headStyle = workbook.createCellStyle();

        // 设置字体css
        headStyle.setFont(xssfFont);

        // 竖向居中
        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 横向居中
        headStyle.setAlignment(HorizontalAlignment.CENTER);

        // 边框
        headStyle.setBorderBottom(BorderStyle.THIN);
        headStyle.setBorderLeft(BorderStyle.THIN);
        headStyle.setBorderRight(BorderStyle.THIN);
        headStyle.setBorderTop(BorderStyle.THIN);

        return headStyle;
    }

}
