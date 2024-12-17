import cn.hutool.core.lang.tree.Tree;
import cn.hutool.json.JSONUtil;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.core.utils.RecursiveUtils;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 树形结构测试
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class RecursiveUtilsTest {

    @Test
    public void 自动树形结构Demo1() {
        List<Demo1> demos = getDemos();
        log.info("原始数据：[{}]", JSONUtil.toJsonPrettyStr(demos));

        List<Tree<String>> trees = RecursiveUtils.get(demos);
        log.info("递归数据：[{}]", JSONUtil.toJsonPrettyStr(trees));
    }

    /**
     * 获取原始数据
     *
     * @return
     */
    public List<Demo1> getDemos() {
        List<Demo1> list = new ArrayList<>();
        Demo1 demo1 = new Demo1();
        demo1.setId("1");
        demo1.setParentId(RecursiveUtils.initial_id);
        demo1.setSerial(2);
        demo1.setName("测试第一级1号");
        demo1.setName1("name1");
        demo1.setName2("name2");
        demo1.setName3("name3");
        list.add(demo1);

        Demo1 demo2 = new Demo1();
        demo2.setId("2");
        demo2.setParentId(RecursiveUtils.initial_id);
        demo2.setSerial(1);
        demo2.setName("测试第一级2号");
        demo2.setName1("name1");
        demo2.setName2("name2");
        demo2.setName3("name3");
        list.add(demo2);

        Demo1 demo11 = new Demo1();
        demo11.setId("3");
        demo11.setParentId("1");
        demo11.setSerial(1);
        demo11.setName("测试第一级的1号1小弟");
        demo11.setName1("name1");
        demo11.setName2("name2");
        demo11.setName3("name3");
        list.add(demo11);

        Demo1 demo12 = new Demo1();
        demo12.setId("4");
        demo12.setParentId("1");
        demo12.setSerial(1);
        demo12.setName("测试第一级的1号2小弟");
        demo12.setName1("name1");
        demo12.setName2("name2");
        demo12.setName3("name3");
        list.add(demo12);

        Demo1 demo21 = new Demo1();
        demo21.setId("5");
        demo21.setParentId("2");
        demo21.setSerial(1);
        demo21.setName("测试第一级的2号1小弟");
        demo21.setName1("name1");
        demo21.setName2("name2");
        demo21.setName3("name3");
        list.add(demo21);
        return list;
    }

    @Data
    @Accessors(chain = true)
    static class Demo1 {

        //核心参数
        private String id;

        private String parentId;

        private int serial;

        //业务数据
        private String name;

        private String name1;

        private String name2;

        private String name3;
    }

}
