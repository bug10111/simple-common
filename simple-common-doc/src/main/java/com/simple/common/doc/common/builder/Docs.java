package com.simple.common.doc.common.builder;

import com.deepoove.poi.data.NumberingFormat;
import com.deepoove.poi.data.Numberings;
import com.deepoove.poi.data.PictureRenderData;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.data.Rows;
import com.deepoove.poi.data.Tables;
import com.deepoove.poi.data.TextRenderData;
import com.deepoove.poi.data.Texts;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.doc.common.function.DocFunction;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * Description: doc替换参数Builder
 *
 * @author qty
 */
public final class Docs {

    public static DocBuilder builder() {
        return new DocBuilder();
    }

    public static class DocBuilder {
        private final Map<String, Object> templateData = new ConcurrentHashMap<>();

        /**
         * 添加普通文本
         * code格式：{{code}}
         *
         * @param key   模板key
         * @param value 对应值(null 按 "" 处理,避免 poi-tl 渲染期 NPE)
         */
        public DocBuilder addStr(String key, String value) {
            return addStr(key, Texts.of(value == null ? "" : value).create());
        }

        /**
         * 添加带链接的文本
         * code格式：{{code}}
         *
         * @param key   模板key
         * @param value 对应值(null 按 "" 处理)
         * @param link  超链接
         */
        public DocBuilder addStrLink(String key, String value, String link) {
            return addStr(key, Texts.of(value == null ? "" : value).link(link).create());
        }

        /**
         * 添加带有颜色的文本
         * code格式：{{code}}
         *
         * @param key   模板key
         * @param value 对应值(null 按 "" 处理)
         * @param color 字体颜色
         */
        public DocBuilder addStrColor(String key, String value, String color) {
            return addStr(key, Texts.of(value == null ? "" : value).color(color).create());
        }

        /**
         * 添加条件判断数据
         * code格式：{{?code}}...{{/code}}
         * <p>
         * 值为 true 时渲染条件块内内容，为 false 或未提供时跳过整块，
         * 底层由 poi-tl 原生 Boolean 条件标签能力实现。
         * </p>
         * <p>模板写法示例：</p>
         * <pre>{@code
         * {{?isVip}}
         *   尊贵的VIP会员，您已享受专属折扣
         * {{/isVip}}
         * }</pre>
         *
         * @param key   模板key
         * @param value 条件值(null 视为 false,避免 ConcurrentHashMap 拒绝 null 值抛 NPE)
         */
        public DocBuilder addBoolean(String key, Boolean value) {
            templateData.put(Objects.requireNonNull(key, "key 不能为空"), Boolean.TRUE.equals(value));
            return this;
        }

        /**
         * 添加本地图片
         * code格式：{{@code}}
         *
         * @param key    模板key
         * @param url    对应值
         * @param width  宽
         * @param height 高
         */
        public DocBuilder addImgLocal(String key, String url, int width, int height) {
            return addImg(key, Pictures.ofLocal(url).size(width, height).create());
        }

        /**
         * 添加图片流
         * code格式：{{@code}}
         *
         * @param key         模板key
         * @param inputStream 文件流
         * @param width       宽
         * @param height      高
         */
        public DocBuilder addImgInputStream(String key, InputStream inputStream, int width, int height) {
            return addImg(key, Pictures.ofStream(inputStream).size(width, height).create());
        }

        /**
         * 添加网络图片
         * code格式：{{@code}}
         *
         * @param key   模板key
         * @param url   图片地址
         * @param width 宽
         * @param height 高
         */
        public DocBuilder addImgUrl(String key, String url, int width, int height) {
            return addImg(key, Pictures.ofUrl(url).size(width, height).create());
        }

        /**
         * 添加文本
         * code格式：{{code}}
         *
         * @param key   模板key
         * @param value 对应值
         */
        public DocBuilder addStr(String key, TextRenderData value) {
            templateData.put(Objects.requireNonNull(key, "key 不能为空"), value);
            return this;
        }

        /**
         * 添加图片
         * code格式：{{@code}}
         *
         * @param key   模板key
         * @param value 对应值
         */
        public DocBuilder addImg(String key, PictureRenderData value) {
            templateData.put(Objects.requireNonNull(key, "key 不能为空"), value);
            return this;
        }

        /**
         * 添加表格
         * code格式：{{#code}}
         *
         * @param key          模板key
         * @param percentWidth 表格宽度百分比，例如：90
         * @param head         表头
         * @param list         表数据
         * @param function     表数据处理函数
         */
        public <T> DocBuilder addTable(String key, String percentWidth, String[] head, List<T> list, DocFunction<T> function) {
            addTable(key, percentWidth, 15, head, list, function);
            return this;
        }

        /**
         * 添加表格
         * code格式：{{#code}}
         *
         * @param key          模板key
         * @param percentWidth 表格宽度百分比，例如：90
         * @param size         字体大小
         * @param head         表头
         * @param list         表数据
         * @param function     表数据处理函数
         */
        public <T> DocBuilder addTable(String key, String percentWidth, int size, String[] head, List<T> list, DocFunction<T> function) {
            AssertUtils.isTrue(head != null && head.length > 0, "addTable 表头数据不能为空，key：{}", key);
            Tables.TableBuilder builder = Tables.ofPercentWidth(percentWidth).center();
            builder.addRow(Rows.of(head).center().textFontSize(size).create());

            // 逐行填充表数据,空集合或空元素不产生表格行
            if (list != null) {
                list.forEach(t -> {
                    if (t != null) {
                        String[] row = function.createRow(t);
                        // 回调返回 null 会在 Rows.of 内部转成 NPE,在这里快速失败并指明来源
                        AssertUtils.isTrue(row != null, "addTable 行数据回调返回 null，key：{}", key);
                        builder.addRow(Rows.of(row).center().textFontSize(size).create());
                    }
                });
            }
            templateData.put(key, builder.create());
            return this;
        }

        /**
         * 添加列表(默认圆点格式)
         * code格式：{{*code}}
         *
         * @param key  模板key
         * @param list 列表数据
         */
        public DocBuilder addList(String key, List<String> list) {
            return addList(key, NumberingFormat.BULLET, list);
        }

        /**
         * 添加列表
         * code格式：{{*code}}
         *
         * @param key             模板key
         * @param numberingFormat 列表编号格式(null 回退为默认圆点格式)
         * @param list            列表数据
         */
        public DocBuilder addList(String key, NumberingFormat numberingFormat, List<String> list) {
            // 统一空列表归一化:入参为 null、空列表或仅含 null 元素时,产出一致的空编号渲染数据
            List<String> items = list == null ? List.of() : list.stream().filter(Objects::nonNull).toList();
            if (items.isEmpty()) {
                templateData.put(key, Numberings.create());
                return this;
            }
            Numberings.NumberingBuilder numberingBuilder = Numberings.of(numberingFormat == null ? NumberingFormat.BULLET : numberingFormat);
            items.forEach(numberingBuilder::addItem);
            templateData.put(key, numberingBuilder.create());
            return this;
        }

        /**
         * 获取参数集(防御性拷贝:外部对返回 Map 的修改不影响内部渲染数据,
         * 内部后续 Builder 操作也不会泄漏给外部)
         */
        public Map<String, Object> create() {
            return new HashMap<>(templateData);
        }
    }

}
