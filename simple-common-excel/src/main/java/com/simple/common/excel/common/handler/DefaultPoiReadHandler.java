package com.simple.common.excel.common.handler;

import cn.hutool.core.lang.Validator;
import lombok.Getter;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: poi 大文件excel导入默认实现
 *
 * @author 兄台丶请冷静
 */
public abstract class DefaultPoiReadHandler<T> implements SheetContentsHandler {

    //读取到空数据的时候默认填充的字符串
    public static final String empty = "empty";

    //第几行开始取数据
    private final int beginRow;

    //第几列开始取数据
    private final int beginCell;

    //第几列结束取数据
    private final int endCell;

    //每一行的数据
    private final StringBuilder stringBuilder = new StringBuilder();

    //数据集合
    @Getter
    private final List<T> list = new ArrayList<>();

    //异常数据集合
    @Getter
    private final List<String> error = new ArrayList<>();

    //当前行数
    private int rowNum = 0;

    //当前列数
    private int cellNum = 0;

    /**
     * 构造函数，初始化部份参数
     *
     * @param beginRow  第几行开始读取
     * @param beginCell 第几列开始读取
     * @param endCell   第几列结束读取
     */
    public DefaultPoiReadHandler(int beginRow, int beginCell, int endCell) {
        this.beginRow = beginRow;
        this.beginCell = beginCell;
        this.endCell = endCell;
    }

    @Override
    public void startRow(int i) {

        //清空stringBuilder
        stringBuilder.delete(0, stringBuilder.length());

        //行数计数
        rowNum++;

        //列数清零
        cellNum = 0;
    }

    @Override
    public void endRow(int i) {
        String str = stringBuilder.toString();
        if (!Validator.isEmpty(str)) {
            String[] split = str.split("\\|@\\|");
            T handler;
            try {
                handler = handler(split);
            } catch (Exception e) {
                error.add("第 [" + rowNum + "] 数据异常，请检查后重新导入");
                return;
            }
            list.add(handler);
        }
    }

    @Override
    public void cell(String s, String s1, XSSFComment xssfComment) {

        //列数计数
        cellNum++;

        //判断行是否在可读的范围(比可读行小，不读)
        if (rowNum < beginRow) {
            return;
        }

        //判断列是否可读（比开始行小，或者比结束行大不读）
        if (cellNum < beginCell || cellNum > endCell) {
            return;
        }

        //不为空
        if (!Validator.isEmpty(s1)) {

            //处理Excel中特殊的空格
            s1 = s1.replaceAll("\u00A0", "");

            //追加数据
            stringBuilder.append(s1);
        } else {
            stringBuilder.append(empty);
        }

        //添加数据间隔
        stringBuilder.append("|@|");
    }

    public abstract T handler(String[] row);

    public Boolean getResults() {
        return error.isEmpty();
    }

}
