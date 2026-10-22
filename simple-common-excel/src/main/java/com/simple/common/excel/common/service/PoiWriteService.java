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
     * // 列宽单位为 1/256 字符宽(POI 原生单位),如需 20 字符宽请传 20 * 256
     * Integer[] widths = {20 * 256, 30 * 256, 15 * 256};
     *
     * ByteArrayOutputStream outputStream = writeService.writeOutputStream(
     *     (row, user) -> {
     *         // 按列顺序填充每一行的数据(execute 为 void,不支持返回值形式)
     *         row.createCell(0).setCellValue(user.getId());
     *         row.createCell(1).setCellValue(user.getName());
     *     },
     *     users,
     *     headers,
     *     widths,
     *     1000, // 每页1000行
     *     new ByteArrayOutputStream()
     * );
     * }</pre>
     *
     * @param function     数据填充函数,对每一行数据回调 execute(Row, T);entity 可能为 null,实现需自行判空
     * @param list         数据集合
     * @param head         表头名称数组,如 {"用户ID", "姓名", "年龄"}
     * @param width        列宽数组(单位:1/256 字符宽,POI 原生单位;如需 20 字符宽请传 20 * 256;
     *                     元素取值须在 POI 允许范围 [0, 255*256] 内且非 null;长度为 1 时全列统一列宽,
     *                     否则长度必须与 head 一致)
     * @param num          每个 Sheet 的最大数据行数(不含表头),必须大于等于 1 且小于等于 1048575
     *                     (每个 Sheet 预留 1 行表头,xlsx 单 Sheet 行上限为 1048576)
     * @param outputStream 目标输出流
     * @param <T>          数据类型
     * @return 写入完成后的 ByteArrayOutputStream
     * @throws IllegalArgumentException 当 num 为 null 或不在 [1, 1048575] 区间、head/width 为 null、
     *                                  width 为空数组或长度既不为 1 也不等于 head 长度时抛出
     * @throws RuntimeException         当写入失败时抛出异常
     */
    <T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num,
                                                ByteArrayOutputStream outputStream);

    /**
     * 导出 Excel 文档到 HTTP Response(浏览器下载)
     * <p>
     * 将数据列表转换为 Excel 格式并直接写入 HTTP Response,
     * 触发浏览器下载文件。支持自定义文件名、列宽、分页等。
     * </p>
     * <p>注意：本方法依赖当前线程的 Servlet 请求上下文,只能在处理 HTTP 请求的线程中调用;
     * 跨域响应头由网关或应用统一 CORS 配置管理。</p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @GetMapping("/export/users")
     * public void exportUsers(HttpServletResponse response) {
     *     List<UserDTO> users = userService.findAll();
     *     String[] headers = {"用户ID", "姓名", "年龄"};
     *     // 列宽单位为 1/256 字符宽(POI 原生单位),如需 20 字符宽请传 20 * 256
     *     Integer[] widths = {20 * 256, 30 * 256, 15 * 256};
     *
     *     writeService.exportResponse(
     *         (row, user) -> {
     *             row.createCell(0).setCellValue(user.getId());
     *             row.createCell(1).setCellValue(user.getName());
     *         },
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
     * @param width     列宽数组(单位:1/256 字符宽,POI 原生单位;如需 20 字符宽请传 20 * 256;
     *                  元素取值须在 POI 允许范围 [0, 255*256] 内且非 null;长度为 1 时全列统一列宽,
     *                  否则长度必须与 head 一致)
     * @param num       每个 Sheet 的最大数据行数(不含表头),必须大于等于 1 且小于等于 1048575
     * @param excelName 导出文件名(不含扩展名,会自动添加 .xlsx),不能为空
     * @param <T>       数据类型
     * @throws IllegalArgumentException 当 num 为 null 或不在 [1, 1048575] 区间、head/width 为 null、
     *                                  width 为空数组或长度既不为 1 也不等于 head 长度时抛出
     * @throws IllegalStateException    当当前线程不存在 Servlet 请求上下文时抛出
     * @throws RuntimeException         当写入失败时抛出异常
     */
    <T> void exportResponse(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num, String excelName);

    /**
     * 将数据写入输入流
     * <p>
     * 注意：本方法会先在内存中物化整个工作簿再拷贝一份字节数组，内存占用约为数据体积两倍，
     * 大数据量导出请改用 {@link #writeOutputStream} 直接写目标流。
     * </p>
     *
     * @param function 数据填充函数
     * @param list     数据集合
     * @param head     导出列的名字
     * @param width    列宽数组
     * @param num      Excel每一页多少数据，必须大于等于 1 且小于等于 1048575(不含表头)
     * @return 内容为生成的 Excel 字节的输入流
     */
    default <T> ByteArrayInputStream writeInputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num) {
        ByteArrayOutputStream export = writeOutputStream(function, list, head, width, num, new ByteArrayOutputStream());
        return new ByteArrayInputStream(export.toByteArray());
    }

    /**
     * 将数据写入输出流
     *
     * @param function 数据填充函数
     * @param list     数据集合
     * @param head     导出列的名字
     * @param width    列宽数组(单位:1/256 字符宽,POI 原生单位;长度为 1 时全列统一列宽,否则必须与 head 长度一致)
     * @param num      Excel每一页多少数据，必须大于等于 1 且小于等于 1048575(不含表头)
     * @return 写入完成后的输出流
     */
    default <T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num) {
        return writeOutputStream(function, list, head, width, num, new ByteArrayOutputStream());
    }

}
