package com.simple.common.logs.client.service;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import com.simple.common.core.common.constant.CoreConstant;
import com.simple.common.core.utils.IPUtils;
import com.simple.common.logs.client.common.constant.LogConstant;
import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.client.common.httpservletrequest.CachedBodyHttpServletRequest;
import com.simple.common.logs.client.common.manager.LogManager;
import com.simple.common.logs.client.common.manager.LogUserManager;
import com.simple.common.logs.client.common.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultLogService implements LogService {

    @Autowired
    private LogUserManager logUserManager;

    @Autowired
    private LogManager logManager;

    @Override
    @SneakyThrows
    public void send(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        //构建请求对象
        LogDataEvent logDataEvent = new LogDataEvent();
        Long startTime = (Long) request.getAttribute(LogConstant.START_TIME);
        long duration = System.currentTimeMillis() - startTime;

        // 设置 TraceId（从请求属性中获取，由拦截器/过滤器设置）
        Object traceId = request.getAttribute(LogConstant.TRACE_ID_HEADER);
        if (traceId != null) {
            logDataEvent.setTraceId(traceId.toString());
        }

        //获取请求参数
        logDataEvent.setOperParam(getAllParameters(request));

        logDataEvent.setMethod(request.getMethod());
        logDataEvent.setOperUrl(request.getRequestURI());
        logDataEvent.setOperIp(IPUtils.getIpAddr(request));

        String userId = logUserManager.loginUserId();
        if (ObjUtil.isEmpty(userId)) {
            logDataEvent.setUserId(null);
        } else {
            logDataEvent.setUserId(userId);
        }

        String nickName = logUserManager.loginNickName();
        if (ObjUtil.isEmpty(nickName)) {
            logDataEvent.setNickname("-");
        } else {
            logDataEvent.setNickname(nickName);
        }

        //获取接口
        if (handler instanceof HandlerMethod handlerMethod) {

            // 获取接口上的注解
            Operation operation = handlerMethod.getMethod().getAnnotation(Operation.class);

            if (operation != null) {
                logDataEvent.setTitle(operation.summary());
            } else {
                logDataEvent.setTitle("-");
            }
        }

        //请求成功
        if (response.getStatus() == HttpServletResponse.SC_OK) {
            logDataEvent.setStatus(HttpServletResponse.SC_OK);
            logDataEvent.setErrorMsg("请求成功");
        } else {
            logDataEvent.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Object attribute = request.getAttribute(CoreConstant.EXCEPTION);
            if (attribute instanceof Exception exception) {
                logDataEvent.setErrorMsg(exception.getMessage());

                String stackTrace = getStackTraceAsString(exception);
                logDataEvent.setErrorData(stackTrace);
            } else {
                logDataEvent.setErrorMsg("未收集到有效异常信息");
            }
            request.removeAttribute(CoreConstant.EXCEPTION);
        }

        logDataEvent.setRequestTime(duration);
        logDataEvent.setCreateTime(DateTime.now());
        logManager.send(logDataEvent);
    }

    @Override
    public void start() {
        logManager.start();
    }

    @Override
    public void stop() {
        logManager.stop();
    }

    /**
     * 辅助方法：将异常堆栈转换为字符串
     */
    protected String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * 获取请求参数
     * <p>
     * 支持多种Content-Type的参数采集：
     * - application/json: 从请求体读取
     * - application/xml: 从请求体读取
     * - text/xml: 从请求体读取
     * - text/plain: 从请求体读取
     * - application/x-www-form-urlencoded: 从请求体读取或从ParameterMap读取
     * - multipart/form-data: 从ParameterMap读取（不包含文件内容）
     * - 其他类型: 从ParameterMap读取
     * </p>
     */
    @SneakyThrows
    protected String getAllParameters(HttpServletRequest request) {
        String contentType = request.getContentType();

        // 处理CachedBodyHttpServletRequest包装的请求
        if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
            // 如果请求体已被缓存，直接获取缓存内容
            if (cachedRequest.isBodyCached()) {
                String cachedBody = cachedRequest.getCachedBody();
                if (ObjUtil.isNotEmpty(cachedBody)) {
                    return cachedBody;
                }
            }

            // 对于multipart/form-data或未缓存的情况，尝试从ParameterMap获取
            if (ObjUtil.isNotEmpty(contentType) && contentType.toLowerCase().contains("multipart/form-data")) {
                return getParametersFromMap(request);
            }

            // 如果请求体未缓存且ParameterMap有数据，返回ParameterMap
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (ObjUtil.isNotEmpty(parameterMap)) {
                return getParametersFromMap(request);
            }

            return "";
        }

        // 处理普通HttpServletRequest请求
        // JSON类型请求
        if (ObjUtil.isNotEmpty(contentType) && contentType.toLowerCase().contains(ContentType.JSON.getValue())) {
            log.warn("请求未经过CachedBodyHttpServletRequest包装，无法读取JSON请求体，请检查LogFilter配置");
            return "";
        }

        // 其他类型从ParameterMap获取
        return getParametersFromMap(request);
    }

    /**
     * 从ParameterMap获取参数并转换为JSON字符串
     *
     * @param request HttpServletRequest请求对象
     * @return JSON格式的参数字符串
     */
    private String getParametersFromMap(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (ObjUtil.isEmpty(parameterMap)) {
            return "";
        }
        return JSONUtil.toJsonStr(parameterMap);
    }

}