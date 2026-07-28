package com.relayflow.module.agent.skill;

import lombok.Builder;
import lombok.Value;

/**
 * Catalog metadata for a bindable skill (product surface).
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
}
