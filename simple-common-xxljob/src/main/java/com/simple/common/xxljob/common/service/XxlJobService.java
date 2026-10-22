package com.simple.common.xxljob.common.service;

import com.simple.common.xxljob.common.dto.CreateXxlJobTaskRequest;
import com.simple.common.xxljob.common.dto.UpdateXxlJobTaskRequest;

/**
 * XXL-Job 定时任务管理服务接口。
 * <p>
 * 提供通过代码动态管理XXL-Job定时任务的功能,包括创建、修改、删除、启停、触发等操作。
 * 默认实现 {@code DefaultXxlJobService} 基于 XXL-Job Admin API。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>动态创建定时任务：根据业务需求动态生成任务</li>
 *   <li>任务生命周期管理：启用、禁用、删除任务</li>
 *   <li>手动触发任务：立即执行某个任务,无需等待调度时间</li>
 *   <li>任务配置修改：动态调整任务的Cron表达式、参数等</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private XxlJobService xxlJobService;
 *
 * // 创建定时任务
 * CreateXxlJobTaskRequest request = new CreateXxlJobTaskRequest();
 * request.setJobGroup(1);                                   // 执行器主键,必填
 * request.setJobDesc("订单超时取消任务");
 * request.setScheduleConf("0 0/5 * * * ?");  // 每5分钟执行一次
 * request.setExecutorHandler("orderTimeoutHandler");
 * request.setExecutorParam("{\"timeoutMinutes\": 30}");
 * Integer jobId = xxlJobService.create(request);
 *
 * // 启动任务
 * xxlJobService.start(jobId);
 *
 * // 立即执行任务
 * xxlJobService.trigger(jobId);
 * }</pre>
 *
 * @author qty
 */
public interface XxlJobService {

    /**
     * 创建定时任务
     * <p>
     * 在XXL-Job Admin中注册一个新的定时任务。
     * 任务创建后默认为停止状态,需要调用 {@link #start(Integer)} 启动。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * CreateXxlJobTaskRequest request = new CreateXxlJobTaskRequest();
     * request.setJobGroup(1);
     * request.setJobDesc("数据同步任务");
     * request.setScheduleConf("0 0 2 * * ?");  // 每天凌晨2点执行
     * request.setExecutorHandler("dataSyncHandler");
     * request.setExecutorParam("{\"source\": \"mysql\", \"target\": \"es\"}");
     *
     * Integer jobId = xxlJobService.create(request);
     * log.info("任务创建成功, jobId: {}", jobId);
     * }</pre>
     *
     * @param request 任务创建请求对象,不能为null,包含任务描述、Cron表达式、执行器等信息
     * @return 新创建的任务ID(Admin侧任务主键为整数,与 {@link #start(Integer)} 等
     *         生命周期方法的 Integer 口径一致,调用方无需再手工解析字符串)
     * @throws RuntimeException 当任务创建失败时抛出异常(如Cron表达式错误、执行器不存在、
     *                          Admin未返回任务ID等)
     */
    Integer create(CreateXxlJobTaskRequest request);

    /**
     * 修改定时任务配置
     * <p>
     * 更新已存在任务的配置信息,如Cron表达式、任务参数等。
     * 所传字段全量覆盖 Admin 侧配置,需要保留的字段必须显式回填原值。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * UpdateXxlJobTaskRequest request = new UpdateXxlJobTaskRequest();
     * request.setId(123);
     * request.setScheduleConf("0 0 3 * * ?");  // 修改为每天凌晨3点执行
     * request.setExecutorParam("{\"source\": \"oracle\", \"target\": \"es\"}");
     *
     * xxlJobService.update(request);
     * }</pre>
     *
     * @param request 任务更新请求对象,不能为null且必须包含任务ID(id 为非空正整数)
     * @throws RuntimeException 当任务不存在或更新失败时抛出异常
     */
    void update(UpdateXxlJobTaskRequest request);

    /**
     * 删除定时任务
     * <p>
     * 从XXL-Job Admin中永久删除指定的定时任务。
     * 删除后无法恢复,请谨慎操作。
     * </p>
     *
     * @param id 任务ID,不能为null
     * @throws RuntimeException 当任务不存在或删除失败时抛出异常
     */
    void delete(Integer id);

    /**
     * 启动定时任务
     * <p>
     * 启用指定的定时任务,使其按照Cron表达式定期执行。
     * 注意:「任务已在运行时不重复操作」的幂等语义由 Admin 侧实现保证,
     * 本接口每次调用都会真实请求 Admin,重复调用的行为以 Admin 版本为准。
     * </p>
     *
     * @param id 任务ID,不能为null
     * @throws RuntimeException 当任务不存在或启动失败时抛出异常
     */
    void start(Integer id);

    /**
     * 停止定时任务
     * <p>
     * 暂停指定的定时任务,使其不再按照Cron表达式执行。
     * 注意:重复调用会再次请求 Admin,Admin 侧返回行为以实际版本为准。
     * </p>
     *
     * @param id 任务ID,不能为null
     * @throws RuntimeException 当任务不存在或停止失败时抛出异常
     */
    void end(Integer id);

    /**
     * 立即触发任务执行
     * <p>
     * 手动触发指定的定时任务立即执行一次,不受Cron表达式限制。
     * 常用于测试任务逻辑或紧急执行某个任务。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 立即执行数据同步任务
     * xxlJobService.trigger(taskId);
     * log.info("任务已触发, taskId: {}", taskId);
     * }</pre>
     *
     * @param id 任务ID,不能为null
     * @throws RuntimeException 当任务不存在或触发失败时抛出异常
     */
    void trigger(Integer id);
}
