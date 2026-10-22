package com.simple.common.auth.server.exchange.sync;

import java.util.List;

/**
 * 实例注册表供给接口（投递引擎 SPI，授权中心实现）
 * <p>
 * 投递引擎按客户端编码查询存活实例清单，逐实例回调其接收端点。
 * </p>
 *
 * <h3>实现契约：</h3>
 * <ul>
 *   <li><strong>存活判定</strong>：由实现方定义并保持一致（启用状态与注册 TTL 的组合语义；
 *       注册时间以实现方的时钟源比较）——投递正确性完全依赖该过滤，仅校验启用标记而不过滤
 *       注册超期实例会导致持续向失效端点投递且在 SPI 边界不可见</li>
 *   <li><strong>永不返回 null</strong>：无实例/无客户端时返回空集合；消费方（投递 worker/扇出）
 *       直接对返回值做 isEmpty/遍历，返回 null 将在投递层 NPE 且无 SPI 违约提示</li>
 *   <li><strong>线程安全快照</strong>：实现必须线程安全，且返回不可变的防御性快照——注册线程会
 *       并发变更注册表，返回内部集合会在扇出遍历中引发 ConcurrentModificationException 或被外部篡改</li>
 *   <li><strong>两次查询相互独立</strong>：findAliveInstances 与 findAllClientIds 无跨调用快照保证，
 *       组合使用可能漏掉刚注册或包含刚注销的客户端</li>
 * </ul>
 *
 * @author qty
 */
public interface SyncInstanceRegistry {

    /**
     * 查询指定客户端的全部存活实例
     *
     * @param clientId 客户端编码
     * @return 存活实例清单（不可变快照，无实例返回空集合，永不返回 null）
     */
    List<SyncInstance> findAliveInstances(String clientId);

    /**
     * 查询全部已注册客户端编码（全员通知类载荷按此扇出）
     *
     * @return 客户端编码集合（不可变快照，无注册返回空集合，永不返回 null）
     */
    List<String> findAllClientIds();

}
