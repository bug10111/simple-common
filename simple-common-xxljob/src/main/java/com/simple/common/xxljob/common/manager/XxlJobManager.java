package com.simple.common.xxljob.common.manager;

import com.simple.common.xxljob.common.dto.CreateXxlJobTaskRequest;
import com.simple.common.xxljob.common.dto.UpdateXxlJobTaskRequest;

/**
 * Created with IntelliJ IDEA
 * Description: xxl-job远程请求接口
 *
 * @author qty
 */
public interface XxlJobManager {

    /**
     * 创建任务
     *
     * @param request 参数类
     * @return 任务主键
     */
    String create(CreateXxlJobTaskRequest request);

    /**
     * 修改任务
     *
     * @param request 参数类
     */
    void update(UpdateXxlJobTaskRequest request);

    /**
     * 删除任务
     *
     * @param id Id
     */
    void delete(Integer id);

    /**
     * 开启任务
     *
     * @param id 主键
     */
    void start(Integer id);

    /**
     * 关闭任务
     *
     * @param id 主键
     */
    void end(Integer id);

    /**
     * 立即执行
     *
     * @param id 主键
     */
    void trigger(Integer id);

}
