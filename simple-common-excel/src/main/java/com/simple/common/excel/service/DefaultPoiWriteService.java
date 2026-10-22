package com.simple.common.excel.service;

import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.excel.common.function.PoiExportFunction;
import com.simple.common.excel.common.service.PoiWriteService;
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
 * @author qty
 */
@Service
public class DefaultPoiWriteService implements PoiWriteService {

    /**
     * xlsx 单 Sheet 物理行上限(行索引 0 ~ 1048576);每个 Sheet 预留 1 行表头后,
     * 数据行数上限为 1048575,超出会在 createRow 时抛出 IllegalArgumentException
     */
    private static final int MAX_DATA_ROWS_PER_SHEET = 1048575;

    @Override
    @SneakyThrows
    public <T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num,
                                                       ByteArrayOutputStream outputStream) {
        // 参数校验：每个 Sheet 的最大数据行数必须为正数,否则取模运算会出现除零或空指针
        if (num == null || num < 1) {
            throw new IllegalArgumentException("num(每个 Sheet 的最大数据行数)必须大于等于 1");
        }
        // 参数校验：单 Sheet 行上限为 1048576(含表头),超出后 createRow 直接抛异常,提前给出可读错误
        if (num > MAX_DATA_ROWS_PER_SHEET) {
            throw new IllegalArgumentException("num(每个 Sheet 的最大数据行数)不能超过 1048575(每个 Sheet 预留 1 行表头)");
        }

        // 参数校验：表头与列宽数组必须提供,否则写入过程中会出现空指针
        if (head == null) {
            throw new IllegalArgumentException("head(表头名称数组)不能为空");
        }
        if (width == null) {
            throw new IllegalArgumentException("width(列宽数组)不能为空");
        }
        // 参数校验：空列宽数组在 widthSize==1 判断下被跳过长度校验,后续 width[i] 会抛数组越界
        int widthSize = width.length;
        if (widthSize == 0) {
            throw new IllegalArgumentException("width(列宽数组)不能为空数组");
        }

        if (widthSize > 1) {
            if (head.length != widthSize) {
                throw new IllegalArgumentException("请为每一个列设置宽度！");
            }
        }

        /*
        默认内存中只创建100个对象，超过的行会被写入磁盘。
        这是为了确保在处理大型 Excel 文件时，不会占用过多的内存，从而避免因内存溢出导致的应用崩溃。
        可以根据需要自行调整这个参数。
        */
        SXSSFWorkbook wb = new SXSSFWorkbook();

        //设置表格样式
        CellStyle cellStyle = this.setHeadStyle(wb);

        //已写入的数据行计数(不含表头),与页内行号分离,保证分页判定不受表头行影响
        int dataRowNo = 0;

        // 页内行号
        int pageRowNo = 0;

        try {
            // 工作表对象
            Sheet sheet = null;

            // 行对象
            Row nRow;

            // 空集合也产出仅含表头的合法工作簿,避免 POI 写出无 Sheet 工作簿时报错
            if (list == null || list.isEmpty()) {
                createSheetWithHeader(wb, cellStyle, head, width, widthSize, 1);
                dataRowNo = num;
            }

            if (list != null) {
                for (T c : list) {

                    // 每写入 num 条数据(不含表头)后切换到下个工作表
                    if (dataRowNo % num == 0) {
                        // 建立新的sheet对象
                        sheet = createSheetWithHeader(wb, cellStyle, head, width, widthSize, dataRowNo / num + 1);

                        // 每当新建了工作表就将当前工作表的行号重置为1
                        pageRowNo = 1;
                    }

                    //新建行对象
                    nRow = sheet.createRow(pageRowNo++);

                    //数据导入
                    function.execute(nRow, c);

                    dataRowNo++;
                }
            }

            wb.write(outputStream);
        } finally {
            // 关闭 SXSSF 工作簿并清理磁盘临时文件(任何异常路径都执行,避免临时文件泄漏)
            wb.close();
        }
        return outputStream;
    }

    @SneakyThrows
    @Override
    public <T> void exportResponse(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num, String excelName) {
        HttpServletResponse response = HttpServletUtils.getResponse();
        if (response == null) {
            throw new IllegalStateException("当前线程不存在 Servlet 请求上下文，无法写入下载响应");
        }
        if (excelName == null || excelName.isBlank()) {
            throw new IllegalArgumentException("excelName(下载文件名)不能为空");
        }

        response.reset();
        response.setContentType("application/octet-stream");

        // 文件名按 UTF-8 URL 编码并将空格替换为 %20,下载头带 UTF-8'' 字符集前缀严格符合 RFC 5987,防止中文文件名乱码
        String fileName = URLEncoder.encode(excelName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");

        // 跨域由网关或应用统一的 CORS 配置管理,不再反射请求 Origin 头(防止任意 Origin 反射绕过同源策略)
        writeOutputStream(function, list, head, width, num, new ByteArrayOutputStream()).writeTo(response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 创建工作表并写入表头
     *
     * @param wb         工作簿
     * @param cellStyle  表头样式
     * @param head       表头名称数组
     * @param width      列宽数组
     * @param widthSize  列宽数组长度
     * @param sheetIndex 第几个工作表(从 1 开始)
     * @return 新创建的工作表
     */
    protected Sheet createSheetWithHeader(SXSSFWorkbook wb, CellStyle cellStyle, String[] head, Integer[] width, int widthSize, int sheetIndex) {
        // 建立新的sheet对象
        Sheet sheet = wb.createSheet("第" + sheetIndex + "个工单簿");

        //定义表头
        Row headRow = sheet.createRow(0);
        for (int i = 0; i < head.length; i++) {
            Cell cell = headRow.createCell(i);
            cell.setCellStyle(cellStyle);
            sheet.setColumnWidth(cell.getColumnIndex(), (widthSize == 1 ? width[0] : width[i]));
            cell.setCellValue(head[i]);
        }
        return sheet;
    }

    /**
     * 设置表格样式
     *
     * @param workbook 工作簿
     * @return 表头样式
     */
    protected CellStyle setHeadStyle(SXSSFWorkbook workbook) {

        // 字体样式
        XSSFFont xssfFont = (XSSFFont) workbook.createFont();

        // 加粗
        xssfFont.setBold(true);

        // 字体名称
        xssfFont.setFontName("楷体");

        // 字体大小
        xssfFont.setFontHeight((short) 12);

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
