package com.relayflow.module.agent.builder;

import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Assembles {@link RuntimeAgent} from definition (grok AgentBuilder analogue).
 */
@Component
public class AgentBuilder {

    static final String PLATFORM_BASE_PROMPT = """
            You are a RelayFlow enterprise assistant running inside an organization workspace.
            Follow the persona instructions below.
            Only use tools that are explicitly provided; if no tools are available, answer helpfully in plain language.
            Respect tenant and user permission boundaries; do not invent access to private data.
            Reply in the same language the user uses unless the persona requires otherwise.
            """;

    /**
     * @param def   persisted agent definition
     * @param tools skill tool callbacks (may be empty)
     */
    public RuntimeAgent build(AgentDefDO def, List<ToolCallback> tools) {
        return build(def, tools, null);
    }

    /**
     * @param domainSummaryAppendix short authorized-domain blurb (may be null/blank)
     */
    public RuntimeAgent build(AgentDefDO def, List<ToolCallback> tools, String domainSummaryAppendix) {
        String persona = def.getSystemPrompt() == null ? "" : def.getSystemPrompt().trim();
        String systemPrompt = PLATFORM_BASE_PROMPT.trim();
        if (StringUtils.hasText(persona)) {
            systemPrompt = systemPrompt + "\n\n# Persona\n" + persona;
        }
        if (StringUtils.hasText(domainSummaryAppendix)) {
            systemPrompt = systemPrompt + "\n\n" + domainSummaryAppendix.trim();
        }
        List<ToolCallback> safeTools = tools == null ? List.of() : List.copyOf(tools);
        return RuntimeAgent.builder()
                .agentId(def.getId())
                .name(def.getName())
                .systemPrompt(systemPrompt)
                .modelRef(def.getModelRef())
                .tools(safeTools)
                .build();
    }

    /** Visible for unit tests. */
    public String assembleSystemPrompt(String persona) {
        AgentDefDO stub = new AgentDefDO();
        stub.setSystemPrompt(persona);
        stub.setId(0L);
        stub.setName("stub");
        stub.setModelRef("stub");
        return build(stub, List.of()).getSystemPrompt();
    }
}
