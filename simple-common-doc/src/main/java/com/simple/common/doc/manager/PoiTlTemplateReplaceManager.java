package com.simple.common.doc.manager;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.Rows;
import com.deepoove.poi.data.Tables;
import com.simple.common.doc.common.function.DocFunction;
import com.simple.common.doc.common.manager.DocTemplateReplaceManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 参考：https://blog.csdn.net/weixin_44496396/article/details/140066940
 * 官网：https://deepoove.com/poi-tl/
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class PoiTlTemplateReplaceManager implements DocTemplateReplaceManager {

    @Override
    @SneakyThrows
    public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
        XWPFTemplate template = XWPFTemplate.compile(inputStream).render(values);
        template.writeAndClose(outputStream);
    }
}
