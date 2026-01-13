package com.simple.common.test.common.entity.cycle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "参数demo")
public class DataDemo {
    private String demoName1;
    private String demoName2;
}
