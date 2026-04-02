package com.simple.common.oauth.start.utils;

import com.simple.common.core.utils.HttpServletUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 请求头帮助类
 *
 * @author 兄台丶请冷静
 */
public class HeadUtils {

    /**
     * 获取当前头
     */
    public static Map<String, String> getHead() {
        String header = HttpServletUtils.getRequest().getHeader("Authorization");
        Map<String, String> map = new HashMap<>();
        map.put("Authorization", header);
        return map;
    }

}
