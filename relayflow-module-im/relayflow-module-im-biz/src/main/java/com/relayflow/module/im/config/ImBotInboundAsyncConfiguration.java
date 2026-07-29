package com.relayflow.module.im.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ImBotInboundAsyncConfiguration {

    public static final String BOT_INBOUND_EXECUTOR = "imBotInboundExecutor";

    @Bean(name = BOT_INBOUND_EXECUTOR)
    public TaskExecutor imBotInboundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("im-bot-inbound-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}
