package com.simple.common.core.utils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * Description: xml操作类
 * <p>安全说明：反序列化前统一禁用 DTD 与外部实体，防止 XXE（本地文件读取/SSRF）攻击。</p>
 *
 * @author qty
 */
public final class XmlUtils {

    /**
     * JAXBContext 创建开销大且线程安全，按类型缓存复用
     */
    private static final Map<Class<?>, JAXBContext> CONTEXT_CACHE = new ConcurrentHashMap<>();

    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private XmlUtils() {
    }

    /**
     * 将对象转化为xml字符串
     *
     * @param obj 对象
     * @return xml字符串（带 XML 声明，不含 standalone 属性）
     */
    public static String convertToXml(Object obj) {
        Objects.requireNonNull(obj, "obj must not be null");
        try {
            Marshaller marshaller = getContext(obj.getClass()).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            // JAXB_FRAGMENT=true 时不自动写声明，由本类统一拼接标准声明，避免 standalone="yes" 字符串后处理
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            StringWriter writer = new StringWriter();
            writer.write(XML_DECLARATION);
            marshaller.marshal(obj, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("对象转XML失败: " + obj.getClass().getName(), e);
        }
    }

    /**
     * 将xml字符串转化为对象
     * <p>解析前禁用 DTD 与外部实体：{@code SUPPORT_DTD=false}、
     * {@code IS_SUPPORTING_EXTERNAL_ENTITIES=false}，杜绝 XXE 攻击面。</p>
     *
     * @param xml   字符串
     * @param clazz 对象
     * @return 反序列化后的对象
     */
    public static <T> T convertToObject(String xml, Class<T> clazz) {
        Objects.requireNonNull(xml, "xml must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");
        try {
            XMLInputFactory inputFactory = createHardenedInputFactory();
            XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(xml));
            try {
                Unmarshaller unmarshaller = getContext(clazz).createUnmarshaller();
                return clazz.cast(unmarshaller.unmarshal(reader));
            } finally {
                reader.close();
            }
        } catch (JAXBException | XMLStreamException e) {
            throw new IllegalStateException("XML转对象失败: " + clazz.getName(), e);
        }
    }

    /**
     * 创建禁用 DTD 与外部实体的 XMLInputFactory（XXE 防护核心）
     */
    private static XMLInputFactory createHardenedInputFactory() {
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        return inputFactory;
    }

    private static JAXBContext getContext(Class<?> clazz) throws JAXBException {
        return CONTEXT_CACHE.computeIfAbsent(clazz, key -> {
            try {
                return JAXBContext.newInstance(key);
            } catch (JAXBException e) {
                throw new IllegalStateException("创建JAXBContext失败: " + key.getName(), e);
            }
        });
    }
}
