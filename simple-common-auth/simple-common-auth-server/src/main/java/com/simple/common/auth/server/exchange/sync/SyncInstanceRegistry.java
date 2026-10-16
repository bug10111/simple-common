package com.simple.common.auth.server.exchange.sync;

import java.util.List;

/**
 * 实例注册表供给接口（投递引擎 SPI，授权中心实现）
 * <p>
 * 投递引擎按客户端编码查询存活实例清单，逐实例回调其接收端点；
 * 存活判定（启用状态/注册超期过滤）由实现方承载。
 * </p>
 *
 * @author qty
 */
public interface SyncInstanceRegistry {

    /**
     * 查询指定客户端的全部存活实例
     *
     * @param clientId 客户端编码
     * @return 存活实例清单（无实例返回空集合）
     */
    List<SyncInstance> findAliveInstances(String clientId);

    /**
     * 查询全部已注册客户端编码（全员通知类载荷按此扇出）
     *
     * @return 客户端编码集合（无注册返回空集合）
     */
    List<String> findAllClientIds();

}
