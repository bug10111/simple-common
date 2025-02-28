import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.Tables;
import com.deepoove.poi.data.style.BorderStyle;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public class DocTest {

    public static void generateWordFile(Map<String, Object> templateData, String templateFilePath, String outputFilePath) {
        // 读取模板文件
        try (InputStream templateIn = DocTest.class.getResourceAsStream(templateFilePath)) {
            // 生成模板文件
            XWPFTemplate template = XWPFTemplate.compile(templateIn).render(templateData);
            template.writeAndClose(new FileOutputStream(outputFilePath));
            // 这个目的是：生成文件之后调用 cmd 打开本地文件，实际生产不需要该操作
            // Runtime.getRuntime().exec(String.format("cmd /c %s", outputFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("table", Tables.of(new String[][] {
                        new String[] { "00", "01" },
                        new String[] { "10", "11" }
        }).border(BorderStyle.DEFAULT).create());
        generateWordFile(map,"/doc/新广丰综合批发市场租赁合同书模板.docx","C:/123.docx");
    }

}
