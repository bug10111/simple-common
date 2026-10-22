package com.simple.common.doc.manager;

import com.deepoove.poi.XWPFTemplate;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.doc.common.manager.DocTemplateReplaceManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 基于 poi-tl 引擎的文档模板替换默认实现。
 * 由 {@link com.simple.common.doc.common.config.DocConfig} 以
 * {@code @ConditionalOnMissingBean(DocTemplateReplaceManager.class)} 方式装配：
 * 容器中注册了自定义 DocTemplateReplaceManager 实现时，本默认实现自动让位不再装配。
 * 参考：https://blog.csdn.net/weixin_44496396/article/details/140066940
 * 官网：https://deepoove.com/poi-tl/
 *
 * @author qty
 */
@Slf4j
public class PoiTlTemplateReplaceManager implements DocTemplateReplaceManager {

    @Override
    public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
        // 入参前置校验,缺失时 poi-tl 内部只会抛出定位困难的 NPE/POIXMLException
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream(模板输入流)不能为空");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream(输出流)不能为空");
        }
        if (values == null) {
            throw new IllegalArgumentException("values(替换参数)不能为空");
        }

        XWPFTemplate template = null;
        try {
            // compile 会持有底层 OPCPackage/XWPFDocument,任何后续分支失败都必须关闭模板,否则持续泄漏堆内存与 POI 资源
            template = XWPFTemplate.compile(inputStream);
            template.render(values);
            // writeAndClose 会写出并关闭模板本身(不关闭传入的流,流由调用方负责关闭)
            template.writeAndClose(outputStream);
        } catch (Exception e) {
            // 渲染失败时模板尚未写出,必须先释放再抛出,避免高频生成场景泄漏 OPCPackage
            closeQuietly(template);
            // 统一包装为业务运行时异常并保留原始异常链;不再使用 @SneakyThrows 把受检异常以未声明方式穿透
            DefaultException exception = new DefaultException("文档模板替换失败：" + e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * 静默关闭模板资源(仅在异常路径调用;正常路径 writeAndClose 已关闭)
     *
     * @param template 待关闭的文档模板,可为 null
     */
    private void closeQuietly(XWPFTemplate template) {
        if (template != null) {
            try {
                template.close();
            } catch (IOException closeException) {
                log.warn("关闭文档模板资源失败", closeException);
            }
        }
    }
}
