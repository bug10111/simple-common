package com.simple.common.mp.common.properties;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis Plus 配置属性
 * <p>
 * 支持通过 simple.mp.db-type 显式指定分页方言；未配置时按数据源 JDBC URL 自动探测。
 * dbType 直接绑定 {@link DbType} 枚举：绑定对大小写不敏感（如 mysql/MySQL/MYSQL 均可），
 * 非法取值在绑定期即抛出可读错误，无需消费侧手工 {@code DbType.valueOf} 解析。
 *
 * @author qty
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "simple.mp")
public class MpProperties {

    /**
     * 分页方言，对应 MyBatis-Plus DbType 枚举（如 POSTGRE_SQL、MYSQL，大小写不敏感）；
     * 未配置时按数据源 JDBC URL 自动探测
     */
    private DbType dbType;

}
