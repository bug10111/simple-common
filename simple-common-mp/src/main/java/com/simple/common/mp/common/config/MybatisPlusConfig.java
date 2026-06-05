package com.simple.common.mp.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.simple.common.mp.common.handler.DataScopeSqlHandler;
import com.simple.common.mp.common.interceptor.DataScopeInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MyBatis Plus 配置
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
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        return new PaginationInnerInterceptor(DbType.MYSQL);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> interceptors) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        for (InnerInterceptor inner : interceptors) {
            interceptor.addInnerInterceptor(inner);
        }
        return interceptor;
    }
}
