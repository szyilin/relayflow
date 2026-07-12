package com.relayflow.module.infra.framework.realtime;

import com.relayflow.module.infra.api.realtime.RealtimeDomainMessageHandler;
import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.websocket.router.DomainMessageRouter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "relayflow.websocket", name = "enable", havingValue = "true", matchIfMissing = true)
public class DomainMessageRouterConfiguration {

    @Bean
    public DomainMessageRouter domainMessageRouter(List<RealtimeDomainMessageHandler> handlers,
                                                   RealtimeSessionSender sessionSender) {
        return new DomainMessageRouter(handlers, sessionSender);
    }
}
