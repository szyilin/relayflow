package com.relayflow.module.agent.skill.pack;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.agent.channel.AgentChannel;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.skill.im.ImReadHistorySkill;
import com.relayflow.module.agent.skill.im.ImReadMessageSkill;
import com.relayflow.module.agent.skill.im.ImReadNearbySkill;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CapabilityPackRegistryImpl implements CapabilityPackRegistry {

    private final Map<String, CapabilityPack> packs;

    public CapabilityPackRegistryImpl() {
        Map<String, CapabilityPack> map = new LinkedHashMap<>();
        map.put(IM_CARRIER, CapabilityPack.builder()
                .id(IM_CARRIER)
                .name("IM Carrier 热路径")
                .description("IM 通道强制热路径：当前会话消息读。")
                .skillCodes(List.of(
                        ImReadMessageSkill.CODE,
                        ImReadNearbySkill.CODE,
                        ImReadHistorySkill.CODE))
                .build());
        map.put(WORKSPACE_DEFAULT, CapabilityPack.builder()
                .id(WORKSPACE_DEFAULT)
                .name("工作台默认热路径")
                .description("工作台 Session 默认热路径起点（平台约定子集，当前为空，依赖 Agent 显式 skillIds / 其他包）。")
                .skillCodes(List.of())
                .build());
        this.packs = Map.copyOf(map);
    }

    @Override
    public List<CapabilityPack> listPacks() {
        return List.copyOf(packs.values());
    }

    @Override
    public Optional<CapabilityPack> find(String packId) {
        if (!StringUtils.hasText(packId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(packs.get(packId.trim()));
    }

    @Override
    public boolean isKnown(String packId) {
        return find(packId).isPresent();
    }

    @Override
    public void validatePackIds(List<String> packIds) {
        if (packIds == null || packIds.isEmpty()) {
            return;
        }
        for (String id : packIds) {
            if (!isKnown(id)) {
                throw new ServiceException(ErrorCodeConstants.AGENT_CAPABILITY_PACK_UNKNOWN.getCode(),
                        "未知的能力包：" + id);
            }
        }
    }

    @Override
    public List<String> forcedPackIds(AgentChannel channel) {
        if (channel == AgentChannel.IM_BOT) {
            return List.of(IM_CARRIER);
        }
        return List.of();
    }

    @Override
    public List<String> resolveSkillCodes(List<String> packIds) {
        if (packIds == null || packIds.isEmpty()) {
            return List.of();
        }
        List<String> codes = new ArrayList<>();
        for (String id : packIds) {
            find(id).ifPresent(pack -> {
                if (pack.getSkillCodes() != null) {
                    codes.addAll(pack.getSkillCodes());
                }
            });
        }
        return List.copyOf(codes);
    }
}
