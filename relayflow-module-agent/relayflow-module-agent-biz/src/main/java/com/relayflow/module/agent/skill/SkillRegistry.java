package com.relayflow.module.agent.skill;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

public interface SkillRegistry {

    List<SkillDescriptor> listCatalog();

    /**
     * Resolve bound skill codes to tool callbacks (order preserved).
     */
    List<ToolCallback> resolveToolCallbacks(List<String> skillCodes);

    boolean isKnown(String skillCode);

    List<String> parseSkillIdsJson(String json);

    String toSkillIdsJson(List<String> skillCodes);

    void validateSkillCodes(List<String> skillCodes);
}
