package com.simple.common.mp.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MyBatis Plus 配置属性
 * <p>
 * 支持通过 simple.mp.db-type 显式指定分页方言；未配置时按数据源 JDBC URL 自动探测数据库类型。
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.mp")
public class MpProperties {

    /**
     * 分页方言，对应 MyBatis-Plus DbType 枚举名（如 POSTGRE_SQL、MYSQL）；留空时按数据源 JDBC URL 自动探测
     */
    private String dbType;

}
