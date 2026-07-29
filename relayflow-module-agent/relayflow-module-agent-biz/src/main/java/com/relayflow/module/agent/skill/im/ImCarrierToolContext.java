package com.relayflow.module.agent.skill.im;

/**
 * Thread-local carrier scope for {@code im.*} BuiltinSkills.
 */
public final class ImCarrierToolContext {

    private static final ThreadLocal<ImCarrierToolScope> HOLDER = new ThreadLocal<>();

    private ImCarrierToolContext() {
    }

    public static void set(ImCarrierToolScope scope) {
        HOLDER.set(scope);
    }

    public static ImCarrierToolScope get() {
        return HOLDER.get();
    }

    public static ImCarrierToolScope require() {
        ImCarrierToolScope scope = HOLDER.get();
        if (scope == null || scope.getConversationId() == null || scope.getTenantId() == null) {
            throw new IllegalStateException("IM carrier tool scope is not bound");
        }
        return scope;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
