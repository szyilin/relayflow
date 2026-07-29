package com.relayflow.module.im.service.bot.ingress;

import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.im.config.ImBotInboundAsyncConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * After-commit async Bot inbound with per-conversation serial execution.
 */
@Slf4j
@Service
public class BotInboundAsyncDispatcher {

    private final TaskExecutor botInboundExecutor;
    private final ConcurrentHashMap<Long, Object> conversationLocks = new ConcurrentHashMap<>();

    public BotInboundAsyncDispatcher(
            @Qualifier(ImBotInboundAsyncConfiguration.BOT_INBOUND_EXECUTOR) TaskExecutor botInboundExecutor) {
        this.botInboundExecutor = botInboundExecutor;
    }

    public void scheduleAfterCommit(Long conversationId, Runnable task) {
        if (task == null) {
            return;
        }
        Long tenantId = TenantContextHolder.get();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Runnable asyncTask = () -> runSerialized(conversationId, tenantId, authentication, task);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    botInboundExecutor.execute(asyncTask);
                }
            });
            return;
        }
        botInboundExecutor.execute(asyncTask);
    }

    private void runSerialized(Long conversationId, Long tenantId, Authentication authentication, Runnable task) {
        Object lock = conversationId == null
                ? new Object()
                : conversationLocks.computeIfAbsent(conversationId, id -> new Object());
        synchronized (lock) {
            Long previousTenant = TenantContextHolder.get();
            SecurityContext previousContext = SecurityContextHolder.getContext();
            try {
                if (tenantId != null) {
                    TenantContextHolder.set(tenantId);
                }
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
                task.run();
            } catch (Exception ex) {
                log.warn("Async bot inbound failed: conversationId={}", conversationId, ex);
            } finally {
                if (previousTenant != null) {
                    TenantContextHolder.set(previousTenant);
                } else {
                    TenantContextHolder.clear();
                }
                SecurityContextHolder.setContext(previousContext);
            }
        }
    }
}
