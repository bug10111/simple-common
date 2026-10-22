package com.simple.common.excel.common.handler;

import cn.hutool.core.lang.Validator;
import lombok.Getter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: poi 大文件excel导入默认实现
 * <p>
 * 行号与列号均为 1 基。列位置取自 POI 单元格引用的真实列号，稀疏单元格（该列在 xlsx XML 中缺值）
 * 以 {@link #empty} 占位补齐，保证每行交给 {@link #handler(String[])} 的数组长度恒等于
 * (endCell - beginCell + 1)，不会因空列或单元格值本身含分隔符而错位。
 * </p>
 *
 * @author qty
 */
public abstract class DefaultPoiReadHandler<T> implements SheetContentsHandler {

    //读取到空数据的时候默认填充的字符串
    public static final String empty = "empty";

    //第几行开始取数据(1 基)
    private final int beginRow;

    //第几列开始取数据(1 基)
    private final int beginCell;

    //第几列结束取数据(1 基,含)
    private final int endCell;

    //每一行的数据(按列槽位存放,稀疏列以 empty 占位)
    private final List<String> rowDataList = new ArrayList<>();

    //数据集合
    @Getter
    private final List<T> list = new ArrayList<>();

    //异常数据集合
    @Getter
    private final List<String> error = new ArrayList<>();

    //当前行号(1 基,取自 POI 行索引)
    private int currentRowNumber = 0;

    /**
     * 构造函数，初始化部份参数
     *
     * @param beginRow  第几行开始读取(1 基,必须大于等于 1)
     * @param beginCell 第几列开始读取(1 基,必须大于等于 1)
     * @param endCell   第几列结束读取(1 基,含,必须大于等于 beginCell)
     */
    public DefaultPoiReadHandler(int beginRow, int beginCell, int endCell) {
        if (beginRow < 1) {
            throw new IllegalArgumentException("beginRow(开始行号,1 基)必须大于等于 1，当前值：" + beginRow);
        }
        if (beginCell < 1) {
            throw new IllegalArgumentException("beginCell(开始列号,1 基)必须大于等于 1，当前值：" + beginCell);
        }
        if (endCell < beginCell) {
            throw new IllegalArgumentException("列区间非法：beginCell=" + beginCell + " 大于 endCell=" + endCell);
        }
        this.beginRow = beginRow;
        this.beginCell = beginCell;
        this.endCell = endCell;
    }

    @Override
    public void startRow(int rowIndex) {

        //清空行数据
        rowDataList.clear();

        //行号取自 POI 行索引(0 基转 1 基)，不使用计数器，避免错位
        currentRowNumber = rowIndex + 1;
    }

    @Override
    public void endRow(int rowIndex) {

        //补齐稀疏列为空占位，保证列数恒等于区间宽度
        while (rowDataList.size() < endCell - beginCell + 1) {
            rowDataList.add(empty);
        }

        //整行均为空占位(全空行)则跳过
        boolean hasData = rowDataList.stream().anyMatch(value -> !Validator.isEmpty(value) && !empty.equals(value));
        if (hasData) {
            T handler;
            try {
                handler = handler(rowDataList.toArray(new String[0]));
            } catch (Exception e) {
                //保留异常信息，便于定位失败行的具体原因
                error.add("第 [" + currentRowNumber + "] 数据异常：" + e.getMessage() + "，请检查后重新导入");
                return;
            }
            if (handler == null) {
                //回调返回空结果时记录为异常行，避免 null 静默进入结果集
                error.add("第 [" + currentRowNumber + "] 数据解析结果为空，请检查后重新导入");
                return;
            }
            list.add(handler);
        }

        rowDataList.clear();
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment xssfComment) {

        if (cellReference == null) {
            return;
        }

        //判断行是否在可读的范围(比可读行小，不读)
        if (currentRowNumber < beginRow) {
            return;
        }

        //列号取自单元格真实引用，稀疏/空单元格不会导致列错位
        int columnIndex = new CellReference(cellReference).getCol() + 1;
        if (columnIndex < beginCell || columnIndex > endCell) {
            return;
        }

        String value;
        if (Validator.isEmpty(formattedValue)) {
            value = empty;
        } else {
            //处理Excel中特殊的空格(字面替换，避免每个单元格重复编译正则)
            value = formattedValue.replace("\u00A0", "");
        }

        //按列槽位存放，缺口(稀疏列)以 empty 占位
        int slot = columnIndex - beginCell;
        while (rowDataList.size() <= slot) {
            rowDataList.add(empty);
        }
        rowDataList.set(slot, value);
    }

    public abstract T handler(String[] row);

    public Boolean getResults() {
        return error.isEmpty();
    }

}
