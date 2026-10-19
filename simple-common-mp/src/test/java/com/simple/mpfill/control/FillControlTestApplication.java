package com.simple.mpfill.control;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 门控事实对照组测试应用：排除 simple-common-mp 的 MybatisPlusConfig（容器内无兜底拦截器），
 * 但保留 {@link ControlGateMetaObjectHandler}，用于实证 MyBatis-Plus 对无 fill 注解实体
 * 不会调用 {@code MetaObjectHandler.insertFill} 的门控事实。
 *
 * @author qty
 */
@SpringBootApplication(scanBasePackages = { "com.simple.mpfill.shared", "com.simple.mpfill.control" })
@MapperScan("com.simple.mpfill.shared.mapper")
public class FillControlTestApplication {
}
