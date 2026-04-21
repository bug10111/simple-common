package com.simple.common.excel.common.service;

import com.alibaba.excel.read.listener.ReadListener;
import lombok.SneakyThrows;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * EasyExcel 读取服务接口。
 * <p>
 * 提供基于阿里巴巴 EasyExcel 的 Excel 文件读取功能,支持大文件流式读取,内存占用低。
 * 默认实现 {@link com.simple.common.excel.service.DefaultEasyExcelReadService} 提供了
 * 标准的 Excel 读取流程,支持从文件路径、输入流、MultipartFile 等多种方式读取。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>大数据量导入：支持百万级数据读取,不会OOM</li>
 *   <li>文件上传解析：直接解析用户上传的 Excel 文件</li>
 *   <li>定时任务数据处理：从指定路径读取 Excel 并处理</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private EasyExcelReadService readService;
 * 
 * // 方式1: 从 MultipartFile 读取(推荐)
 * readService.read(file, 1, UserData.class, new ReadListener<UserData>() {
 *     @Override
 *     public void invoke(UserData data, AnalysisContext context) {
 *         // 处理每一行数据
 *         userService.save(data);
 *     }
 *     
 *     @Override
 *     public void doAfterAllAnalysed(AnalysisContext context) {
 *         log.info("所有数据解析完成");
 *     }
 * });
 * 
 * // 方式2: 从文件路径读取
 * readService.read("/path/to/file.xlsx", 1, UserData.class, listener);
 * }</pre>
 *
 * @author qty
 */
public interface EasyExcelReadService {

    /**
     * 从文件路径读取 Excel
     * <p>
     * 根据文件完整路径读取 Excel 数据,通过监听器逐行处理。
     * 适用于服务器本地文件或网络挂载文件的读取。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * readService.read("/data/import/users.xlsx", 1, UserDTO.class, new ReadListener<UserDTO>() {
     *     @Override
     *     public void invoke(UserDTO data, AnalysisContext context) {
     *         // 处理每一行数据
     *         System.out.println("用户: " + data.getName());
     *     }
     *     
     *     @Override
     *     public void doAfterAllAnalysed(AnalysisContext context) {
     *         log.info("解析完成");
     *     }
     * });
     * }</pre>
     *
     * @param filePath      Excel 文件完整路径
     * @param headRowNumber 表头行数,如为1表示第1行为表头,从第2行开始读取数据
     * @param head          数据实体类 Class,属性顺序需与 Excel 列顺序一致;如需动态映射可传入 Map.class
     * @param readListener  数据读取监听器,用于处理每一行数据
     * @param <T>           数据类型
     * @throws RuntimeException 当文件不存在或格式错误时抛出异常
     */
    <T> void read(String filePath, int headRowNumber, Class<T> head, ReadListener<T> readListener);

    /**
     * 从输入流读取 Excel
     * <p>
     * 从 InputStream 中读取 Excel 数据,通过监听器逐行处理。
     * 适用于从网络下载、数据库 BLOB 等场景获取的输入流。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 从网络下载的文件流读取
     * URL url = new URL("http://example.com/data.xlsx");
     * InputStream inputStream = url.openStream();
     * 
     * readService.read(inputStream, 1, OrderDTO.class, new ReadListener<OrderDTO>() {
     *     @Override
     *     public void invoke(OrderDTO data, AnalysisContext context) {
     *         orderService.save(data);
     *     }
     * });
     * }</pre>
     *
     * @param inputStream   Excel 文件输入流
     * @param headRowNumber 表头行数
     * @param head          数据实体类 Class
     * @param readListener  数据读取监听器
     * @param <T>           数据类型
     * @throws RuntimeException 当输入流为空或格式错误时抛出异常
     */
    <T> void read(InputStream inputStream, int headRowNumber, Class<T> head, ReadListener<T> readListener);

    /**
     * 从 MultipartFile 读取 Excel(便捷方法)
     * <p>
     * 直接从 Spring MVC 上传的文件对象中读取 Excel 数据。
     * 这是最常用的读取方式,适用于 Web 应用的文件上传场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @PostMapping("/import")
     * public R importUsers(@RequestParam("file") MultipartFile file) {
     *     List<UserDTO> userList = new ArrayList<>();
     *     
     *     readService.read(file, 1, UserDTO.class, new ReadListener<UserDTO>() {
     *         @Override
     *         public void invoke(UserDTO data, AnalysisContext context) {
     *             userList.add(data);
     *         }
     *         
     *         @Override
     *         public void doAfterAllAnalysed(AnalysisContext context) {
     *             // 批量保存
     *             userService.saveBatch(userList);
     *         }
     *     });
     *     
     *     return R.ok("导入成功,共" + userList.size() + "条数据");
     * }
     * }</pre>
     *
     * @param file          Spring MVC 上传的文件对象
     * @param headRowNumber 表头行数,有两行的话就意味着从第三行开始读取数据
     * @param head          数据实体类 Class
     * @param readListener  数据读取监听器
     * @param <T>           数据类型
     * @throws RuntimeException 当文件为空或格式错误时抛出异常
     * @see #read(InputStream, int, Class, ReadListener)
     */
    @SneakyThrows
    default <T> void read(MultipartFile file, int headRowNumber, Class<T> head, ReadListener<T> readListener) {
        read(file.getInputStream(), headRowNumber, head, readListener);
    }
}
