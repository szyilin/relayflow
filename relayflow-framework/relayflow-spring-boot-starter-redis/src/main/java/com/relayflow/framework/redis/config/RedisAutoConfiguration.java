package com.relayflow.framework.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.redis.core.RedisTokenRevocationStore;
import com.relayflow.framework.redis.core.RelayflowRedisCache;
import com.relayflow.framework.security.core.TokenRevocationStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(RedisConnectionFactory.class)
@EnableConfigurationProperties(CacheProperties.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public RelayflowRedisCache relayflowRedisCache(StringRedisTemplate stringRedisTemplate,
                                                   ObjectMapper objectMapper,
                                                   CacheProperties cacheProperties) {
        return new RelayflowRedisCache(stringRedisTemplate, objectMapper, cacheProperties);
    }

    @Bean
    @ConditionalOnMissingBean(TokenRevocationStore.class)
    public TokenRevocationStore tokenRevocationStore(RelayflowRedisCache relayflowRedisCache,
                                                     CacheProperties cacheProperties) {
        return new RedisTokenRevocationStore(relayflowRedisCache, cacheProperties);
    }
}
