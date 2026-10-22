package com.simple.common.auth.client.util;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * Description: 登录用户信息工具类
 * <p>
 * ThreadLocal 生命周期契约：登录上下文必须随请求结束清理（{@code remove()}），
 * 框架在 {@code AuthHandlerInterceptor#afterCompletion} 与鉴权责任链异常分支均会兜底清理；
 * 自建线程/异步任务的调用方必须自行 remove，否则线程池复用会向前续请求泄漏身份。
 * 未登录调用 {@link #getUserTemporary()} 返回空对象（userId 为 null）：
 * 判定登录态请使用 {@code notEmpty(getUserTemporary().getUserId())}，不要把空对象当作有效登录态。
 * </p>
 *
 * @author qty
 */
@Slf4j
public final class LoginUserUtils {

    private static final ThreadLocal<UserTemporary> LOGIN_INFO = new ThreadLocal<>();

    private LoginUserUtils() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 添加登录用户信息
     *
     * @param info 用户信息（不允许为 null：错误地传入 null 会覆盖真实登录态，请使用 {@link #remove()} 清除）
     */
    public static void add(UserTemporary info) {
        Objects.requireNonNull(info, "用户信息不能为null，请使用remove()清除登录上下文");
        LOGIN_INFO.set(info);
    }

    /**
     * 获取登录用户信息
     *
     * @return 当前登录用户；未登录返回空对象（userId 为 null），不返回 null
     */
    public static UserTemporary getUserTemporary() {
        UserTemporary userTemporary = LOGIN_INFO.get();
        if (ObjUtil.isNull(userTemporary)) {
            // debug 级：匿名/白名单请求未登录属正常态，error 级会造成日志噪音
            log.debug("没有登录，LoginUserUtils返回空的数据对象！");
            userTemporary = new UserTemporary();
        }
        return userTemporary;
    }

    /**
     * 删除登录用户信息（请求结束必须调用，防止线程池身份泄漏）
     */
    public static void remove() {
        LOGIN_INFO.remove();
    }

}
