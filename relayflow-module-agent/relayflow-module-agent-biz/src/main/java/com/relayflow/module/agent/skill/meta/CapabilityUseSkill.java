package com.relayflow.module.agent.skill.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.module.agent.skill.BuiltinSkill;
import com.relayflow.module.agent.skill.SkillDescriptor;
import com.relayflow.module.agent.skill.SkillRegistry;
import com.relayflow.module.agent.skill.SkillRegistryImpl;
import com.relayflow.module.agent.skill.SkillTier;
import com.relayflow.module.agent.skill.assembly.CapabilityAuthContext;
import lombok.Data;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * Meta tool: invoke a deferred skill by code after ACL / whitelist checks.
 */
@Component
public class CapabilityUseSkill implements BuiltinSkill {

    public static final String CODE = "capability.use";
    public static final String DOMAIN = "capability";

    private final SkillRegistry skillRegistry;
    private final ObjectMapper objectMapper;

    public CapabilityUseSkill(@Lazy SkillRegistry skillRegistry, ObjectMapper objectMapper) {
        this.skillRegistry = skillRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public SkillDescriptor descriptor() {
        return SkillDescriptor.builder()
                .code(CODE)
                .name("调用能力")
                .description("按 skill code + JSON 参数调用已授权的长尾技能。热路径已有的工具请直接调用，不要经本工具绕行。")
                .inputSchema("""
                        {"type":"object","properties":{"toolCode":{"type":"string"},"arguments":{"type":"object"}},"required":["toolCode"]}
                        """)
                .domain(DOMAIN)
                .tier(SkillTier.HOT)
                .build();
    }

    @Override
    public ToolCallback toolCallback() {
        return FunctionToolCallback.builder(SkillRegistryImpl.toToolName(CODE), this::use)
                .description(descriptor().getDescription())
                .inputType(Input.class)
                .build();
    }

    Object use(Input input) {
        CapabilityAuthContext.CapabilityAuthScope scope = CapabilityAuthContext.require();
        if (input == null || !StringUtils.hasText(input.getToolCode())) {
            return Map.of("ok", false, "error", "capability.use 需要 toolCode");
        }
        String code = input.getToolCode().trim();
        if (CODE.equals(code) || CapabilitySearchSkill.CODE.equals(code)) {
            return Map.of("ok", false, "error", "禁止经 capability.use 调用 meta 工具");
        }
        if (scope.hotSkillCodes().contains(code)) {
            String toolName = SkillRegistryImpl.toToolName(code);
            return Map.of(
                    "ok", false,
                    "error", "native_tool_correction",
                    "message", "该技能已在热路径工具列表中，请直接调用工具 " + toolName + "，不要经 capability.use。",
                    "toolCode", code,
                    "toolName", toolName);
        }
        if (!scope.deferredSkillCodes().contains(code)) {
            return Map.of("ok", false, "error", "未授权的技能：" + code);
        }
        Optional<BuiltinSkill> skill = skillRegistry.findSkill(code);
        if (skill.isEmpty()) {
            return Map.of("ok", false, "error", "未知技能：" + code);
        }
        String argsJson = toArgumentsJson(input.getArguments());
        try {
            String result = skill.get().toolCallback().call(argsJson);
            return Map.of("ok", true, "toolCode", code, "result", result == null ? "" : result);
        } catch (Exception ex) {
            String message = ex.getMessage();
            return Map.of(
                    "ok", false,
                    "error", "invoke_failed",
                    "message", StringUtils.hasText(message) ? message : "调用失败",
                    "toolCode", code);
        }
    }

    private String toArgumentsJson(Object arguments) {
        if (arguments == null) {
            return "{}";
        }
        if (arguments instanceof String s) {
            return StringUtils.hasText(s) ? s : "{}";
        }
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (Exception ex) {
            return "{}";
        }
    }

    @Data
    public static class Input {
        private String toolCode;
        /** Raw JSON object from the model; Spring AI may bind as Map. */
        private Object arguments;
    }
}
