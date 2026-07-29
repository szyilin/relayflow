package com.relayflow.module.agent.skill;

import lombok.Builder;
import lombok.Value;

/**
 * Catalog metadata for a bindable skill (product surface).
 * <p>
 * Missing {@code domain} is rejected at registry index time.
 * Missing {@code tier} defaults to {@link SkillTier#DEFERRED}.
 */
@Value
@Builder
public class SkillDescriptor {

    /** Stable skill code stored on AgentDef, e.g. {@code task.search}. */
    String code;

    String name;

    String description;

    /** JSON Schema fragment for documentation / UI (optional). */
    String inputSchema;

    /**
     * Business domain aligned with module prefixes, e.g. {@code im}, {@code task}, {@code capability}.
     */
    String domain;

    /**
     * Hot vs deferred. Null is treated as {@link SkillTier#DEFERRED} by the registry.
     */
    SkillTier tier;
}
