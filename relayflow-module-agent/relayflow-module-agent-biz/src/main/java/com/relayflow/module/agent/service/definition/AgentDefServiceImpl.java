package com.relayflow.module.agent.service.definition;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.controller.app.def.vo.AgentCapabilityProfileVO;
import com.relayflow.module.agent.controller.app.def.vo.AgentDefRespVO;
import com.relayflow.module.agent.controller.app.def.vo.AgentDefSaveReqVO;
import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import com.relayflow.module.agent.dal.mapper.AgentDefMapper;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.llm.LlmConnectionResolver;
import com.relayflow.module.agent.skill.SkillRegistry;
import com.relayflow.module.agent.skill.pack.CapabilityPackRegistry;
import com.relayflow.module.agent.skill.profile.CapabilityProfile;
import com.relayflow.module.im.api.bot.ImBotProvisioningApi;
import com.relayflow.module.im.api.bot.dto.ImBotProvisionAgentCarrierCommand;
import com.relayflow.module.im.api.bot.dto.ImBotProvisionResult;
import com.relayflow.module.im.api.bot.dto.ImBotRetireAgentCarrierCommand;
import com.relayflow.module.im.api.bot.dto.ImBotUpdateAgentCarrierProfileCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentDefServiceImpl implements AgentDefService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_DISABLED = "disabled";
    private static final String VISIBILITY_PRIVATE = "private";

    private final AgentDefMapper agentDefMapper;
    private final LlmConnectionResolver llmConnectionResolver;
    private final SkillRegistry skillRegistry;
    private final CapabilityPackRegistry capabilityPackRegistry;
    private final ImBotProvisioningApi imBotProvisioningApi;

    @Override
    public List<AgentDefRespVO> listMine() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        return agentDefMapper.selectList(Wrappers.<AgentDefDO>lambdaQuery()
                        .eq(AgentDefDO::getTenantId, tenantId)
                        .eq(AgentDefDO::getOwnerUserId, userId)
                        .orderByDesc(AgentDefDO::getUpdateTime))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public AgentDefRespVO getMine(Long id) {
        return toVo(requireOwned(id));
    }

    @Override
    @Transactional
    public Long create(AgentDefSaveReqVO request) {
        validateSave(request);
        llmConnectionResolver.requireEnabled(request.getModelRef().trim());

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        AgentDefDO row = new AgentDefDO();
        row.setTenantId(tenantId);
        row.setOwnerUserId(userId);
        row.setVisibility(VISIBILITY_PRIVATE);
        applyFields(row, request, true);
        agentDefMapper.insert(row);
        bindCarrier(row);
        return row.getId();
    }

    @Override
    @Transactional
    public void update(AgentDefSaveReqVO request) {
        if (request.getId() == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID);
        }
        validateSave(request);
        llmConnectionResolver.requireEnabled(request.getModelRef().trim());
        AgentDefDO row = requireOwned(request.getId());
        applyFields(row, request, false);
        agentDefMapper.updateById(row);
        syncCarrierProfile(row);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AgentDefDO row = requireOwned(id);
        retireCarrier(row);
        agentDefMapper.deleteById(row.getId());
    }

    private void bindCarrier(AgentDefDO row) {
        ImBotProvisionAgentCarrierCommand command = new ImBotProvisionAgentCarrierCommand();
        command.setTenantId(row.getTenantId());
        command.setAgentId(row.getId());
        command.setName(row.getName());
        command.setDescription(row.getDescription());
        command.setOwnerUserId(row.getOwnerUserId());
        ImBotProvisionResult result = imBotProvisioningApi.provisionAgentCarrier(command);
        row.setCarrierBotId(result.getBotId());
        agentDefMapper.updateById(row);
    }

    private void syncCarrierProfile(AgentDefDO row) {
        if (row.getCarrierBotId() == null) {
            bindCarrier(row);
            return;
        }
        ImBotUpdateAgentCarrierProfileCommand command = new ImBotUpdateAgentCarrierProfileCommand();
        command.setTenantId(row.getTenantId());
        command.setAgentId(row.getId());
        command.setName(row.getName());
        command.setDescription(row.getDescription());
        try {
            imBotProvisioningApi.updateAgentCarrierProfile(command);
        } catch (ServiceException ex) {
            // Bot missing — re-provision
            bindCarrier(row);
        }
    }

    private void retireCarrier(AgentDefDO row) {
        ImBotRetireAgentCarrierCommand command = new ImBotRetireAgentCarrierCommand();
        command.setTenantId(row.getTenantId());
        command.setAgentId(row.getId());
        command.setBotId(row.getCarrierBotId());
        imBotProvisioningApi.retireAgentCarrier(command);
    }

    private void applyFields(AgentDefDO row, AgentDefSaveReqVO request, boolean creating) {
        row.setName(request.getName().trim());
        row.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        row.setSystemPrompt(request.getSystemPrompt() == null ? "" : request.getSystemPrompt());
        row.setModelRef(request.getModelRef().trim());
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : STATUS_ACTIVE;
        if (!STATUS_ACTIVE.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID);
        }
        if (creating && !StringUtils.hasText(request.getStatus())) {
            status = STATUS_ACTIVE;
        }
        row.setStatus(status);

        List<String> skillIds = request.getSkillIds() == null ? List.of() : request.getSkillIds();
        skillRegistry.validateSkillCodes(skillIds);
        row.setSkillIdsJson(skillRegistry.toSkillIdsJson(skillIds));

        CapabilityProfile profile = toProfile(request.getCapabilityProfile());
        capabilityPackRegistry.validatePackIds(profile.getHotPacks());
        skillRegistry.validateDeferredDomains(profile.getDeferredDomains());
        row.setCapabilityProfileJson(skillRegistry.toCapabilityProfileJson(profile));
    }

    private static CapabilityProfile toProfile(AgentCapabilityProfileVO vo) {
        if (vo == null) {
            return CapabilityProfile.builder().build();
        }
        return CapabilityProfile.builder()
                .hotPacks(vo.getHotPacks() == null ? List.of() : vo.getHotPacks())
                .deferredDomains(vo.getDeferredDomains() == null ? List.of() : vo.getDeferredDomains())
                .build();
    }

    private static AgentCapabilityProfileVO toProfileVo(CapabilityProfile profile) {
        AgentCapabilityProfileVO vo = new AgentCapabilityProfileVO();
        if (profile == null) {
            vo.setHotPacks(List.of());
            vo.setDeferredDomains(List.of());
            return vo;
        }
        vo.setHotPacks(profile.getHotPacks() == null ? List.of() : profile.getHotPacks());
        vo.setDeferredDomains(profile.getDeferredDomains() == null ? List.of() : profile.getDeferredDomains());
        return vo;
    }

    private void validateSave(AgentDefSaveReqVO request) {
        if (request == null || !StringUtils.hasText(request.getName()) || !StringUtils.hasText(request.getModelRef())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID);
        }
    }

    private AgentDefDO requireOwned(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        AgentDefDO row = agentDefMapper.selectOne(Wrappers.<AgentDefDO>lambdaQuery()
                .eq(AgentDefDO::getTenantId, tenantId)
                .eq(AgentDefDO::getId, id));
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_NOT_FOUND);
        }
        if (!userId.equals(row.getOwnerUserId())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_FORBIDDEN);
        }
        return row;
    }

    private AgentDefRespVO toVo(AgentDefDO row) {
        AgentDefRespVO vo = new AgentDefRespVO();
        vo.setId(row.getId());
        vo.setName(row.getName());
        vo.setDescription(row.getDescription());
        vo.setSystemPrompt(row.getSystemPrompt());
        vo.setModelRef(row.getModelRef());
        vo.setStatus(row.getStatus());
        vo.setOwnerUserId(row.getOwnerUserId());
        vo.setVisibility(row.getVisibility());
        vo.setSkillIds(skillRegistry.parseSkillIdsJson(row.getSkillIdsJson()));
        vo.setCapabilityProfile(toProfileVo(skillRegistry.parseCapabilityProfileJson(row.getCapabilityProfileJson())));
        return vo;
    }
}
