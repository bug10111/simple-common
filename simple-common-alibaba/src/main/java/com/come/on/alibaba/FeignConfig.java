package com.come.on.alibaba;

import com.simple.common.auth.client.common.constant.TokenConstant;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Created with IntelliJ IDEA
 * <p>
 * Feign 调用鉴权头透传拦截器：将当前请求的登录态（Authorization）、用户上下文
 * 与签名头复制到出站 Feign 请求，服务于「应用内服务间调用需要携带调用者身份」的场景。
 * <p>
 * <b>作用域警告（重要）</b>：经 AutoConfiguration.imports 注册后，本拦截器对应用内
 * <b>所有</b> Feign 客户端全局生效——包括指向外部/第三方系统的客户端。将终端用户凭证与
 * 身份上下文发往外部系统属于敏感信息外泄，指向外部服务的客户端必须自行规避：
 * </p>
 * <ul>
 *   <li>设置 {@code simple.alibaba.feign.propagate-auth-headers=false} 全局关闭后按客户端粒度装配；或</li>
 *   <li>为外部客户端声明独立配置类（{@code @FeignClient(configuration = XxxConfig.class)} 的
 *       per-client 配置不会被本全局拦截器覆盖语义所绑架，可按需清除请求头）。</li>
 * </ul>
 *
 * @author qty
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "simple.alibaba.feign", name = "propagate-auth-headers",
        havingValue = "true", matchIfMissing = true)
public class FeignConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        //非 Servlet 环境（异步/定时/测试）的 RequestAttributes 不保证是 ServletRequestAttributes：
        //硬转换会抛 ClassCastException 中断 Feign 调用，这里按「无请求上下文」降级跳过透传
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes requestAttributes)) {
            log.debug("当前无 Servlet 请求上下文，跳过 Feign 鉴权头透传");
            return;
        }

        //传递token
        String token = requestAttributes.getRequest().getHeader(TokenConstant.Authorization);
        if (token != null) {
            requestTemplate.header(TokenConstant.Authorization, token);
        }

        //传递序列化数据
        String encoded = requestAttributes.getRequest().getHeader(TokenConstant.userHead);
        if (encoded != null) {
            requestTemplate.header(TokenConstant.userHead, encoded);
        }

        //传递签名
        String sign = requestAttributes.getRequest().getHeader(TokenConstant.userSignHead);
        if (sign != null) {
            requestTemplate.header(TokenConstant.userSignHead, sign);
        }
    }
}
