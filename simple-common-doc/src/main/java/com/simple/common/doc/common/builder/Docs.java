package com.simple.common.doc.common.builder;

import com.deepoove.poi.data.*;
import com.simple.common.doc.common.function.DocFunction;

import java.io.InputStream;
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
         * @param value 对应值
         */
        public DocBuilder addStr(String key, String value) {
            return addStr(key, Texts.of(value).create());
        }

        /**
         * 添加带链接的文本
         * code格式：{{code}}
         *
         * @param key   模板key
         * @param value 对应值
         * @param link  超链接
         */
        public DocBuilder addStrLink(String key, String value, String link) {
            return addStr(key, Texts.of(value).link(link).create());
        }

        /**
         * 添加带有样式的文本
         * code格式：{{code}}
         *
         * @param key   模板key
         * @param value 对应值
         * @param color 字体颜色
         */
        public DocBuilder addStrColor(String key, String value, String color) {
            return addStr(key, Texts.of(value).color(color).create());
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
         * @param key    模板key
         * @param url    图片地址
         * @param width  宽
         * @param height 高
         */
        public DocBuilder addImgInputUrl(String key, String url, int width, int height) {
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
            templateData.put(key, value);
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
            templateData.put(key, value);
            return this;
        }

        /**
         * 添加表格
         * code格式：{{#code}}
         *
         * @param key          模板key
         * @param percentWidth 表格宽度百分比，例如：90%
         * @param head         表头
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
         * @param percentWidth 表格宽度百分比，例如：90%
         * @param size         字体大小
         * @param head         表头
         * @param function     表数据处理函数
         */
        public <T> DocBuilder addTable(String key, String percentWidth, int size, String[] head, List<T> list, DocFunction<T> function) {
            Tables.TableBuilder builder = Tables.ofPercentWidth(percentWidth).center();
            builder.addRow(Rows.of(head).center().textFontSize(size).create());

            if (list != null) {
                list.forEach(t -> {
                    if (t != null) {
                        String[] row = function.createRow(t);
                        builder.addRow(Rows.of(row).center().textFontSize(size).create());
                    }
                });
            }
            templateData.put(key, builder.create());
            return this;
        }

        /**
         * 添加列表
         * code格式：{{*code}}
         *
         * @param key  模板key
         * @param list 列表数据
         */
        public DocBuilder addList(String key, List<String> list) {
            if (list == null || list.isEmpty()) {
                templateData.put(key, Numberings.create());
                return this;
            }
            Numberings.NumberingBuilder numberingBuilder = Numberings.of(NumberingFormat.BULLET);
            list.stream().filter(Objects::nonNull).forEach(numberingBuilder::addItem);
            templateData.put(key, numberingBuilder.create());
            return this;
        }

        /**
         * 添加列表
         * code格式：{{*code}}
         *
         * @param key             模板key
         * @param numberingFormat 列表编号格式
         * @param list            列表数据
         */
        public DocBuilder addList(String key, NumberingFormat numberingFormat, List<String> list) {
            if (list == null || list.isEmpty()) {
                templateData.put(key, Numberings.create());
                return this;
            }
            Numberings.NumberingBuilder numberingBuilder = Numberings.of(numberingFormat);
            list.stream().filter(Objects::nonNull).forEach(numberingBuilder::addItem);
            templateData.put(key, numberingBuilder.create());
            return this;
        }

        /**
         * 获取参数集
         */
        public Map<String, Object> create() {
            return templateData;
        }
    }

}
