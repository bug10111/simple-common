package com.simple.mpfill.positive;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 兜底填充正向测试应用：装载 simple-common-mp 的 MybatisPlusConfig（含兜底拦截器装配），
 * 并注册 {@code @Primary} 测试处理器模拟宿主固定字段兜底语义。
 *
 * @author qty
 */
@SpringBootApplication(exclude = MybatisPlusInnerInterceptorAutoConfiguration.class,
        scanBasePackages = { "com.simple.mpfill.shared", "com.simple.mpfill.positive" })
@MapperScan("com.simple.mpfill.shared.mapper")
public class FillPositiveTestApplication {
}
