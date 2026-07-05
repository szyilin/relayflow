package com.relayflow.framework.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.relayflow.framework.tenant.core.RelayflowTenantLineHandler;
import com.relayflow.framework.tenant.web.TenantWebFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantAutoConfiguration {

    @Bean
    public FilterRegistrationBean<TenantWebFilter> tenantWebFilterRegistration(TenantProperties properties) {
        FilterRegistrationBean<TenantWebFilter> registration = new FilterRegistrationBean<>(new TenantWebFilter(properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public RelayflowTenantLineHandler relayflowTenantLineHandler(TenantProperties properties) {
        return new RelayflowTenantLineHandler(properties);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(RelayflowTenantLineHandler tenantLineHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(tenantLineHandler);
        interceptor.addInnerInterceptor(tenantInterceptor);
        return interceptor;
    }
}
