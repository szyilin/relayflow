package com.relayflow.module.agent.skill.assembly;

import com.relayflow.module.agent.channel.AgentChannel;
import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import com.relayflow.module.agent.skill.SkillDescriptor;
import com.relayflow.module.agent.skill.SkillRegistry;
import com.relayflow.module.agent.skill.meta.CapabilitySearchSkill;
import com.relayflow.module.agent.skill.meta.CapabilityUseSkill;
import com.relayflow.module.agent.skill.pack.CapabilityPackRegistry;
import com.relayflow.module.agent.skill.profile.CapabilityProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Assembles hot-path ∪ meta tools and domain summary for a Channel + AgentDef.
 */
@Component
public class CapabilityAssemblyService {

    private static final Map<String, String> DOMAIN_BLURBS = Map.of(
            "im", "会话消息读（当前会话绑定）",
            "task", "待办任务搜索与截止列表",
            "capability", "能力发现（search / use）");

    private final SkillRegistry skillRegistry;
    private final CapabilityPackRegistry packRegistry;
    private final CapabilitySearchSkill capabilitySearchSkill;
    private final CapabilityUseSkill capabilityUseSkill;

    public CapabilityAssemblyService(
            SkillRegistry skillRegistry,
            CapabilityPackRegistry packRegistry,
            @Lazy CapabilitySearchSkill capabilitySearchSkill,
            @Lazy CapabilityUseSkill capabilityUseSkill) {
        this.skillRegistry = skillRegistry;
        this.packRegistry = packRegistry;
        this.capabilitySearchSkill = capabilitySearchSkill;
        this.capabilityUseSkill = capabilityUseSkill;
    }

    public CapabilityAssembly assemble(AgentDefDO def, AgentChannel channel) {
        CapabilityProfile profile = skillRegistry.parseCapabilityProfileJson(
                def == null ? null : def.getCapabilityProfileJson());
        List<String> explicitSkills = skillRegistry.parseSkillIdsJson(
                def == null ? null : def.getSkillIdsJson());

        LinkedHashSet<String> hotCodes = new LinkedHashSet<>();
        if (explicitSkills != null) {
            hotCodes.addAll(explicitSkills);
        }

        List<String> profilePacks = profile.getHotPacks() == null ? List.of() : profile.getHotPacks();
        hotCodes.addAll(packRegistry.resolveSkillCodes(profilePacks));
        hotCodes.addAll(packRegistry.resolveSkillCodes(packRegistry.forcedPackIds(channel)));

        hotCodes.remove(CapabilitySearchSkill.CODE);
        hotCodes.remove(CapabilityUseSkill.CODE);

        List<String> deferredDomains = profile.getDeferredDomains() == null
                ? List.of()
                : profile.getDeferredDomains().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();

        LinkedHashSet<String> deferredSkillCodes = new LinkedHashSet<>();
        for (SkillDescriptor descriptor : skillRegistry.listCatalog()) {
            if (descriptor == null || !StringUtils.hasText(descriptor.getCode())) {
                continue;
            }
            if (isMetaDomain(descriptor.getDomain())) {
                continue;
            }
            if (!deferredDomains.contains(descriptor.getDomain())) {
                continue;
            }
            if (hotCodes.contains(descriptor.getCode())) {
                continue;
            }
            deferredSkillCodes.add(descriptor.getCode());
        }

        boolean attachMeta = !deferredDomains.isEmpty();

        List<String> hotList = List.copyOf(hotCodes);
        List<ToolCallback> tools = new ArrayList<>(skillRegistry.resolveToolCallbacks(hotList));
        if (attachMeta) {
            tools.add(capabilitySearchSkill.toolCallback());
            tools.add(capabilityUseSkill.toolCallback());
        }

        LinkedHashSet<String> authorizedDomains = new LinkedHashSet<>(deferredDomains);
        for (String code : hotList) {
            skillRegistry.findDescriptor(code).ifPresent(d -> {
                if (StringUtils.hasText(d.getDomain()) && !isMetaDomain(d.getDomain())) {
                    authorizedDomains.add(d.getDomain());
                }
            });
        }

        String summary = buildDomainSummary(List.copyOf(authorizedDomains));

        return CapabilityAssembly.builder()
                .hotSkillCodes(hotList)
                .deferredDomains(deferredDomains)
                .authorizedDomains(List.copyOf(authorizedDomains))
                .domainSummaryAppendix(summary)
                .metaToolsAttached(attachMeta)
                .tools(List.copyOf(tools))
                .hotSkillCodeSet(Set.copyOf(hotCodes))
                .deferredSkillCodeSet(Set.copyOf(deferredSkillCodes))
                .build();
    }

    static boolean isMetaDomain(String domain) {
        return domain != null && "capability".equalsIgnoreCase(domain);
    }

    static String buildDomainSummary(List<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Authorized capability domains\n");
        sb.append("You may discover deferred tools in these domains via capability.search, then call them with capability.use.\n");
        sb.append("Hot-path tools already listed in your toolset should be called directly (not via capability.use).\n");
        for (String domain : domains) {
            String blurb = DOMAIN_BLURBS.getOrDefault(domain, "业务能力");
            sb.append("- ").append(domain).append("：").append(blurb).append('\n');
        }
        return sb.toString().trim();
    }
}
