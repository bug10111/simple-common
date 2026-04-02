package com.simple.common.oauth.start.common.manager;

import com.simple.common.oauth.start.common.dto.SysDictDatasResponse;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 字典相关接口
 *
 * @author qty
 */
public interface DictManager {

    /**
     * 获取字典数据
     *
     * @param type 字典类型
     * @return 字典数据集合
     */
    Map<String, List<SysDictDatasResponse>> getDict(List<String> type);

}
