package com.simple.common.logs.client.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import com.simple.common.core.common.constant.CoreConstant;
import com.simple.common.core.utils.IPUtils;
import com.simple.common.logs.client.common.constant.LogConstant;
import com.simple.common.logs.client.common.httpservletrequest.CachedBodyHttpServletRequest;
import com.simple.common.logs.client.manager.BufferedLogManager;
import com.simple.common.logs.client.common.manager.LogUserManager;
import com.simple.common.logs.client.common.service.LogService;
import com.simple.common.logs.proto.common.event.LogDataEvent;
import com.simple.common.logs.proto.common.time.TimeStampProvider;
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
 * 默认日志服务实现
 * <p>
 * 负责从 HTTP 请求中提取日志信息，并通过 BufferedLogManager 异步发送。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultLogService implements LogService {

    @Autowired
    private LogUserManager logUserManager;

    @Autowired
    private BufferedLogManager bufferedLogManager;

    @Autowired
    private TimeStampProvider timeStampProvider;

    @Override
    public void send(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 从对象池获取 LogDataEvent 实例（避免频繁创建对象）
        LogDataEvent logDataEvent = LogDataEvent.acquire();

        try {
            Long startTime = (Long) request.getAttribute(LogConstant.START_TIME);
            long duration = System.currentTimeMillis() - startTime;

            // 设置 TraceId（从请求属性中获取，由拦截器/过滤器设置）
            Object traceId = request.getAttribute(LogConstant.TRACE_ID_HEADER);
            if (traceId != null) {
                logDataEvent.setTraceId(traceId.toString());
            }

            // 获取请求参数
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

            // 获取接口注解信息
            if (handler instanceof HandlerMethod handlerMethod) {
                Operation operation = handlerMethod.getMethod().getAnnotation(Operation.class);
                if (operation != null) {
                    logDataEvent.setTitle(operation.summary());
                } else {
                    logDataEvent.setTitle("-");
                }
            }

            // 请求状态处理
            if (response.getStatus() == HttpServletResponse.SC_OK) {
                logDataEvent.setStatus(HttpServletResponse.SC_OK);
                logDataEvent.setErrorMsg("请求成功");
            } else {
                logDataEvent.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                // 优先使用传入的异常参数 ex，若为 null 再从 request 属性中获取
                Exception exceptionToLog = ex;
                if (exceptionToLog == null) {
                    Object attribute = request.getAttribute(CoreConstant.EXCEPTION);
                    if (attribute instanceof Exception) {
                        exceptionToLog = (Exception) attribute;
                    }
                }
                if (exceptionToLog != null) {
                    logDataEvent.setErrorMsg(exceptionToLog.getMessage());
                    logDataEvent.setErrorData(getStackTraceAsString(exceptionToLog));
                } else {
                    logDataEvent.setErrorMsg("未收集到有效异常信息");
                }
                request.removeAttribute(CoreConstant.EXCEPTION);
            }

            logDataEvent.setRequestTime(duration);
            // 使用缓存的时间戳（秒级精度），避免创建 DateTime 对象
            logDataEvent.setCreateTimestamp(timeStampProvider.getCurrentTimestamp());

            // 发送日志（异步入队），BufferedLogManager 内部已处理对象回收
            bufferedLogManager.send(logDataEvent);
        } catch (Exception e) {
            log.error("构建日志事件失败", e);
            // 异常情况下必须回收对象，避免线程内对象污染
            logDataEvent.recycle();
        }
    }

    @Override
    public void start() {
        bufferedLogManager.start();
    }

    @Override
    public void stop() {
        bufferedLogManager.stop();
    }

    /**
     * 辅助方法：将异常堆栈转换为字符串
     *
     * @param throwable 异常对象
     * @return 堆栈字符串
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
     *
     * @param request HttpServletRequest
     * @return 参数字符串
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