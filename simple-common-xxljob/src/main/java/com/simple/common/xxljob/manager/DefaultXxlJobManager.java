package com.simple.common.xxljob.manager;

import cn.hutool.core.util.StrUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: xxl-job任务管理器默认实现，基于xxl-job Admin远程接口实现任务的动态管理
 * <p>
 * 鉴权分两层：
 * 一、accessToken层（{@code xxl.job.access-token}）：官方Admin仅对 /api/* 做校验，对 /jobinfo/* 无效；
 * 对自定义部署开启token拦截的场景同样生效，Admin未配置accessToken时不携带请求头。
 * 二、登录态层（{@code xxl.job.admin-username}/{@code xxl.job.admin-password}）：官方Admin的 /jobinfo/*
 * 由 PermissionInterceptor 做登录cookie校验，配置账号密码后先登录Admin获取
 * {@link #LOGIN_COOKIE_NAME} cookie，后续请求全部携带；缺省空=不启用登录态。
 * </p>
 *
 * @author qty
 */
@Component
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

    @Autowired
    private XxlJobConfig xxlJobConfig;

    /**
     * 缓存的Admin登录态cookie值，volatile保证多请求线程可见
     */
    private volatile String loginCookie;

    @Override
    public String create(CreateXxlJobTaskRequest request) {
        // 向Admin发起创建任务请求并统一校验返回结果
        XxlJobResponse response = postForForm(XxlJobRequestUrl.ADD, buildJobForm(request));

        // 返回Admin生成的任务ID
        return response.getContent();
    }

    @Override
    public void update(UpdateXxlJobTaskRequest request) {
        // 向Admin发起修改任务请求并统一校验返回结果，请求字段与Admin表单绑定字段同名
        postForForm(XxlJobRequestUrl.UPDATE, buildJobForm(request));
    }

    @Override
    public void delete(Integer id) {
        // 向Admin发起删除任务请求并统一校验返回结果
        postForForm(XxlJobRequestUrl.DELETE, buildIdParams(id));
    }

    @Override
    public void start(Integer id) {
        // 向Admin发起启动任务请求并统一校验返回结果
        postForForm(XxlJobRequestUrl.START, buildIdParams(id));
    }

    @Override
    public void end(Integer id) {
        // 向Admin发起停止任务请求并统一校验返回结果
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

        // Admin登录态失效返回302重定向，重新登录一次后重试
        if (isNotLoginResponse(record)) {
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
        return HttpRequestUtils.post(requestUrl.url(xxlJobConfig), buildAuthHeaders(), form,
                                     xxlJobConfig.getRequestTimeout() * SECONDS_TO_MILLIS);
    }

    /**
     * 判断响应是否为Admin登录态失效
     *
     * @param record HTTP请求记录
     * @return 登录态启用且响应为未登录重定向时返回true
     */
    private boolean isNotLoginResponse(HttpRecord record) {
        return isLoginEnabled() && record.getExecute().getStatus() == NOT_LOGIN_STATUS;
    }

    /**
     * 构建任务表单参数，字段名与Admin侧XxlJobInfo属性名一致，空值字段不参与绑定
     *
     * @param request 任务请求对象
     * @return 表单参数
     */
    private Map<String, Object> buildJobForm(CreateXxlJobTaskRequest request) {
        // 转换为表单参数并剔除空值，避免Admin侧绑定空串覆盖业务字段
        return BeanUtils.toMap(request);
    }

    /**
     * 构建按任务ID操作的表单参数
     *
     * @param id 任务ID
     * @return 表单参数
     */
    private Map<String, Object> buildIdParams(Integer id) {
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
     */
    private void refreshLogin() {
        // 清除已失效的cookie后再重新登录
        loginCookie = null;
        ensureLogin();
    }

    /**
     * 登录Admin并缓存登录态cookie，登录失败抛出框架统一业务异常并携带Admin返回信息
     */
    private void doLogin() {
        // 构建登录表单参数，参数名与Admin登录接口绑定名一致
        Map<String, Object> form = new HashMap<>();
        form.put(LOGIN_PARAM_USERNAME, xxlJobConfig.getAdminUsername());
        form.put(LOGIN_PARAM_PASSWORD, xxlJobConfig.getAdminPassword());

        // 发起登录请求并解析返回体
        HttpRecord record = HttpRequestUtils.post(xxlJobConfig.getAdminAddresses() + LOGIN_URL, new HashMap<>(), form,
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
     * 构建Admin请求头：accessToken头对 /api/* 与自定义token拦截部署生效；启用登录态时携带登录cookie
     *
     * @return 请求头集合
     */
    private Map<String, String> buildAuthHeaders() {
        Map<String, String> heads = new HashMap<>();

        // Admin开启accessToken鉴权时远程校验依赖该请求头
        if (StrUtil.isNotBlank(xxlJobConfig.getAccessToken())) {
            heads.put(ACCESS_TOKEN_HEADER, xxlJobConfig.getAccessToken());
        }

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
     * 构建Admin业务失败的异常信息，包含Admin返回的完整内容
     *
     * @param response Admin统一返回对象
     * @return 异常信息
     */
    private String buildBusinessErrorMessage(XxlJobResponse response) {
        return "xxl-job Admin业务处理失败：code=" + response.getCode() + "，msg=" + response.getMsg() + "，content=" + response.getContent();
    }

}
