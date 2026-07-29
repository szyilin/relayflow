package com.relayflow.module.agent.skill;

/**
 * Hot-path vs deferred (discovered via capability.search / capability.use).
 */
public enum SkillTier {

    /** May appear directly in the model tool list when bound / packed. */
    HOT,

    /** Not flattened into tools by default; invoke via capability.use. */
    DEFERRED
}
