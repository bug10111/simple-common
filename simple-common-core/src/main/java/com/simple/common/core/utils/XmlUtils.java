package com.simple.common.core.utils;

import cn.hutool.core.util.XmlUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import lombok.SneakyThrows;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created with IntelliJ IDEA
 * Description: xml操作类
 *
 * @author 兄台丶请冷静
 */
public class XmlUtils extends XmlUtil {

    /**
     * 将对象转化为xml字符串
     *
     * @param obj 对象
     */
    @SneakyThrows
    public static String convertToXml(Object obj) {
        JAXBContext context = JAXBContext.newInstance(obj.getClass());
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE); // 生成XML声明

        StringWriter writer = new StringWriter();
        marshaller.marshal(obj, writer);
        String xml = writer.toString();

        // 删除 standalone="yes" 属性
        if (xml.contains("standalone=\"yes\"")) {
            xml = xml.replace("standalone=\"yes\"", "");
        }
        return xml;
    }

    /**
     * 将xml字符串转化为对象
     *
     * @param xml   字符串
     * @param clazz 对象
     */
    @SneakyThrows
    public static <T> T convertToObject(String xml, Class<T> clazz) {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        StringReader reader = new StringReader(xml);
        return (T) unmarshaller.unmarshal(reader);
    }

}
