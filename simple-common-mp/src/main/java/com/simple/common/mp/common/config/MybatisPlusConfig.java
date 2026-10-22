package com.simple.common.mp.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.toolkit.JdbcUtils;
import com.simple.common.mp.common.properties.MpProperties;
import com.simple.common.mp.exception.MPExceptionHandler;
import com.simple.common.mp.generator.CustomIdGenerator;
import com.simple.common.mp.handler.MybatisPlusOperationHandler;
import com.simple.common.mp.interceptor.InsertFillInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

/**
 * MyBatis Plus 配置
 * <p>
 * 分页方言通过 simple.mp.db-type 配置指定；未配置时按数据源 JDBC URL 自动探测。
 * 历史说明：原数据权限行级改写拦截器（DataScopeInnerInterceptor/DataScopeSqlHandler/DataScopeTable）
 * 已随行级机制退役整体移除，租户过滤职责移交 PostgreSQL 库层 RLS 策略。
 * <p>
 * 装配说明：本自动配置以显式 {@code @Bean} 注册库内全部组件（不再使用包级 {@code @ComponentScan}），
 * 各 Bean 均标注 {@code @ConditionalOnMissingBean}，业务工程可按需覆盖；整体剔除本自动配置时
 * 库内组件同步不装配，保持 exclude 语义完整（不含游离的组件扫描残留）。
 *
 * @author qty
 */
@AutoConfiguration
@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnClass(PaginationInnerInterceptor.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(MpProperties.class)
public class MybatisPlusConfig {

    /**
     * 雪花 ID 生成器（ASSIGN_ID 实体）
     */
    @Bean
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    public CustomIdGenerator customIdGenerator() {
        return new CustomIdGenerator();
    }

    /**
     * 固定字段填充处理器（createTime/updateTime）
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MybatisPlusOperationHandler mybatisPlusOperationHandler() {
        return new MybatisPlusOperationHandler();
    }

    /**
     * 唯一键冲突统一异常处理
     */
    @Bean
    @ConditionalOnMissingBean(MPExceptionHandler.class)
    public MPExceptionHandler mpExceptionHandler() {
        return new MPExceptionHandler();
    }

    /**
     * 注册分页内部拦截器
     *
     * @param mpProperties MyBatis Plus 配置属性
     * @param dataSource   数据源
     * @return 分页内部拦截器
     */
    @Bean
    @ConditionalOnMissingBean(PaginationInnerInterceptor.class)
    public PaginationInnerInterceptor paginationInnerInterceptor(MpProperties mpProperties, DataSource dataSource) {
        return new PaginationInnerInterceptor(resolveDbType(mpProperties, dataSource));
    }

    /**
     * 注册 MyBatis-Plus 拦截器链
     * <p>
     * 消费者注册的其他 {@link InnerInterceptor} Bean（多租户、动态表名、防全表更新等）会一并通过
     * {@code List<InnerInterceptor>} 注入；此处显式排序保证 {@link PaginationInnerInterceptor}
     * 恒在链尾——分页的 count/limit 必须基于前序改写型拦截器（租户条件等）处理后的 SQL，
     * 顺序错误会导致分页结果缺失租户/数据权限条件。
     *
     * @param interceptors 容器内全部内部拦截器
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> interceptors) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptors.stream()
                .sorted(Comparator.comparing(inner -> !(inner instanceof PaginationInnerInterceptor)))
                .forEach(interceptor::addInnerInterceptor);
        return interceptor;
    }

    /**
     * 注册插入固定字段兜底填充拦截器
     * <p>
     * 处理器经 {@link ObjectProvider} 延迟获取；容器内无处理器或候选不唯一（未标 @Primary）时
     * 拦截器安全跳过，不影响业务插入。
     *
     * @param metaObjectHandlerProvider 容器内元对象填充处理器提供者
     * @return 插入填充拦截器
     */
    @Bean
    @ConditionalOnMissingBean(InsertFillInterceptor.class)
    public InsertFillInterceptor insertFillInterceptor(ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider) {
        return new InsertFillInterceptor(metaObjectHandlerProvider);
    }

    /**
     * 解析分页方言
     * <p>
     * 配置了 simple.mp.db-type 时直接使用配置值（绑定期已完成枚举合法性校验）；
     * 未配置时从数据源连接元数据读取 JDBC URL 自动探测，探测失败或无法识别时抛出
     * 带操作指引的明确错误，引导显式配置，避免问题被拖到首次分页查询时才以难定位的形态暴露。
     *
     * @param mpProperties MyBatis Plus 配置属性
     * @param dataSource   数据源
     * @return 分页方言
     */
    private DbType resolveDbType(MpProperties mpProperties, DataSource dataSource) {
        // 已显式配置方言时直接使用配置值
        DbType configured = mpProperties.getDbType();
        if (configured != null) {
            return configured;
        }

        // 未配置方言时从数据源连接元数据读取 JDBC URL，交由 MyBatis-Plus JdbcUtils 探测数据库类型
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            DbType detected = JdbcUtils.getDbType(jdbcUrl);
            if (detected == null || detected == DbType.OTHER) {
                throw new IllegalStateException(
                        "无法从 JDBC URL 识别分页方言，请通过 simple.mp.db-type 显式配置（JDBC URL: " + jdbcUrl + "）");
            }
            return detected;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "分页方言自动探测失败，无法从数据源获取 JDBC URL；可通过 simple.mp.db-type 显式配置绕过启动期探测", e);
        }
    }
}
