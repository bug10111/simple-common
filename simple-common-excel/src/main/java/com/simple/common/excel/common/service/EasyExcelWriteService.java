package com.simple.common.excel.common.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * EasyExcel 写入服务接口。
 * <p>
 * 提供基于阿里巴巴 EasyExcel 的 Excel 文件写入功能,支持大数据量导出,内存占用低。
 * 默认实现 {@link com.simple.common.excel.service.DefaultEasyExcelWriteService} 提供了
 * 标准的 Excel 写入流程,支持输出流、HTTP Response、输入流等多种写入方式。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>数据导出：将数据库数据导出为 Excel 文件</li>
 *   <li>报表生成：生成各种业务报表并下载</li>
 *   <li>数据备份：定期导出数据进行备份</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private EasyExcelWriteService writeService;
 * 
 * // 导出到 HTTP Response(浏览器下载)
 * @GetMapping("/export")
 * public void exportUsers(HttpServletResponse response) {
 *     List<UserDTO> userList = userService.findAll();
 *     writeService.writeResponse(UserDTO.class, userList, "用户列表");
 * }
 * 
 * // 导出到输出流
 * ByteArrayOutputStream outputStream = writeService.writeOutputStream(UserDTO.class, userList);
 * }</pre>
 *
 * @author qty
 */
public interface EasyExcelWriteService {

    /**
     * 将数据写入输出流
     * <p>
     * 将数据列表转换为 Excel 格式并写入指定的 ByteArrayOutputStream。
     * 适用于需要将 Excel 数据进一步处理的场景,如上传到 OSS、发送邮件附件等。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * List<OrderDTO> orders = orderService.findAll();
     * ByteArrayOutputStream outputStream = writeService.writeOutputStream(
     *     new ByteArrayOutputStream(), OrderDTO.class, orders
     * );
     * 
     * // 上传到 OSS
     * ossClient.putObject("exports/orders.xlsx", outputStream.toByteArray());
     * }</pre>
     *
     * @param outputStream 目标输出流(必填;写入完成后本实现会尝试关闭该流,ByteArrayOutputStream 的 close 为空操作,不影响后续 toByteArray)
     * @param clazz        数据实体类 Class,需使用 @ExcelProperty 注解标注列信息
     * @param data         数据集合,null 或空集合时仅写出表头行,不会抛出异常
     * @param <T>          数据类型
     * @return 写入完成后的 ByteArrayOutputStream
     * @throws IllegalArgumentException 当 outputStream 为 null 时抛出
     * @throws RuntimeException         当写入失败时抛出异常
     */
    <T> ByteArrayOutputStream writeOutputStream(ByteArrayOutputStream outputStream, Class<T> clazz, List<T> data);

    /**
     * 将数据写入 HTTP Response(浏览器下载)
     * <p>
     * 将数据列表转换为 Excel 格式并直接写入 HTTP Response,
     * 触发浏览器下载文件。这是 Web 应用中最常用的导出方式。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @GetMapping("/export/users")
     * public void exportUsers(HttpServletResponse response) {
     *     List<UserDTO> userList = userService.findAll();
     *     // 自动设置响应头,触发浏览器下载 "用户列表.xlsx"
     *     writeService.writeResponse(UserDTO.class, userList, "用户列表");
     * }
     * }</pre>
     *
     * @param clazz     数据实体类 Class
     * @param data      数据集合
     * @param writeName 导出文件名称(不含扩展名,会自动添加 .xlsx)
     * @param <T>       数据类型
     * @throws RuntimeException 当写入失败时抛出异常
     */
    <T> void writeResponse(Class<T> clazz, List<T> data, String writeName);

    /**
     * 将数据写入输出流
     *
     * @param <T>   数据对象
     * @param clazz 数据对象class
     * @param data  数据集合
     */
    default <T> ByteArrayOutputStream writeOutputStream(Class<T> clazz, List<T> data) {
        return writeOutputStream(new ByteArrayOutputStream(), clazz, data);
    }

    /**
     * 将数据写入输入流
     * <p>
     * 注意：本方法会先在内存中物化整个工作簿(ByteArrayOutputStream)再拷贝一份字节数组，
     * 内存占用约为数据体积的两倍，大数据量导出请改用 {@link #writeOutputStream} 或
     * {@link #createWriter} 流式写入。
     * </p>
     *
     * @param clazz 数据实体类
     * @param data  数据集合
     * @param <T>   数据类型
     * @return 内容为生成的 Excel 字节的输入流
     */
    default <T> ByteArrayInputStream writeInputStream(Class<T> clazz, List<T> data) {
        ByteArrayOutputStream write = writeOutputStream(clazz, data);
        return new ByteArrayInputStream(write.toByteArray());
    }

    /**
     * 创建流式 ExcelWriter（用于大数据量分批写入）。
     * <p>
     * {@link WriteContext} 已实现 {@link AutoCloseable}，推荐 try-with-resources 使用，
     * close() 内部等价于 {@code excelWriter.finish()} 收尾；若不使用 try-with-resources，
     * 必须在 finally 中调用 finish()，否则分批写入抛异常时工作簿与底层流会泄漏。
     * 大数据量场景下配合 OutputStream 使用，边查边写，避免内存溢出。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * try (WriteContext<DemoData> ctx = writeService.createWriter(
     *         response.getOutputStream(), DemoData.class, "数据导出")) {
     *
     *     while (hasMore) {
     *         List<DemoData> batch = queryBatch();
     *         ctx.getExcelWriter().write(batch, ctx.getWriteSheet());
     *     }
     *     // try 块结束时自动调用 finish() 收尾
     * }
     * }</pre>
     *
     * @param outputStream 输出流（如 HttpServletResponse.getOutputStream()），不能为 null
     * @param clazz        数据实体类，需用 @ExcelProperty 注解标注列信息，不能为 null
     * @param sheetName    Sheet 名称
     * @param <T>          数据类型
     * @return WriteContext 包含 ExcelWriter 和 WriteSheet
     */
    <T> WriteContext<T> createWriter(OutputStream outputStream, Class<T> clazz, String sheetName);
}