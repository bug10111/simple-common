package com.simple.common.mp.common.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.toolkit.JdbcUtils;
import com.simple.common.mp.common.handler.DataScopeSqlHandler;
import com.simple.common.mp.common.interceptor.DataScopeInnerInterceptor;
import com.simple.common.mp.common.properties.MpProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * MyBatis Plus 配置
 * <p>
 * 分页方言通过 simple.mp.db-type 配置指定；未配置时按数据源 JDBC URL 自动探测数据库类型。
 *
 * @author qty
 */
@Configuration
@ComponentScan(basePackages = { "com.simple.common.mp" })
public class MybatisPlusConfig {

    @Bean
    @ConditionalOnBean(DataScopeSqlHandler.class)
    public DataScopeInnerInterceptor dataScopeInnerInterceptor(DataScopeSqlHandler dataScopeSqlHandler) {
        return new DataScopeInnerInterceptor(dataScopeSqlHandler);
    }

    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor(MpProperties mpProperties, DataSource dataSource) {
        DbType dbType = resolveDbType(mpProperties, dataSource);
        return new PaginationInnerInterceptor(dbType);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> interceptors) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        for (InnerInterceptor inner : interceptors) {
            interceptor.addInnerInterceptor(inner);
        }
        return interceptor;
    }

    /**
     * 解析分页方言
     * <p>
     * 配置了 simple.mp.db-type 时直接使用配置值（对应 DbType 枚举名）；未配置时从数据源连接元数据读取 JDBC URL 自动探测。
     *
     * @param mpProperties MyBatis Plus 配置属性
     * @param dataSource   数据源
     * @return 分页方言
     */
    private DbType resolveDbType(MpProperties mpProperties, DataSource dataSource) {
        // 已显式配置方言时直接使用配置值，非法枚举名由 DbType.valueOf 抛出可诊断错误
        if (StrUtil.isNotBlank(mpProperties.getDbType())) {
            return DbType.valueOf(mpProperties.getDbType().trim());
        }

        // 未配置方言时从数据源连接元数据读取 JDBC URL，交由 MyBatis-Plus JdbcUtils 探测数据库类型
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            return JdbcUtils.getDbType(jdbcUrl);
        } catch (SQLException e) {
            throw new IllegalStateException("分页方言自动探测失败，无法从数据源获取 JDBC URL", e);
        }
    }
}
