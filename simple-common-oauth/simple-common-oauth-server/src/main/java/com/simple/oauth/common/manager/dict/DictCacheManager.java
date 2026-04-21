package com.simple.oauth.common.manager.dict;

import com.simple.oauth.common.dto.sysDictData.SysDictDatasResponse;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 字典缓存接口
 *
 * @author qty
 */
public interface DictCacheManager {

    /**
     * 根据类型集合获取缓存
     * @param types 类型集合
     */
    List<SysDictDatasResponse> get(String types);

}
