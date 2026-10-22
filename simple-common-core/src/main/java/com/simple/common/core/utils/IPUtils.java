package com.simple.common.core.utils;

import cn.hutool.core.util.StrUtil;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import jakarta.servlet.http.HttpServletRequest;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by IntelliJ IDEA
 * <p>
 * ip地址获取工具类
 * <p>安全说明：{@code X-Forwarded-For} 等转发头完全由客户端可注入，仅当直连地址
 * {@code request.getRemoteAddr()} 属于内网（即部署了可信反向代理，如同机/同VPC Nginx）时才采信转发头，
 * 且取转发链中最后一个非unknown段（最接近本层代理的一跳，攻击者无法伪造）；
 * 直连地址为公网时一律以 remoteAddr 为准。以此保证限流/审计的IP不被伪造绕过或污染。</p>
 *
 * @author qty
 */
public class IPUtils {

    /**
     * 未知IP标识
     */
    public static final String UNKNOWN = "unknown";

    private IPUtils() {
    }

    /**
     * 获取客户端IP
     *
     * @return IP地址，当前线程未绑定请求上下文时返回unknown
     */
    public static String getIpAddr() {
        try {
            HttpServletRequest request = HttpServletUtils.getRequest();
            return getIpAddr(request);
        } catch (IllegalStateException e) {
            // 非Web请求线程（异步/定时任务）无请求上下文，返回unknown而非裸NPE
            return UNKNOWN;
        }
    }

    /**
     * 获取客户端IP
     *
     * @param request 当前HTTP请求对象
     * @return IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String remoteAddr = request.getRemoteAddr();
        // 仅当直连方为内网地址（可信代理，如同机/同VPC Nginx）时才采信客户端可注入的转发头
        if (!isPrivateAddress(remoteAddr)) {
            return normalizeLoopback(remoteAddr);
        }

        String ip = firstTrustedForwardedIp(request);
        if (ip == null) {
            ip = remoteAddr;
        }
        return normalizeLoopback(ip);
    }

    /**
     * 按可信顺序取转发头中最后一个非unknown的IP段（最后一段距本层代理最近，最不可伪造）
     */
    private static String firstTrustedForwardedIp(HttpServletRequest request) {
        String[] headers = {"x-forwarded-for", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StrUtil.isBlank(value) || UNKNOWN.equalsIgnoreCase(value)) {
                continue;
            }
            // 从后向前取最后一个非unknown段
            String[] segments = value.split(",");
            for (int i = segments.length - 1; i >= 0; i--) {
                String candidate = segments[i].trim();
                if (!isUnknown(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * IPv4回环与IPv6回环统一归一为127.0.0.1
     */
    private static String normalizeLoopback(String ip) {
        return ip == null ? UNKNOWN : ("0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip);
    }

    /**
     * 是否内网地址（RFC1918站点本地/回环/链路本地）——仅这类直连方才被当作可信代理
     */
    private static boolean isPrivateAddress(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(normalizeLoopback(ip));
            return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
        } catch (Exception e) {
            // 解析失败按公网处理（不采信转发头）
            return false;
        }
    }

    /**
     * 获取本机内网IP
     *
     * @return 本机内网IPv4地址
     */
    public static String getIntranetIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // 跳过禁用与回环的网络接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    // 仅接受IPv4且为RFC1918站点本地地址，排除Docker网桥外的公网网卡/VPN utun被误选
                    if (inetAddress instanceof Inet4Address && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            throw new DefaultException(DefaultExceptionEnum.ERROR, "获取内网IP失败: " + e.getMessage());
        }
        throw new DefaultException(DefaultExceptionEnum.ERROR, "未获取到内网IP");
    }

    /**
     * 获取本机地址（localhost解析结果，非公网IP）
     *
     * @return 本机IP地址
     */
    public static String getLocalHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            throw new DefaultException(DefaultExceptionEnum.ERROR, "获取本机IP失败: " + e.getMessage());
        }
    }

    /**
     * 获取本机公网IP
     *
     * @return 本机IP地址
     * @deprecated 该方法实际返回本机地址（localhost解析结果）而非公网IP，请使用 {@link #getLocalHostIp()}
     */
    @Deprecated
    public static String getPublicNetworkIp() {
        return getLocalHostIp();
    }

    /**
     * 检测给定字符串是否为未知，多用于检测HTTP请求相关
     *
     * @param checkString 被检测的字符串
     * @return 是否未知
     */
    private static boolean isUnknown(String checkString) {
        return StrUtil.isBlank(checkString) || UNKNOWN.equalsIgnoreCase(checkString);
    }

}
