package com.relayflow.module.agent.skill;

import org.springframework.ai.tool.ToolCallback;

/**
 * Built-in skill: product metadata + Spring AI tool factory.
 */
public interface BuiltinSkill {

    SkillDescriptor descriptor();

    ToolCallback toolCallback();
}
