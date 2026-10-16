package com.simple.common.auth.server.exchange.sync;

/**
 * 实例同步记录存储接口（投递引擎 SPI，授权中心实现）
 * <p>
 * 投递引擎在每次投递落定（送达/超限失败）时写入同步记录；
 * 查询呈现面由授权中心管理端承载。
 * </p>
 *
 * @author qty
 */
public interface SyncRecordStore {

    /**
     * 保存一条同步记录
     *
     * @param record 同步记录
     */
    void save(SyncRecord record);

}
