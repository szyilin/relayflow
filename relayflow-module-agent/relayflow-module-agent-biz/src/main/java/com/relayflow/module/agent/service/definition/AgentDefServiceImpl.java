package com.relayflow.module.agent.service.definition;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.controller.app.def.vo.AgentDefRespVO;
import com.relayflow.module.agent.controller.app.def.vo.AgentDefSaveReqVO;
import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import com.relayflow.module.agent.dal.mapper.AgentDefMapper;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.llm.LlmConnectionResolver;
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
        row.setSkillIdsJson("[]");
        applyFields(row, request, true);
        agentDefMapper.insert(row);
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
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AgentDefDO row = requireOwned(id);
        agentDefMapper.deleteById(row.getId());
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
        return vo;
    }
}
