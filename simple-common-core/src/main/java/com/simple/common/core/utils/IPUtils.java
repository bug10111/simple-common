package com.simple.common.core.utils;

import cn.hutool.core.util.StrUtil;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by IntelliJ IDEA
 * <p>
 * ip地址获取工具类
 *
 * @author qty
 */
public class IPUtils {

    /**
     * 获取客户端IP
     *
     * @return IP地址
     */
    public static String getIpAddr() {
        HttpServletRequest request = HttpServletUtils.getRequest();
        return getIpAddr(request);
    }

    /**
     * 获取客户端IP
     *
     * @return IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : getMultistageReverseProxyIp(ip);
    }

    /**
     * 获取本机内网IP
     */
    @SneakyThrows
    public static String getIntranetIp() {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            // 跳过禁用的网络接口
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress inetAddress = addresses.nextElement();
                // 找到IPv4类型的地址
                if (inetAddress.getAddress().length == 4) {
                    return inetAddress.getHostAddress();
                }
            }
        }
        throw new DefaultException(DefaultExceptionEnum.ERROR, "未获取到内网IP");
    }

    /**
     * 获取本机公网IP
     */
    @SneakyThrows
    public static String getPublicNetworkIp() {
        return InetAddress.getLocalHost().getHostAddress();
    }

    /**
     * 从多级反向代理中获得第一个非unknown IP地址
     *
     * @param ip 获得的IP地址
     * @return 第一个非unknown IP地址
     */
    private static String getMultistageReverseProxyIp(String ip) {
        // 多级反向代理检测
        if (ip != null && ip.indexOf(",") > 0) {
            final String[] ips = ip.trim().split(",");
            for (String subIp : ips) {
                if (!isUnknown(subIp)) {
                    ip = subIp;
                    break;
                }
            }
        }
        return ip;
    }

    /**
     * 检测给定字符串是否为未知，多用于检测HTTP请求相关
     *
     * @param checkString 被检测的字符串
     * @return 是否未知
     */
    private static boolean isUnknown(String checkString) {
        return StrUtil.isBlank(checkString) || "unknown".equalsIgnoreCase(checkString);
    }

}
