package com.simple.common.mp.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Configuration
@ComponentScan(basePackages = { "com.simple.common.mp" })
//@MapperScan(basePackages = { "com.simple.*" })
public class MybatisPlusConfig {

    /**
     * 分页count性能瓶颈。以下设置可以自定义总数
     * page.setOptimizeCountSql(false);
     * page.setSearchCount(false);
     * page.setOptimizeJoinOfCountSql(false);
     * page.setTotal(缓存的数据总数);
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

}
