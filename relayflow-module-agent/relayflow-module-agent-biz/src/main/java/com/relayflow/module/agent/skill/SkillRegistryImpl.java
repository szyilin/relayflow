package com.relayflow.module.agent.skill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.skill.meta.CapabilitySearchSkill;
import com.relayflow.module.agent.skill.meta.CapabilityUseSkill;
import com.relayflow.module.agent.skill.profile.CapabilityProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SkillRegistryImpl implements SkillRegistry {

    private final List<BuiltinSkill> builtinSkills;
    private final ObjectMapper objectMapper;

    private Map<String, BuiltinSkill> index() {
        Map<String, BuiltinSkill> map = new LinkedHashMap<>();
        for (BuiltinSkill skill : builtinSkills) {
            SkillDescriptor descriptor = normalize(skill.descriptor());
            String code = descriptor.getCode();
            if (!StringUtils.hasText(code)) {
                throw new IllegalStateException("BuiltinSkill missing code: " + skill.getClass().getName());
            }
            if (!StringUtils.hasText(descriptor.getDomain())) {
                throw new IllegalStateException("BuiltinSkill missing domain: " + code);
            }
            map.put(code, skill);
        }
        return map;
    }

    private static SkillDescriptor normalize(SkillDescriptor raw) {
        if (raw == null) {
            throw new IllegalStateException("BuiltinSkill returned null descriptor");
        }
        SkillTier tier = raw.getTier() == null ? SkillTier.DEFERRED : raw.getTier();
        if (raw.getTier() == tier
                && StringUtils.hasText(raw.getDomain())
                && StringUtils.hasText(raw.getCode())) {
            return raw;
        }
        return SkillDescriptor.builder()
                .code(raw.getCode())
                .name(raw.getName())
                .description(raw.getDescription())
                .inputSchema(raw.getInputSchema())
                .domain(raw.getDomain())
                .tier(tier)
                .build();
    }

    private SkillDescriptor descriptorOf(BuiltinSkill skill) {
        return normalize(skill.descriptor());
    }

    @Override
    public List<SkillDescriptor> listCatalog() {
        return builtinSkills.stream().map(this::descriptorOf).toList();
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

    @Override
    public Optional<BuiltinSkill> findSkill(String skillCode) {
        if (!StringUtils.hasText(skillCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(index().get(skillCode.trim()));
    }

    @Override
    public Optional<SkillDescriptor> findDescriptor(String skillCode) {
        return findSkill(skillCode).map(this::descriptorOf);
    }

    @Override
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

    @Override
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

    @Override
    public void validateSkillCodes(List<String> skillCodes) {
        if (skillCodes == null || skillCodes.isEmpty()) {
            return;
        }
        for (String code : skillCodes) {
            if (!isKnown(code)) {
                throw new ServiceException(ErrorCodeConstants.AGENT_SKILL_UNKNOWN.getCode(),
                        "未知的技能编码：" + code);
            }
            if (isMetaSkillCode(code)) {
                throw new ServiceException(ErrorCodeConstants.AGENT_SKILL_UNKNOWN.getCode(),
                        "系统元技能不可绑定：" + code);
            }
        }
    }

    @Override
    public CapabilityProfile parseCapabilityProfileJson(String json) {
        if (!StringUtils.hasText(json) || "null".equalsIgnoreCase(json.trim()) || "{}".equals(json.trim())) {
            return CapabilityProfile.builder().build();
        }
        try {
            CapabilityProfile raw = objectMapper.readValue(json, CapabilityProfile.class);
            if (raw == null) {
                return CapabilityProfile.builder().build();
            }
            List<String> hotPacks = cleanStringList(raw.getHotPacks());
            List<String> deferredDomains = cleanStringList(raw.getDeferredDomains());
            return CapabilityProfile.builder()
                    .hotPacks(hotPacks)
                    .deferredDomains(deferredDomains)
                    .build();
        } catch (JsonProcessingException ex) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID);
        }
    }

    @Override
    public String toCapabilityProfileJson(CapabilityProfile profile) {
        CapabilityProfile safe = profile == null
                ? CapabilityProfile.builder().build()
                : CapabilityProfile.builder()
                .hotPacks(cleanStringList(profile.getHotPacks()))
                .deferredDomains(cleanStringList(profile.getDeferredDomains()))
                .build();
        try {
            return objectMapper.writeValueAsString(safe);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID);
        }
    }

    @Override
    public void validateDeferredDomains(List<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return;
        }
        Set<String> known = knownDomains();
        for (String domain : domains) {
            if (!StringUtils.hasText(domain)) {
                continue;
            }
            String trimmed = domain.trim();
            if (!known.contains(trimmed)) {
                throw new ServiceException(ErrorCodeConstants.AGENT_CAPABILITY_DOMAIN_UNKNOWN.getCode(),
                        "未知的能力域：" + trimmed);
            }
        }
    }

    @Override
    public Set<String> knownDomains() {
        LinkedHashSet<String> domains = new LinkedHashSet<>();
        for (SkillDescriptor descriptor : listCatalog()) {
            if (descriptor == null || !StringUtils.hasText(descriptor.getDomain())) {
                continue;
            }
            if ("capability".equalsIgnoreCase(descriptor.getDomain())) {
                continue;
            }
            domains.add(descriptor.getDomain());
        }
        return Set.copyOf(domains);
    }

    private static List<String> cleanStringList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static boolean isMetaSkillCode(String code) {
        return CapabilitySearchSkill.CODE.equals(code) || CapabilityUseSkill.CODE.equals(code);
    }

    /** OpenAI-safe tool name from skill code ({@code task.search} → {@code task_search}). */
    public static String toToolName(String skillCode) {
        return skillCode.replace('.', '_');
    }
}
