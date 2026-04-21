package com.simple.common.excel.common.service;

import com.simple.common.excel.common.function.PoiExportFunction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * POI Excel 写入服务接口。
 * <p>
 * 提供基于 Apache POI 的 Excel 文件写入功能,支持自定义列宽、分页等高级特性。
 * 默认实现 {@link com.simple.common.excel.service.DefaultPoiWriteService} 提供了
 * 标准的 Excel 写入流程,支持输出流、HTTP Response、输入流等多种写入方式。
 * </p>
 *
 * <h3>与 EasyExcel 的区别：</h3>
 * <ul>
 *   <li>POI Write：更灵活,支持复杂的单元格样式、合并单元格等</li>
 *   <li>EasyExcel Write：更简单,推荐使用</li>
 * </ul>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>复杂报表导出：需要自定义样式、合并单元格</li>
 *   <li>大数据量分页导出：每页固定行数,避免内存溢出</li>
 *   <li>动态列导出：根据业务需求动态生成列</li>
 * </ul>
 *
 * @author qty
 */
public interface PoiWriteService {

    /**
     * 将数据写入输出流
     * <p>
     * 将数据列表转换为 Excel 格式并写入指定的 ByteArrayOutputStream。
     * 支持自定义列名、列宽、每页行数等高级配置。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * List<UserDTO> users = userService.findAll();
     * String[] headers = {"用户ID", "姓名", "年龄"};
     * Integer[] widths = {20, 30, 15};
     * 
     * ByteArrayOutputStream outputStream = writeService.writeOutputStream(
     *     (rowNum, user) -> {
     *         // 填充每一行的数据
     *         return new Object[]{user.getId(), user.getName(), user.getAge()};
     *     },
     *     users,
     *     headers,
     *     widths,
     *     1000  // 每页1000行
     * );
     * }</pre>
     *
     * @param function     数据填充函数,接收行号和数据对象,返回该行的单元格值数组
     * @param list         数据集合
     * @param head         表头名称数组,如 {"用户ID", "姓名", "年龄"}
     * @param width        列宽数组(单位:字符),如 {20, 30, 15}
     * @param num          每个 Sheet 的最大行数(不要超过 1048576)
     * @param outputStream 目标输出流
     * @param <T>          数据类型
     * @return 写入完成后的 ByteArrayOutputStream
     * @throws RuntimeException 当写入失败时抛出异常
     */
    <T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num,
                                                ByteArrayOutputStream outputStream);

    /**
     * 导出 Excel 文档到 HTTP Response(浏览器下载)
     * <p>
     * 将数据列表转换为 Excel 格式并直接写入 HTTP Response,
     * 触发浏览器下载文件。支持自定义文件名、列宽、分页等。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @GetMapping("/export/users")
     * public void exportUsers(HttpServletResponse response) {
     *     List<UserDTO> users = userService.findAll();
     *     String[] headers = {"用户ID", "姓名", "年龄"};
     *     Integer[] widths = {20, 30, 15};
     *     
     *     writeService.exportResponse(
     *         (rowNum, user) -> new Object[]{user.getId(), user.getName(), user.getAge()},
     *         users,
     *         headers,
     *         widths,
     *         1000,  // 每页1000行
     *         "用户列表"  // 文件名
     *     );
     * }
     * }</pre>
     *
     * @param function  数据填充函数
     * @param list      数据集合
     * @param head      表头名称数组
     * @param width     列宽数组
     * @param num       每个 Sheet 的最大行数
     * @param excelName 导出文件名(不含扩展名,会自动添加 .xlsx)
     * @param <T>       数据类型
     * @throws RuntimeException 当写入失败时抛出异常
     */
    <T> void exportResponse(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num, String excelName);

    /**
     * 将数据写入输入流
     *
     * @param function 数据填充
     * @param list     数据集合
     * @param head     导出列的名字
     * @param width    列宽
     * @param num      Excel每一页多少数据（不要超过1048576）
     */
    default <T> ByteArrayInputStream writeInputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num) {
        ByteArrayOutputStream export = writeOutputStream(function, list, head, width, num);
        return new ByteArrayInputStream(export.toByteArray());
    }

    /**
     * 将数据写入输出流
     *
     * @param function 数据填充
     * @param list     数据集合
     * @param head     导出列的名字
     * @param width    列宽
     * @param num      Excel每一页多少数据（不要超过1048576）
     */
    default <T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        return writeOutputStream(function, list, head, width, num, outputStream);
    }

}