package com.relayflow.module.agent.skill.assembly;

import java.util.Set;

/**
 * Per-run authorization for capability.search / capability.use (ThreadLocal).
 */
public final class CapabilityAuthContext {

    private static final ThreadLocal<CapabilityAuthScope> HOLDER = new ThreadLocal<>();

    private CapabilityAuthContext() {
    }

    public static void set(CapabilityAuthScope scope) {
        HOLDER.set(scope);
    }

    public static CapabilityAuthScope get() {
        return HOLDER.get();
    }

    public static CapabilityAuthScope require() {
        CapabilityAuthScope scope = HOLDER.get();
        if (scope == null) {
            throw new IllegalStateException("CapabilityAuthContext is not bound for this run");
        }
        return scope;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record CapabilityAuthScope(
            Set<String> deferredDomains,
            Set<String> hotSkillCodes,
            Set<String> deferredSkillCodes
    ) {
        public CapabilityAuthScope {
            deferredDomains = deferredDomains == null ? Set.of() : Set.copyOf(deferredDomains);
            hotSkillCodes = hotSkillCodes == null ? Set.of() : Set.copyOf(hotSkillCodes);
            deferredSkillCodes = deferredSkillCodes == null ? Set.of() : Set.copyOf(deferredSkillCodes);
        }
    }
}
