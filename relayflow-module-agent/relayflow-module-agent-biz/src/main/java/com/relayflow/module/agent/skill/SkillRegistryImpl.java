package com.relayflow.module.agent.skill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SkillRegistryImpl implements SkillRegistry {

    private final List<BuiltinSkill> builtinSkills;
    private final ObjectMapper objectMapper;

    private Map<String, BuiltinSkill> index() {
        Map<String, BuiltinSkill> map = new LinkedHashMap<>();
        for (BuiltinSkill skill : builtinSkills) {
            map.put(skill.descriptor().getCode(), skill);
        }
        return map;
    }

    @Override
    public List<SkillDescriptor> listCatalog() {
        return builtinSkills.stream().map(BuiltinSkill::descriptor).toList();
    }

    @Override
    public List<ToolCallback> resolveToolCallbacks(List<String> skillCodes) {
        if (skillCodes == null || skillCodes.isEmpty()) {
            return List.of();
        }
        Map<String, BuiltinSkill> byCode = index();
        List<ToolCallback> tools = new ArrayList<>();
        for (String code : skillCodes) {
            BuiltinSkill skill = byCode.get(code);
            if (skill != null) {
                tools.add(skill.toolCallback());
            }
        }
        return List.copyOf(tools);
    }

    @Override
    public boolean isKnown(String skillCode) {
        return StringUtils.hasText(skillCode) && index().containsKey(skillCode.trim());
    }

    public List<String> parseSkillIdsJson(String json) {
        if (!StringUtils.hasText(json) || "null".equalsIgnoreCase(json.trim())) {
            return List.of();
        }
        try {
            List<String> raw = objectMapper.readValue(json, new TypeReference<>() {
            });
            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            List<String> cleaned = new ArrayList<>();
            for (String code : raw) {
                if (StringUtils.hasText(code)) {
                    cleaned.add(code.trim());
                }
            }
            return List.copyOf(cleaned);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID);
        }
    }

    public String toSkillIdsJson(List<String> skillCodes) {
        List<String> codes = skillCodes == null ? List.of() : skillCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        try {
            return objectMapper.writeValueAsString(codes);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID);
        }
    }

    public void validateSkillCodes(List<String> skillCodes) {
        if (skillCodes == null || skillCodes.isEmpty()) {
            return;
        }
        for (String code : skillCodes) {
            if (!isKnown(code)) {
                throw new ServiceException(ErrorCodeConstants.AGENT_SKILL_UNKNOWN.getCode(),
                        "未知的技能编码：" + code);
            }
        }
    }

    /** OpenAI-safe tool name from skill code ({@code task.search} → {@code task_search}). */
    public static String toToolName(String skillCode) {
        return skillCode.replace('.', '_');
    }
}
