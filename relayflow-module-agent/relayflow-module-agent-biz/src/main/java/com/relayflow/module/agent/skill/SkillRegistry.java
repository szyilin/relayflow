package com.relayflow.module.agent.skill;

import com.relayflow.module.agent.skill.profile.CapabilityProfile;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SkillRegistry {

    List<SkillDescriptor> listCatalog();

    /**
     * Resolve bound skill codes to tool callbacks (order preserved).
     */
    List<ToolCallback> resolveToolCallbacks(List<String> skillCodes);

    boolean isKnown(String skillCode);

    Optional<BuiltinSkill> findSkill(String skillCode);

    Optional<SkillDescriptor> findDescriptor(String skillCode);

    List<String> parseSkillIdsJson(String json);

    String toSkillIdsJson(List<String> skillCodes);

    void validateSkillCodes(List<String> skillCodes);

    CapabilityProfile parseCapabilityProfileJson(String json);

    String toCapabilityProfileJson(CapabilityProfile profile);

    void validateDeferredDomains(List<String> domains);

    /** Known non-meta domains from the catalog. */
    Set<String> knownDomains();
}
