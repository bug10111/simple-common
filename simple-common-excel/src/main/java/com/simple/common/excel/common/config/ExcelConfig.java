package com.simple.common.excel.common.config;

import com.simple.common.excel.common.service.EasyExcelReadService;
import com.simple.common.excel.common.service.EasyExcelWriteService;
import com.simple.common.excel.common.service.PoiReadService;
import com.simple.common.excel.common.service.PoiWriteService;
import com.simple.common.excel.service.DefaultEasyExcelReadService;
import com.simple.common.excel.service.DefaultEasyExcelWriteService;
import com.simple.common.excel.service.DefaultPoiReadService;
import com.simple.common.excel.service.DefaultPoiWriteService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Created with IntelliJ IDEA
 * Description: excel 模块装配配置。
 * 以自动配置方式装配:按类型条件装配默认服务,业务方注册自己的
 * EasyExcelReadService / EasyExcelWriteService / PoiReadService / PoiWriteService
 * 实现后,对应默认实现自动让位,避免同类型双候选导致启动失败。
 *
 * @author qty
 */
@AutoConfiguration
public class ExcelConfig {

    /**
     * EasyExcel 读取服务默认装配,容器已有同类型 Bean 时让位
     */
    @Bean
    @ConditionalOnMissingBean(EasyExcelReadService.class)
    public EasyExcelReadService defaultEasyExcelReadService() {
        return new DefaultEasyExcelReadService();
    }

    /**
     * EasyExcel 写入服务默认装配,容器已有同类型 Bean 时让位
     */
    @Bean
    @ConditionalOnMissingBean(EasyExcelWriteService.class)
    public EasyExcelWriteService defaultEasyExcelWriteService() {
        return new DefaultEasyExcelWriteService();
    }

    /**
     * POI 读取服务默认装配,容器已有同类型 Bean 时让位
     */
    @Bean
    @ConditionalOnMissingBean(PoiReadService.class)
    public PoiReadService defaultPoiReadService() {
        return new DefaultPoiReadService();
    }

    /**
     * POI 写入服务默认装配,容器已有同类型 Bean 时让位
     */
    @Bean
    @ConditionalOnMissingBean(PoiWriteService.class)
    public PoiWriteService defaultPoiWriteService() {
        return new DefaultPoiWriteService();
    }
}
