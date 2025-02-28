package com.simple.common.test.common.entity.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@Accessors(chain = true)
@Schema(description = "测试文档对象")
public class DocTestEntity {
    private String name;
    private String sex;
    private int age;
}
