package com.simple.common.doc.common.manager;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 文档模板替换管理器接口。
 * <p>
 * 用于处理Word文档(.docx)模板的变量替换,支持基于模板生成定制化文档。
 * 默认实现 {@link com.simple.common.doc.manager.PoiTlTemplateReplaceManager} 基于
 * poi-tl模板引擎实现文档模板替换功能,支持文本、图片、表格、列表等多种元素替换。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>合同生成：根据业务数据动态生成合同文档</li>
 *   <li>报告导出：生成包含图表和数据的分析报告</li>
 *   <li>证书制作：批量生成荣誉证书、培训证书等</li>
 *   <li>通知函件：自动生成个性化的通知、邀请函等</li>
 * </ul>
 *
 * <h3>模板语法：</h3>
 * <ul>
 *   <li>文本替换：{{variableName}}</li>
 *   <li>图片替换：{{@imageName}}</li>
 *   <li>表格循环：{{#tableData}}...{{/tableData}}</li>
 *   <li>条件判断：{{?condition}}...{{/condition}}</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class CustomDocTemplateManager implements DocTemplateReplaceManager {
 *     @Override
 *     public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
 *         // 使用poi-tl引擎进行模板替换
 *         XWPFTemplate template = XWPFTemplate.compile(inputStream).render(values);
 *         template.writeAndClose(outputStream);
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface DocTemplateReplaceManager {

    /**
     * 替换Word文档模板参数
     * <p>
     * 读取模板文件流,将占位符替换为实际值,并将结果写入输出流。
     * 该方法会自动关闭输入流和输出流。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 准备模板数据
     * Map<String, Object> data = new HashMap<>();
     * data.put("contractNo", "HT20240115001");
     * data.put("partyA", "甲方公司名称");
     * data.put("partyB", "乙方公司名称");
     * data.put("amount", "100000.00");
     * data.put("signDate", "2024年1月15日");
     * 
     * // 加载模板并替换
     * try (InputStream templateStream = getClass().getResourceAsStream("/templates/contract.docx");
     *      FileOutputStream outputStream = new FileOutputStream("output.docx")) {
     *     docTemplateManager.replace(templateStream, outputStream, data);
     * }
     * }</pre>
     *
     * @param inputStream  Word文档模板输入流,不能为null
     * @param outputStream 替换后的文档输出流,不能为null
     * @param values       参数Map,key为模板中的占位符名称(不含{{}}),value为替换值
     *                     支持String、Number、Date、PictureRenderData等类型
     * @throws RuntimeException 当模板格式错误、占位符不存在或IO异常时抛出异常
     */
    void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values);


}