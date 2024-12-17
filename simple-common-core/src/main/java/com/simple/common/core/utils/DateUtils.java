package com.simple.common.core.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with IntelliJ IDEA
 * Description: 时间操作工具类
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class DateUtils extends DateUtil {

    /**
     * 获取网络北京时间
     */
    @SneakyThrows
    public static String getNetworkDate() {
        return new DateTime().toString();
    }

    //    public static String getNetworkDate() {
    //
    //        // 使用多个NTP服务器地址
    //        String[] ntpServers = { "ntp.aliyun.com", "ntp2.aliyun.com", "ntp3.aliyun.com", "ntp4.aliyun.com", "ntp5.aliyun.com", "ntp6.aliyun.com",
    //                                "ntp7.aliyun.com", "ntp1.aliyun.com", "ntp.tencent.com", "time1.cloud.tencent.com" };
    //
    //        Instant networkTime = null;
    //
    //        for (String server : ntpServers) {
    //
    //            try (NTPUDPClient client = new NTPUDPClient()) {
    //
    //                // 设置超时时间
    //                client.setDefaultTimeout(300);
    //
    //                // 获取时间信息
    //                TimeInfo timeInfo = client.getTime(InetAddress.getByName(server));
    //
    //                // 计算偏移量
    //                timeInfo.computeDetails();
    //                networkTime = timeInfo.getMessage().getTransmitTimeStamp().getDate().toInstant();
    //                break;
    //
    //            } catch (Exception e) {
    //                log.error("无法从 {} 获取网络时间: {}", server, e.getMessage());
    //            }
    //        }
    //
    //        if (networkTime == null) {
    //            networkTime = new Date().toInstant();
    //        }
    //
    //        // 转换为北京时间
    //        ZonedDateTime beijingTime = networkTime.atZone(ZoneId.of("Asia/Shanghai"));
    //
    //        // 格式化输出
    //        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    //        return beijingTime.format(formatter);
    //    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(getNetworkDate());
        }
    }
}