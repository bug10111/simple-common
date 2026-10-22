package com.simple.common.exchange;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * Exchange 模式配置。读取 game.exchange.mode，暴露 ExchangeModeHolder Bean。
 * <p>
 * 解析在 Bean 创建期一次性完成：非法配置启动即快速失败，而非拖到首次模式判定时
 * 才以大小写敏感、无诊断信息的 valueOf 异常暴露。
 */
@Configuration
public class ExchangeConfig {

    @Value("${game.exchange.mode:local}")
    private String exchangeMode;

    @Bean
    public ExchangeModeHolder exchangeModeHolder() {
        // 启动期解析一次并闭包固化：每次 getMode() 不再重复 toUpperCase/valueOf
        final ExchangeMode mode = parseExchangeMode(exchangeMode);
        return () -> mode;
    }

    /**
     * 容错解析配置值：剔除首尾空白、大小写不敏感；以 {@link ExchangeMode#getValue()}
     * 为唯一映射来源（枚举常量名与配置值两种写法均可），非法值抛出带可选值清单的可诊断异常。
     */
    private static ExchangeMode parseExchangeMode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalStateException("game.exchange.mode 不能为空白，可选值: local | cloud");
        }
        String trimmed = raw.trim();
        for (ExchangeMode mode : ExchangeMode.values()) {
            if (mode.getValue().equalsIgnoreCase(trimmed) || mode.name().equalsIgnoreCase(trimmed)) {
                return mode;
            }
        }
        throw new IllegalStateException(
                "不支持的 game.exchange.mode: " + raw + "（可选值: local | cloud）");
    }
}
