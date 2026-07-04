package com.relayflow.framework.mybatis.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration
@MapperScan("com.relayflow.module.**.dal.mysql")
public class MybatisAutoConfiguration {
}
