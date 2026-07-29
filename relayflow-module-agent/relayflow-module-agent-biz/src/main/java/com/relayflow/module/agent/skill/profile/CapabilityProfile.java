package com.relayflow.module.agent.skill.profile;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * AgentDef capability configuration (hot packs + searchable domains).
 */
@Value
@Builder
@Jacksonized
public class CapabilityProfile {

    /** Named hot-path packs (e.g. workspace-default). */
    @Builder.Default
    List<String> hotPacks = List.of();

    /** Domains searchable via capability.search / invocable via capability.use. */
    @Builder.Default
    List<String> deferredDomains = List.of();

    public boolean isEmpty() {
        return (hotPacks == null || hotPacks.isEmpty())
                && (deferredDomains == null || deferredDomains.isEmpty());
    }
}
