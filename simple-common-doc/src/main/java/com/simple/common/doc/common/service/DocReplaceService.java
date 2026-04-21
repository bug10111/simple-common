package com.simple.common.doc.common.service;

import com.simple.common.core.utils.FileUtils;
import com.simple.common.core.utils.ResponseUtils;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 文档替换服务接口。
 * <p>
 * 提供Word文档(.docx)模板替换的高级封装,支持多种使用场景。
 * 基于 {@link DocTemplateReplaceManager} 实现,提供了更便捷的API。
 * </p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *   <li>基础替换：从输入流读取模板,输出到指定流</li>
 *   <li>HTTP响应：直接生成文档并写入HTTP响应,支持浏览器下载</li>
 *   <li>资源加载：从classpath(resources目录)自动加载模板文件</li>
 *   <li>流转换：支持返回InputStream或ByteArrayOutputStream,便于进一步处理</li>
 * </ul>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>合同生成：根据订单数据动态生成合同文档供用户下载</li>
 *   <li>报表导出：将业务数据填充到Excel/Word模板中导出</li>
 *   <li>证书打印：批量生成培训证书、荣誉证书等</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Service
 * public class ContractDocService implements DocReplaceService {
 *     @Autowired
 *     private DocTemplateReplaceManager templateManager;
 *     
 *     @Override
 *     public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
 *         // 添加业务逻辑,如数据校验、权限检查等
 *         validateContractData(values);
 *         
 *         // 调用模板管理器进行替换
 *         templateManager.replace(inputStream, outputStream, values);
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface DocReplaceService {

    /**
     * 替换Word文档模板参数(基础方法)
     * <p>
     * 读取模板文件流,将占位符替换为实际值,并将结果写入输出流。
     * 该方法会自动关闭输入流和输出流。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 手动控制输入输出流
     * try (InputStream templateStream = new FileInputStream("template.docx");
     *      FileOutputStream outputStream = new FileOutputStream("output.docx")) {
     *     docReplaceService.replace(templateStream, outputStream, data);
     * }
     * }</pre>
     *
     * @param inputStream  Word文档模板输入流
     * @param outputStream 替换后的文档输出流
     * @param values       参数Map,key为占位符名称,value为替换值
     * @throws RuntimeException 当模板解析或替换失败时抛出异常
     */
    void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values);

    /**
     * 替换文档并直接写入HTTP响应(从流加载模板)
     * <p>
     * 便捷方法,适用于Controller层直接返回文档下载的场景。
     * 自动设置响应头,浏览器会弹出下载对话框。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @GetMapping("/contract/download/{orderId}")
     * public void downloadContract(@PathVariable String orderId, HttpServletResponse response) {
     *     // 查询订单数据
     *     Order order = orderService.findById(orderId);
     *     
     *     // 准备模板数据
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("orderNo", order.getOrderNo());
     *     data.put("customerName", order.getCustomerName());
     *     data.put("amount", order.getAmount());
     *     
     *     // 加载模板并生成文档
     *     try (InputStream templateStream = getClass().getResourceAsStream("/templates/contract.docx")) {
     *         docReplaceService.replaceResponse("合同_" + order.getOrderNo(), templateStream, data);
     *     }
     * }
     * }</pre>
     *
     * @param name        下载文件名(不含扩展名),会自动添加.docx后缀
     * @param inputStream 模板文件输入流
     * @param values      参数Map,key为占位符名称,value为替换值
     * @throws RuntimeException 当模板解析、替换或响应写入失败时抛出异常
     */
    @SneakyThrows
    default void replaceResponse(String name, InputStream inputStream, Map<String, Object> values) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        replace(inputStream, byteArrayOutputStream, values);
        ResponseUtils.writeResponse(name + ".docx", byteArrayOutputStream);
    }

    /**
     * 替换文档并直接写入HTTP响应(从resources加载模板)
     * <p>
     * 最便捷的方法,只需提供模板路径和数据,自动完成加载、替换、响应写入全流程。
     * 模板文件应放在resources目录下。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @GetMapping("/certificate/download/{userId}")
     * public void downloadCertificate(@PathVariable String userId, HttpServletResponse response) {
     *     User user = userService.findById(userId);
     *     
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("userName", user.getName());
     *     data.put("courseName", "Java高级编程");
     *     data.put("completeDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
     *     
     *     // 一行代码完成所有操作
     *     docReplaceService.replaceResponse("结业证书_" + user.getName(), 
     *                                      "/templates/certificate.docx", data);
     * }
     * }</pre>
     *
     * @param name         下载文件名(不含扩展名)
     * @param templatePath 模板文件路径(相对于resources目录),如 "/templates/contract.docx"
     * @param values       参数Map,key为占位符名称,value为替换值
     * @throws RuntimeException 当模板加载、解析、替换或响应写入失败时抛出异常
     */
    @SneakyThrows
    default void replaceResponse(String name, String templatePath, Map<String, Object> values) {
        ByteArrayOutputStream byteArrayOutputStream = replaceAndGetOutputStream(templatePath, values);
        ResponseUtils.writeResponse(name + ".docx", byteArrayOutputStream);
    }

    /**
     * 替换文档并返回输入流(从resources加载模板)
     * <p>
     * 适用于需要对生成的文档进行进一步处理的场景,如上传到OSS、发送邮件附件等。
     * 注意：返回的流已关闭,调用方不应再次关闭。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 生成文档后上传到OSS
     * Map<String, Object> data = buildReportData();
     * ByteArrayInputStream inputStream = docReplaceService.replaceAndGetInputStream(
     *     "/templates/monthly_report.docx", data
     * );
     * 
     * // 上传到OSS
     * String objectKey = ossService.upload("reports/report.docx", inputStream);
     * }</pre>
     *
     * @param templatePath 模板文件路径(相对于resources目录)
     * @param values       参数Map,key为占位符名称,value为替换值
     * @return 替换后的文档输入流,已关闭状态
     * @throws RuntimeException 当模板加载、解析或替换失败时抛出异常
     */
    @SneakyThrows
    default ByteArrayInputStream replaceAndGetInputStream(String templatePath, Map<String, Object> values) {
        ByteArrayOutputStream byteArrayOutputStream = replaceAndGetOutputStream(templatePath, values);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        byteArrayInputStream.close();
        return byteArrayInputStream;
    }

    /**
     * 替换文档并返回输出流(从resources加载模板)
     * <p>
     * 适用于需要获取完整字节数组的场景,如计算文件大小、加密等。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 生成文档并计算大小
     * Map<String, Object> data = buildInvoiceData();
     * ByteArrayOutputStream outputStream = docReplaceService.replaceAndGetOutputStream(
     *     "/templates/invoice.docx", data
     * );
     * 
     * byte[] documentBytes = outputStream.toByteArray();
     * int fileSize = documentBytes.length;
     * 
     * // 可以用于加密、签名等操作
     * byte[] encrypted = encrypt(documentBytes);
     * }</pre>
     *
     * @param templatePath 模板文件路径(相对于resources目录)
     * @param values       参数Map,key为占位符名称,value为替换值
     * @return 替换后的文档输出流,包含完整的文档字节数据
     * @throws RuntimeException 当模板加载、解析或替换失败时抛出异常
     */
    default ByteArrayOutputStream replaceAndGetOutputStream(String templatePath, Map<String, Object> values) {
        InputStream inputStream = FileUtils.getResourcesFileInputStream(templatePath);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        replace(inputStream, byteArrayOutputStream, values);
        return byteArrayOutputStream;
    }

}
