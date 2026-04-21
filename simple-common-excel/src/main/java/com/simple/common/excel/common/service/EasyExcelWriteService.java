package com.simple.common.excel.common.service;

import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
     * @param outputStream 目标输出流,可为 null,为 null 时会自动创建新的流
     * @param clazz        数据实体类 Class,需使用 @ExcelProperty 注解标注列信息
     * @param data         数据集合
     * @param <T>          数据类型
     * @return 写入完成后的 ByteArrayOutputStream
     * @throws RuntimeException 当数据为空或写入失败时抛出异常
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
     *
     * @param clazz 数据对象class
     * @param data  数据集合
     * @param <T>   数据对象
     */
    @SneakyThrows
    default <T> ByteArrayInputStream writeInputStream(Class<T> clazz, List<T> data) {
        ByteArrayOutputStream write = writeOutputStream(clazz, data);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(write.toByteArray());
        byteArrayInputStream.close();
        return byteArrayInputStream;
    }
}
