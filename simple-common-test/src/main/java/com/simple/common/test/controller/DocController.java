package com.simple.common.test.controller;

import com.simple.common.core.response.R;
import com.simple.common.doc.common.builder.Docs;
import com.simple.common.doc.common.function.DocFunction;
import com.simple.common.doc.common.service.DocReplaceService;
import com.simple.common.test.common.entity.doc.DocTestEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@RequestMapping("doc")
@Tag(name = "doc文档填充")
@RestController
public class DocController {

    @Autowired
    private DocReplaceService docService;

    @Operation(summary = "填充")
    @PostMapping("replace")
    public R<Object> replace() {
        Docs.DocBuilder builder = Docs.builder();
        builder.addStr("code", "普通文字填充");
        builder.addStr("num", "普通文字填充");
        builder.addStrColor("code1", "有颜色的文字填充", "ff0000");
        builder.addStrLink("code2", "有超链接的文字填充", "https://www.baidu.com");
//        builder.addImgInputUrl("code3", "http://minio.dev.joyswon.com/market-web-public/png/cd37c4411cc64f43a7aa4775b0574e4d.png", 50, 50);

        List<DocTestEntity> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            DocTestEntity doc = new DocTestEntity();
            doc.setName("张三" + i);
            doc.setSex("男" + i);
            doc.setAge(i);
            list.add(doc);
        }
        DocFunction<DocTestEntity> function = docTestEntity -> new String[] { docTestEntity.getName(), docTestEntity.getSex(), docTestEntity.getAge() + "" };
        builder.addTable("table", "90%", new String[] { "姓名", "性别", "年龄" }, list, function);
        docService.replaceResponse("测试文档", "doc/测试模板.docx", builder.create());
//        docService.replaceResponse("测试文档", "doc/场地占用合同.docx", builder.create());
        return R.ok();
    }
}
