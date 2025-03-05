package com.simple.common.logs.client.manager;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.ContentType;
import com.simple.common.core.common.constant.CoreConstant;
import com.simple.common.core.utils.IPUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.common.logs.client.common.constant.LogConstant;
import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.client.common.httpservletrequest.CachedBodyHttpServletRequest;
import com.simple.common.logs.client.common.manager.LogManager;
import com.simple.common.logs.client.common.manager.LogUserManager;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class DefaultLogManager implements LogManager {

    @Autowired
    private EventBusService eventBusService;

    @Autowired
    private LogUserManager logUserManager;

    @Override
    @SneakyThrows
    public void create(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        //构建请求对象
        LogDataEvent logDataEvent = new LogDataEvent();
        Long startTime = (Long) request.getAttribute(LogConstant.START_TIME);
        long duration = System.currentTimeMillis() - startTime;

        //获取请求参数
        logDataEvent.setOperParam(getAllParameters(request));

        logDataEvent.setMethod(request.getMethod());
        logDataEvent.setOperUrl(request.getRequestURI());
        logDataEvent.setOperIp(IPUtils.getIpAddr(request));

        String userId = logUserManager.loginUserId();
        if (ObjUtil.isEmpty(userId)) {
            logDataEvent.setUserId("-");
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
        }

        logDataEvent.setRequestTime(duration);
        logDataEvent.setCreateTime(DateTime.now());
        eventBusService.push(logDataEvent);
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
     */
    @SneakyThrows
    protected String getAllParameters(HttpServletRequest request) {

        String json = "";
        String contentType = request.getContentType();

        if (contentType != null && contentType.contains(ContentType.JSON.getValue())) {
            if (request instanceof CachedBodyHttpServletRequest request1) {
                json = new String(request1.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } else {
            Map<String, String[]> parameterMap = request.getParameterMap();
            json = JsonUtils.toJsonStr(parameterMap);
        }
        return json;
    }

}
