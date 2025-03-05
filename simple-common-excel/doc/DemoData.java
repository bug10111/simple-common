import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.*;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import com.alibaba.excel.enums.poi.VerticalAlignmentEnum;
import com.come.on.demo.demo.excel.CustomStringStringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * 官方文档地址：https://easyexcel.opensource.alibaba.com/docs/current/quickstart/read
 * 类注解如下：
 *
 * @HeadRowHeight(20) 头高
 * @ColumnWidth(30) 列宽 ===>可用于字段
 * @ContentRowHeight(20) 内容行高
 * @HeadStyle(HorizontalAlignmentEnum = HorizontalAlignment.CENTER,verticalAlignment = VerticalAlignmentEnum.CENTER,fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND,fillForegroundColor = 17) 头背景设置成红色 illForegroundColor = 10； IndexedColors.RED.getIndex() = 10  ===>可用于字段
 * @ContentStyle(HorizontalAlignmentEnum = HorizontalAlignment.CENTER,verticalAlignment = VerticalAlignmentEnum.CENTER,fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND,fillForegroundColor = 17) 内容的背景设置成绿色 fillForegroundColor = 17； IndexedColors.GREEN.getIndex()  ===>可用于字段
 * @HeadFontStyle(fontHeightInPoints = 20) 头字体设置成20  ===>可用于字段
 * @ContentFontStyle(fontHeightInPoints = 20) 内容字体设置成20  ===>可用于字段
 */
@Getter
@Setter
@EqualsAndHashCode
@HeadRowHeight(20)//头高
@ColumnWidth(30)//列宽
@ContentRowHeight(20)//内容行高
@OnceAbsoluteMerge(firstRowIndex = 5, lastRowIndex = 6, firstColumnIndex = 1, lastColumnIndex = 2)// 将第6-7行的2-3列合并成一个单元格
@HeadStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
@ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
public class DemoData {

    /**
     * value：标题
     * index：导出或者读取的列
     * converter：列处理方案
     */
    @ExcelProperty(value = { "主标题", "字符串标题" }, index = 0, converter = CustomStringStringConverter.class)
    @ContentLoopMerge(eachRow = 2)// 这一列 每隔2行 合并单元格
    private String string;

    @DateTimeFormat("yyyy年MM月dd日HH时mm分ss秒")//格式化时间格式
    @ExcelProperty(value = { "主标题", "日期" }, index = 1)
    private Date date;

    @NumberFormat("#.##%")//百分比表示
    @ExcelProperty(value = { "主标题", "数字" }, index = 3)
    private Double doubleData;

    /**
     * 根据url导出图片
     *
     * @since 2.1.1
     */
    //    @ColumnWidth(60)
    @ExcelProperty(value = { "二维码" }, index = 4)
    private URL url;

    /**
     * 忽略这个字段
     */
    @ExcelIgnore
    private String ignore;
}
