package com.simple.common.auth.server.exchange.sync;

/**
 * 实例同步记录存储接口（投递引擎 SPI，授权中心实现）。
 * <p>
 * 投递引擎在每次投递落定（送达/超限失败）时写入同步记录；
 * 查询呈现面由授权中心管理端承载。
 * </p>
 *
 * <h3>实现契约：</h3>
 * <ul>
 *   <li><strong>追加语义</strong>：save 为追加写而非 upsert——记录本体不含去重键，失败重投会按
 *       存活实例重放任务，同一逻辑落定可能写入多条记录，实现方须容忍重复并按 syncTime 取最近</li>
 *   <li><strong>调用方吞异常</strong>：记录持久化失败会被投递 worker 捕获并记错误日志，不会中断投递主链路，
 *       也不做重试/补偿——落库不可用会造成审计空洞，实现方应在实现内部保障写入可靠性</li>
 *   <li><strong>线程安全</strong>：可能被投递线程并发调用，实现必须线程安全；record 及其字段非 null
 *       （由投递引擎构造保证）</li>
 * </ul>
 *
 * @author qty
 */
public interface SyncRecordStore {

    /**
     * 保存一条同步记录（追加语义，见类级契约）
     *
     * @param record 同步记录（非 null）
     */
    void save(SyncRecord record);

}
