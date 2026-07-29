package com.relayflow.module.agent.skill.meta;

import com.relayflow.module.agent.skill.BuiltinSkill;
import com.relayflow.module.agent.skill.SkillDescriptor;
import com.relayflow.module.agent.skill.SkillRegistry;
import com.relayflow.module.agent.skill.SkillRegistryImpl;
import com.relayflow.module.agent.skill.SkillTier;
import com.relayflow.module.agent.skill.assembly.CapabilityAuthContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Meta tool: search skills within authorized deferred domains.
 */
@Component
public class CapabilitySearchSkill implements BuiltinSkill {

    public static final String CODE = "capability.search";
    public static final String DOMAIN = "capability";

    private final SkillRegistry skillRegistry;

    public CapabilitySearchSkill(@Lazy SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    @Override
    public SkillDescriptor descriptor() {
        return SkillDescriptor.builder()
                .code(CODE)
                .name("搜索能力")
                .description("在已授权业务域内按关键词检索可调用技能，返回 code、domain、description 与 inputSchema 摘要。")
                .inputSchema("""
                        {"type":"object","properties":{"query":{"type":"string"},"limit":{"type":"integer","minimum":1,"maximum":20}},"required":["query"]}
                        """)
                .domain(DOMAIN)
                .tier(SkillTier.HOT)
                .build();
    }

    @Override
    public ToolCallback toolCallback() {
        return FunctionToolCallback.builder(SkillRegistryImpl.toToolName(CODE), this::search)
                .description(descriptor().getDescription())
                .inputType(Input.class)
                .build();
    }

    List<SearchHit> search(Input input) {
        CapabilityAuthContext.CapabilityAuthScope scope = CapabilityAuthContext.require();
        String query = input == null || !StringUtils.hasText(input.getQuery())
                ? ""
                : input.getQuery().trim().toLowerCase(Locale.ROOT);
        int limit = normalizeLimit(input == null ? null : input.getLimit(), 8);

        List<SearchHit> hits = new ArrayList<>();
        for (SkillDescriptor descriptor : skillRegistry.listCatalog()) {
            if (descriptor == null || !StringUtils.hasText(descriptor.getCode())) {
                continue;
            }
            if (DOMAIN.equalsIgnoreCase(descriptor.getDomain())) {
                continue;
            }
            if (!scope.deferredDomains().contains(descriptor.getDomain())) {
                continue;
            }
            if (!matches(query, descriptor)) {
                continue;
            }
            hits.add(toHit(descriptor));
            if (hits.size() >= limit) {
                break;
            }
        }
        return hits;
    }

    private static boolean matches(String query, SkillDescriptor descriptor) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        String hay = ((descriptor.getCode() == null ? "" : descriptor.getCode()) + " "
                + (descriptor.getName() == null ? "" : descriptor.getName()) + " "
                + (descriptor.getDescription() == null ? "" : descriptor.getDescription()) + " "
                + (descriptor.getDomain() == null ? "" : descriptor.getDomain()))
                .toLowerCase(Locale.ROOT);
        return hay.contains(query);
    }

    private static SearchHit toHit(SkillDescriptor descriptor) {
        SearchHit hit = new SearchHit();
        hit.setCode(descriptor.getCode());
        hit.setDomain(descriptor.getDomain());
        hit.setName(descriptor.getName());
        hit.setDescription(descriptor.getDescription());
        hit.setInputSchemaSummary(summarizeSchema(descriptor.getInputSchema()));
        hit.setTier(descriptor.getTier() == null ? SkillTier.DEFERRED.name() : descriptor.getTier().name());
        return hit;
    }

    private static String summarizeSchema(String schema) {
        if (!StringUtils.hasText(schema)) {
            return "";
        }
        String trimmed = schema.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= 240) {
            return trimmed;
        }
        return trimmed.substring(0, 237) + "...";
    }

    private static int normalizeLimit(Integer limit, int defaultLimit) {
        if (limit == null || limit < 1) {
            return defaultLimit;
        }
        return Math.min(limit, 20);
    }

    @Data
    public static class Input {
        private String query;
        private Integer limit;
    }

    @Data
    public static class SearchHit {
        private String code;
        private String domain;
        private String name;
        private String description;
        private String inputSchemaSummary;
        private String tier;
    }
}
