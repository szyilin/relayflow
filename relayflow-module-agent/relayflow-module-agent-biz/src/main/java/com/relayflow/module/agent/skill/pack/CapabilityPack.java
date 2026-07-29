package com.relayflow.module.agent.skill.pack;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Named hot-path skill bundle for Channel / AgentDef profile assembly.
 */
@Value
@Builder
public class CapabilityPack {

    String id;

    String name;

    String description;

    /** Skill codes included in this pack (order preserved). */
    List<String> skillCodes;
}
