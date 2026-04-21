package com.simple.common.xxljob.common.manager;

import com.simple.common.xxljob.common.dto.CreateXxlJobTaskRequest;
import com.simple.common.xxljob.common.dto.UpdateXxlJobTaskRequest;

/**
 * XXL-JOB任务管理器接口。
 * <p>
 * 用于通过代码动态管理XXL-JOB分布式任务调度平台中的定时任务,支持任务的创建、修改、删除、启停等操作。
 * 默认实现 {@link com.simple.common.xxljob.manager.DefaultXxlJobManager} 基于XXL-JOB Admin API实现远程调用。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>动态任务创建：业务系统根据配置自动创建定时任务</li>
 *   <li>任务生命周期管理：在业务流程中控制任务的启用和禁用</li>
 *   <li>任务批量操作：批量创建、修改或删除多个任务</li>
 *   <li>任务监控：查询任务执行状态和执行记录</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class CustomXxlJobManager implements XxlJobManager {
 *     @Value("${xxl.job.admin.addresses}")
 *     private String adminAddresses;
 *     
 *     @Override
 *     public String create(CreateXxlJobTaskRequest request) {
 *         // 调用XXL-JOB Admin API创建任务
 *         String url = adminAddresses + "/jobinfo/add";
 *         HttpResponse response = HttpUtil.post(url, JsonUtils.toJsonStr(request));
 *         return parseJobId(response);
 *     }
 *     
 *     // 其他方法实现...
 * }
 * }</pre>
 *
 * @author qty
 */
public interface XxlJobManager {

    /**
     * 创建定时任务
     * <p>
     * 在XXL-JOB Admin中创建一个新的定时任务,返回任务ID。
     * 创建后的任务默认为停止状态,需要调用start方法启动。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * CreateXxlJobTaskRequest request = new CreateXxlJobTaskRequest();
     * request.setJobDesc("订单超时取消任务");
     * request.setAuthor("system");
     * request.setScheduleConf("0 0/5 * * * ?");  // 每5分钟执行一次
     * request.setExecutorHandler("orderTimeoutHandler");
     * request.setExecutorParam("{\"timeoutMinutes\":30}");
     * 
     * String jobId = xxlJobManager.create(request);
     * log.info("创建任务成功,任务ID: {}", jobId);
     * 
     * // 启动任务
     * xxlJobManager.start(Integer.parseInt(jobId));
     * }</pre>
     *
     * @param request 任务创建请求对象,包含任务描述、Cron表达式、执行器Handler等配置
     * @return 任务ID字符串,创建失败时抛出异常
     * @throws RuntimeException 当参数校验失败或API调用失败时抛出异常
     */
    String create(CreateXxlJobTaskRequest request);

    /**
     * 修改定时任务
     * <p>
     * 更新已存在的定时任务配置,包括Cron表达式、执行参数、任务描述等。
     * 修改后任务会自动重启以应用新配置。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * UpdateXxlJobTaskRequest request = new UpdateXxlJobTaskRequest();
     * request.setId(123);  // 任务ID
     * request.setScheduleConf("0 0/10 * * * ?");  // 修改为每10分钟执行
     * request.setJobDesc("订单超时取消任务(已优化)");
     * 
     * xxlJobManager.update(request);
     * }</pre>
     *
     * @param request 任务更新请求对象,必须包含任务ID
     * @throws RuntimeException 当任务不存在或参数校验失败时抛出异常
     */
    void update(UpdateXxlJobTaskRequest request);

    /**
     * 删除定时任务
     * <p>
     * 从XXL-JOB Admin中永久删除指定的定时任务。
     * 删除前建议先停止任务,避免正在执行的任务被中断。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 先停止任务
     * xxlJobManager.end(jobId);
     * 
     * // 再删除任务
     * xxlJobManager.delete(jobId);
     * }</pre>
     *
     * @param id 任务ID
     * @throws RuntimeException 当任务不存在或删除失败时抛出异常
     */
    void delete(Integer id);

    /**
     * 启动定时任务
     * <p>
     * 启动已停止的定时任务,使其按照Cron表达式定期执行。
     * 如果任务已经在运行,调用此方法不会产生副作用。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 创建任务后启动
     * String jobId = xxlJobManager.create(request);
     * xxlJobManager.start(Integer.parseInt(jobId));
     * 
     * // 或者恢复之前停止的任务
     * xxlJobManager.start(existingJobId);
     * }</pre>
     *
     * @param id 任务ID
     * @throws RuntimeException 当任务不存在或启动失败时抛出异常
     */
    void start(Integer id);

    /**
     * 停止定时任务
     * <p>
     * 暂停正在运行的定时任务,任务将不再按照Cron表达式触发执行。
     * 已触发但未执行完的任务会继续执行完成。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 临时停止任务(如系统维护期间)
     * xxlJobManager.end(jobId);
     * 
     * // 维护完成后重新启动
     * xxlJobManager.start(jobId);
     * }</pre>
     *
     * @param id 任务ID
     * @throws RuntimeException 当任务不存在或停止失败时抛出异常
     */
    void end(Integer id);

    /**
     * 立即执行任务
     * <p>
     * 手动触发一次任务执行,不等待Cron表达式到达触发时间。
     * 常用于测试任务逻辑或紧急执行业务操作。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 测试新创建的任务
     * String jobId = xxlJobManager.create(request);
     * xxlJobManager.trigger(Integer.parseInt(jobId));
     * 
     * // 查看执行日志确认任务是否正常
     * List<JobLog> logs = xxlJobService.queryLog(Integer.parseInt(jobId), 1, 10);
     * }</pre>
     *
     * @param id 任务ID
     * @throws RuntimeException 当任务不存在或触发失败时抛出异常
     */
    void trigger(Integer id);

}
