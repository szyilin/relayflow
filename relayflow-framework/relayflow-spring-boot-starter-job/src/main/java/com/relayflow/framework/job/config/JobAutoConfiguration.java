package com.relayflow.framework.job.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@AutoConfiguration
@ConditionalOnClass(XxlJobSpringExecutor.class)
@ConditionalOnProperty(prefix = "relayflow.job", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JobProperties.class)
public class JobAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JobAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public XxlJobSpringExecutor xxlJobSpringExecutor(JobProperties properties) {
        JobProperties.Executor executor = properties.getExecutor();
        log.info("XXL-JOB executor enabled: appname={}, adminAddresses={}",
                executor.getAppname(), properties.getAdminAddresses());

        XxlJobSpringExecutor springExecutor = new XxlJobSpringExecutor();
        springExecutor.setAdminAddresses(properties.getAdminAddresses());
        springExecutor.setAccessToken(properties.getAccessToken());
        springExecutor.setAppname(executor.getAppname());
        if (StringUtils.hasText(executor.getAddress())) {
            springExecutor.setAddress(executor.getAddress());
        }
        if (StringUtils.hasText(executor.getIp())) {
            // Only set when resolvable; unresolved hostnames NPE in XXL-JOB 3.x EmbedServer.
            springExecutor.setIp(executor.getIp());
        }
        springExecutor.setPort(executor.getPort());
        springExecutor.setLogPath(executor.getLogPath());
        springExecutor.setLogRetentionDays(executor.getLogRetentionDays());
        return springExecutor;
    }
}
