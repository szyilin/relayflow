package com.relayflow.framework.messaging;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DomainEventListenerStarter {

    private final DomainEventListenerContainer container;

    @PostConstruct
    public void start() {
        container.start();
    }
}
