package com.relayflow.module.agent.skill.assembly;

import lombok.Builder;
import lombok.Value;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Set;

/**
 * Resolved hot-path tools + optional meta discovery for one Agent run.
 */
@Value
@Builder
public class CapabilityAssembly {

    /** Hot-path skill codes (explicit skillIds ∪ packs ∪ channel forced). */
    List<String> hotSkillCodes;

    /** Authorized deferred domains for search/use. */
    List<String> deferredDomains;

    /** Domain codes to summarize in the system prompt. */
    List<String> authorizedDomains;

    /** Short domain summary appendix (may be empty). */
    String domainSummaryAppendix;

    /** Whether meta tools were attached. */
    boolean metaToolsAttached;

    /** Tool callbacks = hot ∪ meta (when attached). */
    List<ToolCallback> tools;

    /** Hot codes as a set for native-tool correction. */
    Set<String> hotSkillCodeSet;

    /** Deferred skill codes authorized for capability.use. */
    Set<String> deferredSkillCodeSet;
}
