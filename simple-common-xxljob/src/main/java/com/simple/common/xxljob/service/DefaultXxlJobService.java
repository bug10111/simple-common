package com.simple.common.xxljob.service;

import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.xxljob.common.dto.CreateXxlJobTaskRequest;
import com.simple.common.xxljob.common.dto.UpdateXxlJobTaskRequest;
import com.simple.common.xxljob.common.manager.XxlJobManager;
import com.simple.common.xxljob.common.service.XxlJobService;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 装配说明：本类由 {@code XxlJobConfig} 以显式 {@code @Bean} 注册（带 @ConditionalOnMissingBean），
 * 不再使用 {@code @Service}——{@code xxl.job.open} 未开启时整组组件不装配，
 * 且避免宿主工程误扫本包时在配置缺失状态下拉起半套组件。
 *
 * @author qty
 */
public class DefaultXxlJobService implements XxlJobService {

    private final XxlJobManager xxlJobManager;

    public DefaultXxlJobService(XxlJobManager xxlJobManager) {
        this.xxlJobManager = xxlJobManager;
    }

    @Override
    public Integer create(CreateXxlJobTaskRequest request) {
        // 服务边界统一校验：Admin 未返回任务ID时在此边界抛出可读错误，
        // 而不是把 null/空串透传给调用方、由调用方的 Integer 解析处抛出难定位的 NPE/NumberFormatException
        Objects.requireNonNull(request, "request 不能为 null");
        Integer jobId = xxlJobManager.create(request);
        if (jobId == null) {
            throw new DefaultException(DefaultExceptionEnum.ERROR, "xxl-job 创建任务未返回任务ID");
        }
        return jobId;
    }

    @Override
    public void update(UpdateXxlJobTaskRequest request) {
        Objects.requireNonNull(request, "request 不能为 null");
        Objects.requireNonNull(request.getId(), "任务 id 不能为 null");
        xxlJobManager.update(request);
    }

    @Override
    public void delete(Integer id) {
        requireJobId(id);
        xxlJobManager.delete(id);
    }

    @Override
    public void start(Integer id) {
        requireJobId(id);
        xxlJobManager.start(id);
    }

    @Override
    public void end(Integer id) {
        requireJobId(id);
        xxlJobManager.end(id);
    }

    @Override
    public void trigger(Integer id) {
        requireJobId(id);
        xxlJobManager.trigger(id);
    }

    /**
     * 服务边界统一校验任务ID非空：id 为 null 时在边界快速失败并给出明确提示，
     * 而不是由管理器 String.valueOf(null) 拼出 "null" 透传 Admin、以误导性的下游错误暴露
     */
    private void requireJobId(Integer id) {
        if (id == null) {
            throw new DefaultException(DefaultExceptionEnum.ERROR, "任务 id 不能为空");
        }
    }
}
