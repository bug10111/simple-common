package com.simple.common.xxljob.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 策略类字段以 {@code @Pattern} 限定官方 Admin 认可的取值集合：非法取值在 API 入参边界
 * 即被拒绝，而不是静默透传到 Admin 后变成永不触发的坏任务。
 * 数值字段（executorTimeout/executorFailRetryCount）声明为可空 Integer：
 * 仅在调用方显式赋值时才提交，未赋值不携带（交由 Admin 侧默认值兜底），
 * 避免「创建默认值被原样透传、更新请求漏填字段时静默重置既有配置」。
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Schema(description = "手动创建xxl-job的请求对象")
public class CreateXxlJobTaskRequest {

    @Schema(description = "执行器主键ID")
    @NotNull(message = "执行器主键不能为空")
    private Integer jobGroup;

    @Schema(description = "调度配置，值含义取决于调度类型,一般是时间cron")
    @NotBlank(message = "调度配置不能为空")
    private String scheduleConf;

    @Schema(description = "JobHandler，任务Handler名称,需要和项目里注解内容一致")
    @NotBlank(message = "executorHandler不能为空")
    private String executorHandler;

    @Schema(description = "负责人")
    @NotBlank(message = "负责人不能为空")
    private String author;

    @Schema(description = "任务描述")
    @NotBlank(message = "任务描述不能为空")
    private String jobDesc;

    @Schema(description = "调度类型：NONE | CRON | FIX_RATE，默认CRON")
    @Pattern(regexp = "NONE|CRON|FIX_RATE", message = "scheduleType 仅支持 NONE/CRON/FIX_RATE")
    private String scheduleType = "CRON";

    @Schema(description = "运行模式：BEAN | GLUE_GROOVY | GLUE_SHELL | GLUE_PYTHON | GLUE_PHP | GLUE_NODEJS | GLUE_POWERSHELL")
    @Pattern(regexp = "BEAN|GLUE_GROOVY|GLUE_SHELL|GLUE_PYTHON|GLUE_PHP|GLUE_NODEJS|GLUE_POWERSHELL",
            message = "glueType 取值非法，仅支持 BEAN/GLUE_* 运行模式")
    private String glueType = "BEAN";

    @Schema(description = "执行器路由策略：FIRST | LAST | ROUND | RANDOM | CONSISTENT_HASH | LEAST_FREQUENTLY_USED | LEAST_RECENTLY_USED | FAILOVER | BUSYOVER | SHARDING_BROADCAST，默认FIRST")
    @Pattern(regexp = "FIRST|LAST|ROUND|RANDOM|CONSISTENT_HASH|LEAST_FREQUENTLY_USED|LEAST_RECENTLY_USED|FAILOVER|BUSYOVER|SHARDING_BROADCAST",
            message = "executorRouteStrategy 取值非法，仅支持官方路由策略常量")
    private String executorRouteStrategy = "FIRST";

    @Schema(description = "调度过期策略：DO_NOTHING | FIRE_ONCE_NOW")
    @Pattern(regexp = "DO_NOTHING|FIRE_ONCE_NOW", message = "misfireStrategy 仅支持 DO_NOTHING/FIRE_ONCE_NOW")
    private String misfireStrategy = "DO_NOTHING";

    @Schema(description = "阻塞处理策略：SERIAL_EXECUTION | DISCARD_LATER | COVER_EARLY")
    @Pattern(regexp = "SERIAL_EXECUTION|DISCARD_LATER|COVER_EARLY",
            message = "executorBlockStrategy 仅支持 SERIAL_EXECUTION/DISCARD_LATER/COVER_EARLY")
    private String executorBlockStrategy = "SERIAL_EXECUTION";

    @Schema(description = "报警邮件")
    @Email(message = "alarmEmail 不是合法的邮箱地址")
    private String alarmEmail;

    @Schema(description = "任务参数")
    private String executorParam;

    @Schema(description = "子任务Id，多个以英文逗号分隔的任务ID")
    @Pattern(regexp = "\\d+(,\\d+)*", message = "childJobId 仅支持逗号分隔的任务ID")
    private String childJobId;

    @Schema(description = "任务执行超时时间，单位秒，0表示不限制；未赋值时不提交该字段")
    @Min(value = 0, message = "executorTimeout 不能小于0")
    private Integer executorTimeout;

    @Schema(description = "失败重试次数，0表示不重试；未赋值时不提交该字段")
    @Min(value = 0, message = "executorFailRetryCount 不能小于0")
    private Integer executorFailRetryCount;

}
