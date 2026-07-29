package com.relayflow.module.agent.skill.pack;

import java.util.List;
import java.util.Optional;

public interface CapabilityPackRegistry {

    String IM_CARRIER = "im-carrier";
    String WORKSPACE_DEFAULT = "workspace-default";

    List<CapabilityPack> listPacks();

    Optional<CapabilityPack> find(String packId);

    boolean isKnown(String packId);

    void validatePackIds(List<String> packIds);

    /** Channel-forced pack ids (always merged into hot path). */
    List<String> forcedPackIds(com.relayflow.module.agent.channel.AgentChannel channel);

    /** Resolve skill codes for pack ids (unknown packs skipped). */
    List<String> resolveSkillCodes(List<String> packIds);
}
