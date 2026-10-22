package com.simple.common.xxljob.manager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import com.simple.common.core.common.entity.HttpRecord;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.utils.BeanUtils;
import com.simple.common.core.utils.HttpRequestUtils;
import com.simple.common.xxljob.common.dto.CreateXxlJobTaskRequest;
import com.simple.common.xxljob.common.dto.UpdateXxlJobTaskRequest;
import com.simple.common.xxljob.common.dto.XxlJobResponse;
import com.simple.common.xxljob.common.enums.XxlJobRequestUrl;
import com.simple.common.xxljob.common.manager.XxlJobManager;
import com.simple.common.xxljob.config.XxlJobConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * Description: xxl-job任务管理器默认实现，基于xxl-job Admin远程接口实现任务的动态管理
 * <p>
 * 装配说明：本类由 {@code XxlJobConfig} 以显式 {@code @Bean} 注册（带 @ConditionalOnMissingBean），
 * 不再使用 {@code @Component}——{@code xxl.job.open} 未开启时整组组件不装配，
 * 且避免宿主工程误扫本包时在配置缺失状态下拉起半套组件。
 * <p>
 * 鉴权分两层：
 * 一、accessToken层（{@code xxl.job.access-token}）：官方Admin仅对 /api/* 做校验，对 /jobinfo/* 无效；
 * 对自定义部署开启token拦截的场景同样生效（登录请求同样携带token头），Admin未配置accessToken时不携带请求头。
 * 二、登录态层（{@code xxl.job.admin-username}/{@code xxl.job.admin-password}）：官方Admin的 /jobinfo/*
 * 由 PermissionInterceptor 做登录cookie校验，配置账号密码后先登录Admin获取
 * {@link #LOGIN_COOKIE_NAME} cookie，后续请求全部携带；缺省空=不启用登录态。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class DefaultXxlJobManager implements XxlJobManager {

    /**
     * Admin accessToken鉴权请求头名称
     */
    private static final String ACCESS_TOKEN_HEADER = "XXL-JOB-ACCESS-TOKEN";

    /**
     * 登录态cookie请求头名称
     */
    private static final String COOKIE_HEADER = "Cookie";

    /**
     * Admin登录态cookie名称，官方LoginService固定写入该名称
     */
    private static final String LOGIN_COOKIE_NAME = "XXL_JOB_LOGIN_IDENTITY";

    /**
     * Admin登录接口路径，官方IndexController固定映射
     */
    private static final String LOGIN_URL = "/login";

    /**
     * 登录表单账号参数名，官方IndexController.loginDo固定绑定
     */
    private static final String LOGIN_PARAM_USERNAME = "userName";

    /**
     * 登录表单密码参数名，官方IndexController.loginDo固定绑定
     */
    private static final String LOGIN_PARAM_PASSWORD = "password";

    /**
     * Admin返回体中表示成功的业务码
     */
    private static final int SUCCESS_CODE = 200;

    /**
     * Admin登录态失效时PermissionInterceptor返回的重定向状态码
     */
    private static final int NOT_LOGIN_STATUS = 302;

    /**
     * 秒转毫秒换算基数
     */
    private static final int SECONDS_TO_MILLIS = 1000;

    /**
     * Admin 配置（构造注入，同时服务 @Bean 装配与测试构造）
     */
    private final XxlJobConfig xxlJobConfig;

    /**
     * 缓存的Admin登录态cookie值，volatile保证多请求线程可见
     */
    private volatile String loginCookie;

    public DefaultXxlJobManager(XxlJobConfig xxlJobConfig) {
        this.xxlJobConfig = xxlJobConfig;
    }

    @Override
    public Integer create(CreateXxlJobTaskRequest request) {
        // 向Admin发起创建任务请求并统一校验返回结果
        XxlJobResponse response = postForForm(XxlJobRequestUrl.ADD, buildJobForm(request));

        // Admin生成的任务ID为整数主键，与 start/end/delete/trigger 的 Integer 口径对齐；
        // 缺失任务ID属于Admin侧异常返回，立即失败而不是把 null 透传给调用方
        return parseJobId(response);
    }

    @Override
    public void update(UpdateXxlJobTaskRequest request) {
        Objects.requireNonNull(request, "request 不能为 null");
        // 向Admin发起修改任务请求并统一校验返回结果，请求字段与Admin表单绑定字段同名
        postForForm(XxlJobRequestUrl.UPDATE, buildJobForm(request));
    }

    @Override
    public void delete(Integer id) {
        postForForm(XxlJobRequestUrl.DELETE, buildIdParams(id));
    }

    @Override
    public void start(Integer id) {
        postForForm(XxlJobRequestUrl.START, buildIdParams(id));
    }

    @Override
    public void end(Integer id) {
        postForForm(XxlJobRequestUrl.END, buildIdParams(id));
    }

    @Override
    public void trigger(Integer id) {
        // 向Admin发起立即执行任务请求并统一校验返回结果，executorParam与addressList为Admin侧可选参数
        postForForm(XxlJobRequestUrl.TRIGGER, buildIdParams(id));
    }

    /**
     * 发起Admin的表单请求并统一校验返回结果
     * <p>
     * 官方Admin的 /jobinfo/* 均为表单/查询参数绑定，统一走表单口径；
     * 登录态失效时重新登录一次后重试，避免Admin侧cookie过期导致操作中断。
     * </p>
     *
     * @param requestUrl Admin远程接口路径
     * @param form       表单参数
     * @return Admin统一返回对象
     */
    private XxlJobResponse postForForm(XxlJobRequestUrl requestUrl, Map<String, Object> form) {
        // 先保证登录态就绪再发起请求
        ensureLogin();

        // 发起表单请求并解析校验返回体
        HttpRecord record = doExecuteForm(requestUrl, form);

        // Admin登录态失效时重新登录一次后重试
        if (isNotLoginResponse(record)) {
            closeQuietly(record);
            refreshLogin();
            record = doExecuteForm(requestUrl, form);
        }
        return parseResponse(record);
    }

    /**
     * 发起Admin登录态层的表单请求
     *
     * @param requestUrl Admin远程接口路径
     * @param form       表单参数
     * @return HTTP请求记录
     */
    private HttpRecord doExecuteForm(XxlJobRequestUrl requestUrl, Map<String, Object> form) {
        // 发起表单请求，超时时间由配置的秒数换算为毫秒
        return HttpRequestUtils.postForm(requestUrl.url(xxlJobConfig), buildAuthHeaders(), form,
                                         xxlJobConfig.getRequestTimeout() * SECONDS_TO_MILLIS);
    }

    /**
     * 判断响应是否为Admin登录态失效
     * <p>
     * 官方Admin登录态失效时 PermissionInterceptor 返回 302 重定向；但底层 HTTP 客户端
     * 默认自动跟随重定向，实际拿到的是 200 + 登录页 HTML——仅凭 302 判定会恒为 false，
     * 重登分支沦为死代码，后续按 JSON 反序列化 HTML 抛出误导性的解析异常。
     * 因此除 302 外，按「响应体非 JSON（HTML 登录页）」双口径识别登录态失效。
     * </p>
     *
     * @param record HTTP请求记录
     * @return 登录态启用且响应为未登录重定向/登录页时返回true
     */
    private boolean isNotLoginResponse(HttpRecord record) {
        if (!isLoginEnabled()) {
            return false;
        }
        HttpResponse execute = record.getExecute();
        if (execute.getStatus() == NOT_LOGIN_STATUS) {
            return true;
        }
        // 未跟随重定向时状态码可能仍为 200：Admin 登录页为 HTML（<!DOCTYPE html> 开头），
        // 正常业务返回体恒为 JSON（'{' 开头），以响应体首字符识别登录页
        String body = execute.body();
        return body != null && body.trim().startsWith("<");
    }

    /**
     * 安静关闭已判定的失效响应，释放底层连接后再重试
     */
    private void closeQuietly(HttpRecord record) {
        try {
            HttpResponse execute = record.getExecute();
            if (execute != null) {
                execute.close();
            }
        } catch (Exception e) {
            log.debug("关闭 xxl-job Admin 失效响应失败", e);
        }
    }

    /**
     * 构建任务表单参数，字段名与Admin侧XxlJobInfo属性名一致，空值字段不参与绑定
     *
     * @param request 任务请求对象
     * @return 表单参数
     */
    private Map<String, Object> buildJobForm(CreateXxlJobTaskRequest request) {
        // 转换为表单参数并剔除空值，避免Admin侧绑定空串覆盖业务字段；
        // 更新场景未赋值的可选字段（超时/重试次数等）不提交，不会静默重置既有配置
        return BeanUtils.toMap(request);
    }

    /**
     * 构建按任务ID操作的表单参数
     *
     * @param id 任务ID
     * @return 表单参数
     */
    private Map<String, Object> buildIdParams(Integer id) {
        // id 为 null 时快速失败：String.valueOf(null) 会静默产出 "null" 透传给 Admin，
        // 以难定位的下游错误暴露
        Objects.requireNonNull(id, "任务 id 不能为 null");
        Map<String, Object> params = new HashMap<>();
        params.put("id", String.valueOf(id));
        return params;
    }

    /**
     * 保证Admin登录态就绪，未启用登录态或已持有cookie时直接返回
     */
    private void ensureLogin() {
        // 未配置Admin账号密码时不启用登录态
        if (!isLoginEnabled()) {
            return;
        }

        // 已持有登录cookie时直接复用
        if (StrUtil.isNotBlank(loginCookie)) {
            return;
        }

        // 并发下只登录一次，后续请求复用同一cookie
        synchronized (this) {
            if (StrUtil.isNotBlank(loginCookie)) {
                return;
            }
            doLogin();
        }
    }

    /**
     * 重建Admin登录态，用于检测到cookie失效后重登一次
     * <p>
     * 置空旧cookie与重登必须在同一把锁内完成：{@link #buildAuthHeaders()} 无锁读取 loginCookie，
     * 锁外置空会与并发线程「刚刷新成功的新cookie」发生 check-then-act 竞态——
     * 误清有效cookie、或在置空与重登之间发出的请求丢失Cookie头再次返回未登录。
     * </p>
     */
    private void refreshLogin() {
        synchronized (this) {
            // 清除已失效的cookie后重新登录
            loginCookie = null;
            doLogin();
        }
    }

    /**
     * 登录Admin并缓存登录态cookie，登录失败抛出框架统一业务异常并携带Admin返回信息
     */
    private void doLogin() {
        // 构建登录表单参数，参数名与Admin登录接口绑定名一致
        Map<String, Object> form = new HashMap<>();
        form.put(LOGIN_PARAM_USERNAME, xxlJobConfig.getAdminUsername());
        form.put(LOGIN_PARAM_PASSWORD, xxlJobConfig.getAdminPassword());

        // 登录请求必须携带 accessToken 头（仅token，不带可能已失效的登录cookie）：
        // 自定义部署对 /login 全局开启 token 拦截时，空请求头会导致登录必然被拒，
        // 后续所有任务管理操作级联失败
        HttpRecord record = HttpRequestUtils.postForm(xxlJobConfig.getAdminAddresses() + LOGIN_URL, buildTokenHeaders(), form,
                                                      xxlJobConfig.getRequestTimeout() * SECONDS_TO_MILLIS);
        XxlJobResponse response = record.get(XxlJobResponse.class,
                                             body -> new DefaultException(DefaultExceptionEnum.ERROR, "xxl-job Admin登录失败：" + body));

        // 登录业务失败时携带Admin返回信息抛出
        if (response.getCode() == null || response.getCode() != SUCCESS_CODE) {
            throw new DefaultException(DefaultExceptionEnum.ERROR, buildBusinessErrorMessage(response));
        }

        // 从登录响应中提取登录态cookie并缓存
        String cookieValue = record.getExecute().getCookieValue(LOGIN_COOKIE_NAME);
        if (StrUtil.isBlank(cookieValue)) {
            throw new DefaultException(DefaultExceptionEnum.ERROR, "xxl-job Admin登录成功但未返回登录态cookie");
        }
        loginCookie = cookieValue;
    }

    /**
     * 构建登录请求头：仅携带 accessToken（不带可能已失效的登录cookie）
     *
     * @return 登录请求头集合
     */
    private Map<String, String> buildTokenHeaders() {
        Map<String, String> heads = new HashMap<>();
        // Admin开启accessToken鉴权时（含对 /login 的自定义token拦截部署）远程校验依赖该请求头
        if (StrUtil.isNotBlank(xxlJobConfig.getAccessToken())) {
            heads.put(ACCESS_TOKEN_HEADER, xxlJobConfig.getAccessToken());
        }
        return heads;
    }

    /**
     * 构建Admin请求头：accessToken头对 /api/* 与自定义token拦截部署生效；启用登录态时携带登录cookie
     *
     * @return 请求头集合
     */
    private Map<String, String> buildAuthHeaders() {
        Map<String, String> heads = buildTokenHeaders();

        // 启用登录态后所有Admin请求携带登录cookie
        if (StrUtil.isNotBlank(loginCookie)) {
            heads.put(COOKIE_HEADER, LOGIN_COOKIE_NAME + "=" + loginCookie);
        }
        return heads;
    }

    /**
     * 判断是否启用登录态鉴权，账号密码均配置时启用
     *
     * @return 启用登录态时返回true
     */
    private boolean isLoginEnabled() {
        return StrUtil.isNotBlank(xxlJobConfig.getAdminUsername())
               && StrUtil.isNotBlank(xxlJobConfig.getAdminPassword());
    }

    /**
     * 解析Admin返回体并统一判错，HTTP层失败与业务码非成功均抛出框架统一业务异常
     *
     * @param record HTTP请求记录
     * @return Admin统一返回对象
     */
    private XxlJobResponse parseResponse(HttpRecord record) {
        // 校验HTTP层结果并解析Admin返回体
        XxlJobResponse response = record.get(XxlJobResponse.class,
                                             body -> new DefaultException(DefaultExceptionEnum.ERROR, "xxl-job Admin请求失败：" + body));

        // 校验Admin业务层结果，HTTP成功但业务码非200同样视为操作失败
        if (response.getCode() == null || response.getCode() != SUCCESS_CODE) {
            throw new DefaultException(DefaultExceptionEnum.ERROR, buildBusinessErrorMessage(response));
        }
        return response;
    }

    /**
     * 从Admin创建任务返回中解析任务ID
     *
     * @param response Admin统一返回对象
     * @return 任务ID
     */
    private Integer parseJobId(XxlJobResponse response) {
        String content = response.getContent();
        if (StrUtil.isBlank(content)) {
            throw new DefaultException(DefaultExceptionEnum.ERROR,
                    "xxl-job Admin 创建任务成功但未返回任务ID，content: " + content);
        }
        try {
            return Integer.valueOf(content.trim());
        } catch (NumberFormatException e) {
            throw new DefaultException(DefaultExceptionEnum.ERROR,
                    "xxl-job Admin 返回的任务ID不是合法整数: " + content);
        }
    }

    /**
     * 构建Admin业务失败的异常信息，包含Admin返回的完整内容
     *
     * @param response Admin统一返回对象
     * @return 异常信息
     */
    private String buildBusinessErrorMessage(XxlJobResponse response) {
        return "xxl-job Admin业务处理失败：code=" + response.getCode() + "，msg=" + response.getMsg() + "，content=" + response.getContent();
    }

}
