package com.relayflow.framework.web.config;

import com.relayflow.framework.web.handler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebAutoConfiguration {
}
