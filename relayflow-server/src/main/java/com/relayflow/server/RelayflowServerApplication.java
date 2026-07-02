package com.relayflow.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.relayflow")
public class RelayflowServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RelayflowServerApplication.class, args);
    }
}
