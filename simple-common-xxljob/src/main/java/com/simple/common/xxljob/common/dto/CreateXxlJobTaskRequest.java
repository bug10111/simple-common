package com.simple.common.xxljob.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "手动创建xxl-job的请求对象")
public class CreateXxlJobTaskRequest {

    @Schema(description = "执行器主键ID")
    @NotEmpty(message = "执行器住建不能为空")
    private Integer jobGroup;

    @Schema(description = "调度配置，值含义取决于调度类型,一般是时间cron")
    @NotEmpty(message = "调度配置不能为空")
    private String scheduleConf;

    @Schema(description = "JobHandler，任务Handler名称,需要和项目里注解内容一致")
    @NotEmpty(message = "executorHandler不能为空")
    private String executorHandler;

    @Schema(description = "负责人")
    @NotEmpty(message = "负责人不能为空")
    private String author;

    @Schema(description = "任务描述")
    @NotEmpty(message = "任务描述不能为空")
    private String jobDesc;

    @Schema(description = "调度类型,默认CRON")
    private String scheduleType = "CRON";

    @Schema(description = "运行模式")
    private String glueType = "BEAN";

    @Schema(description = "执行器路由策略，默认第一个")
    private String executorRouteStrategy = "FIRST";

    @Schema(description = "调度过期策略")
    private String misfireStrategy = "DO_NOTHING";

    @Schema(description = "阻塞处理策略")
    private String executorBlockStrategy = "SERIAL_EXECUTION";

    @Schema(description = "报警邮件")
    private String alarmEmail;

    @Schema(description = "任务参数")
    private String executorParam;

    @Schema(description = "子任务Id")
    private String childJobId;

    @Schema(description = "任务执行超时时间，单位秒")
    private int executorTimeout = 30;

    @Schema(description = "失败重试次数")
    private int executorFailRetryCount = 3;

}
