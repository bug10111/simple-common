package com.simple.common.excel.service;

import com.simple.common.excel.common.service.PoiReadService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultPoiReadService implements PoiReadService {

    @Override
    @SneakyThrows
    public void read(String filename, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler) {
        OPCPackage pkg = OPCPackage.open(filename, PackageAccess.READ);
        execution(pkg, sheetContentsHandler);
    }

    @Override
    @SneakyThrows
    public void read(InputStream inputStream, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler) {
        OPCPackage pkg = OPCPackage.open(inputStream);
        execution(pkg, sheetContentsHandler);
    }

    /**
     * 执行实现
     *
     * @param pkg                  文档内部结构的访问管理器
     * @param sheetContentsHandler 实现
     */
    @SneakyThrows
    protected void execution(OPCPackage pkg, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler) {
        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
        XSSFReader xssfReader = new XSSFReader(pkg);
        StylesTable styles = xssfReader.getStylesTable();
        XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
        InputStream stream = null;
        while (iter.hasNext()) {
            stream = iter.next();
            parserSheetXml(styles, strings, sheetContentsHandler, stream);
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * 解析excel 转换成xml
     */
    @SneakyThrows
    protected void parserSheetXml(StylesTable styles, ReadOnlySharedStringsTable strings, XSSFSheetXMLHandler.SheetContentsHandler sheetHandler,
                                  InputStream sheetInputStream) {
        DataFormatter formatter = new DataFormatter();
        InputSource sheetSource = new InputSource(sheetInputStream);

        // 使用 SAXParserFactory 创建新的 SAXParser，并获取 XMLReader
        //        XMLReader sheetParser = SAXHelper.newXMLReader();
        XMLReader sheetParser = XMLHelper.newXMLReader();

        ContentHandler handler = new XSSFSheetXMLHandler(styles, null, strings, sheetHandler, formatter, false);
        sheetParser.setContentHandler(handler);
        sheetParser.parse(sheetSource);
    }
}
