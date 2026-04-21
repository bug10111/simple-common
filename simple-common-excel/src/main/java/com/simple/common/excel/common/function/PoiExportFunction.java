package com.simple.common.excel.common.function;

import org.apache.poi.ss.usermodel.Row;

/**
 * POI导出函数式接口。
 * <p>
 * 用于自定义Excel导出时的单元格数据填充逻辑。通过实现此接口,可以灵活控制每一行数据的写入方式,
 * 支持复杂的数据转换、格式化、合并单元格等操作。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>自定义单元格样式：设置字体、颜色、边框等</li>
 *   <li>数据转换：将枚举值转换为中文、日期格式化等</li>
 *   <li>合并单元格：根据业务规则合并相同值的单元格</li>
 *   <li>条件渲染：根据不同条件显示不同的内容或样式</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 定义导出函数
 * PoiExportFunction<User> exportFunction = (row, user) -> {
 *     row.createCell(0).setCellValue(user.getId());
 *     row.createCell(1).setCellValue(user.getName());
 *     
 *     // 性别转换
 *     Cell genderCell = row.createCell(2);
 *     genderCell.setCellValue(user.getGender() == 1 ? "男" : "女");
 *     
 *     // 日期格式化
 *     Cell dateCell = row.createCell(3);
 *     dateCell.setCellValue(DateUtils.format(user.getCreateTime(), "yyyy-MM-dd"));
 * };
 * 
 * // 使用导出函数
 * excelService.export("用户列表", userList, exportFunction);
 * }</pre>
 *
 * @param <T> 导出数据实体类型
 * @author qty
 */
@FunctionalInterface
public interface PoiExportFunction<T> {
    
    /**
     * 执行单元格数据填充
     * <p>
     * 在Excel导出过程中,对每一行数据调用此方法进行单元格填充。
     * 可以根据业务需求自定义单元格的值、样式、格式等。
     * </p>
     *
     * @param row    当前行的POI Row对象,可通过createCell创建单元格
     * @param entity 当前行对应的数据实体对象
     */
    void execute(Row row, T entity);
}
