package com.simple.common.test.common.entity.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.*;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import com.alibaba.excel.enums.poi.VerticalAlignmentEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Data
@Accessors(chain = false) //这里必须是false，不然会导致读取的数据为空
@Schema(description = "easyexcel测试实体")
@HeadRowHeight(20)//头高
@ColumnWidth(30)//列宽
@ContentRowHeight(20)//内容行高
@HeadStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
@ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
public class EasyExcelDemo {

    @ExcelProperty(value = { "主标题", "名称" }, index = 0)
    @Schema(description = "名称")
    private String name;

    @Schema(description = "父编码")
    @ExcelProperty(value = { "主标题", "父编码" }, index = 1)
    private String parentCode;

    @Schema(description = "编码")
    @ExcelProperty(value = { "主标题", "编码" }, index = 2)
    private String code;

    @Schema(description = "创建时间")
    @DateTimeFormat("yyyy年MM月dd日HH时mm分ss秒")
    @ExcelProperty(value = { "创建时间" }, index = 3)
    private Date createTime;

    /**
     * 忽略这个字段
     */
    @ExcelIgnore
    private String ignore;
}
