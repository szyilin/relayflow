package com.relayflow.module.agent.service.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.builder.AgentBuilder;
import com.relayflow.module.agent.builder.RuntimeAgent;
import com.relayflow.module.agent.controller.app.automation.vo.AgentAutomationRespVO;
import com.relayflow.module.agent.controller.app.automation.vo.AgentAutomationSaveReqVO;
import com.relayflow.module.agent.dal.dataobject.AgentAutomationDO;
import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import com.relayflow.module.agent.dal.mapper.AgentAutomationMapper;
import com.relayflow.module.agent.dal.mapper.AgentDefMapper;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.runtime.AgentRunCommand;
import com.relayflow.module.agent.runtime.AgentRunResult;
import com.relayflow.module.agent.runtime.AgentRuntime;
import com.relayflow.module.agent.skill.SkillRegistry;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentAutomationServiceImpl implements AgentAutomationService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_DISABLED = "disabled";
    private static final String AGENT_ACTIVE = "active";
    private static final String RUN_SUCCESS = "success";
    private static final String RUN_FAILED = "failed";
    private static final String RUN_SKIPPED = "skipped";

    private final AgentAutomationMapper automationMapper;
    private final AgentDefMapper agentDefMapper;
    private final AgentBuilder agentBuilder;
    private final AgentRuntime agentRuntime;
    private final SkillRegistry skillRegistry;
    private final ImBotApi imBotApi;

    @Override
    public List<AgentAutomationRespVO> listMine() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        List<AgentAutomationDO> rows = automationMapper.selectList(Wrappers.<AgentAutomationDO>lambdaQuery()
                .eq(AgentAutomationDO::getTenantId, tenantId)
                .eq(AgentAutomationDO::getOwnerUserId, userId)
                .orderByDesc(AgentAutomationDO::getUpdateTime));
        Map<Long, AgentDefDO> agents = loadAgents(tenantId, rows);
        return rows.stream().map(row -> toVo(row, agents.get(row.getAgentId()))).toList();
    }

    @Override
    public AgentAutomationRespVO getMine(Long id) {
        AgentAutomationDO row = requireOwned(id);
        AgentDefDO agent = agentDefMapper.selectById(row.getAgentId());
        return toVo(row, agent);
    }

    @Override
    @Transactional
    public Long create(AgentAutomationSaveReqVO request) {
        validateSave(request);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        AgentDefDO agent = requireOwnedAgent(request.getAgentId(), tenantId, userId);
        requireCarrier(agent);

        AgentAutomationDO row = new AgentAutomationDO();
        row.setTenantId(tenantId);
        row.setOwnerUserId(userId);
        applyFields(row, request, userId);
        row.setNextRunAt(AutomationCronSupport.nextRunAfter(row.getCronExpr(), OffsetDateTime.now()));
        automationMapper.insert(row);
        return row.getId();
    }

    @Override
    @Transactional
    public void update(AgentAutomationSaveReqVO request) {
        if (request.getId() == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_AUTOMATION_INVALID);
        }
        validateSave(request);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        AgentAutomationDO row = requireOwned(request.getId());
        AgentDefDO agent = requireOwnedAgent(request.getAgentId(), tenantId, userId);
        requireCarrier(agent);
        applyFields(row, request, userId);
        row.setNextRunAt(AutomationCronSupport.nextRunAfter(row.getCronExpr(), OffsetDateTime.now()));
        automationMapper.updateById(row);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AgentAutomationDO row = requireOwned(id);
        automationMapper.deleteById(row.getId());
    }

    @Override
    public void runNow(Long id) {
        AgentAutomationDO row = requireOwned(id);
        if (!STATUS_ACTIVE.equals(row.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_AUTOMATION_DISABLED);
        }
        executeOne(row, true);
    }

    @Override
    public int runDueAutomations() {
        OffsetDateTime now = OffsetDateTime.now();
        List<AgentAutomationDO> due = automationMapper.selectList(Wrappers.<AgentAutomationDO>lambdaQuery()
                .eq(AgentAutomationDO::getStatus, STATUS_ACTIVE)
                .le(AgentAutomationDO::getNextRunAt, now)
                .orderByAsc(AgentAutomationDO::getNextRunAt)
                .last("LIMIT 50"));
        int ran = 0;
        for (AgentAutomationDO row : due) {
            try {
                executeOne(row, false);
                ran++;
            } catch (Exception ex) {
                log.warn("automation tick failed id={}", row.getId(), ex);
            }
        }
        return ran;
    }

    private void executeOne(AgentAutomationDO row, boolean manual) {
        Long previousTenant = TenantContextHolder.get();
        var previousAuth = SecurityContextHolder.getContext().getAuthentication();
        try {
            TenantContextHolder.set(row.getTenantId());
            LoginUser jobUser = new LoginUser(row.getOwnerUserId(), "automation", row.getTenantId(), "user", List.of());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(jobUser, null, jobUser.getAuthorities()));

            AgentDefDO agent = agentDefMapper.selectOne(Wrappers.<AgentDefDO>lambdaQuery()
                    .eq(AgentDefDO::getTenantId, row.getTenantId())
                    .eq(AgentDefDO::getId, row.getAgentId()));
            if (agent == null || !AGENT_ACTIVE.equals(agent.getStatus()) || agent.getCarrierBotId() == null) {
                markRun(row, RUN_SKIPPED, "智能体不可用或未绑定 Carrier Bot", manual);
                return;
            }

            List<String> skillIds = skillRegistry.parseSkillIdsJson(agent.getSkillIdsJson());
            List<ToolCallback> tools = skillRegistry.resolveToolCallbacks(skillIds);
            RuntimeAgent runtimeAgent = agentBuilder.build(agent, tools);
            AgentRunResult result = agentRuntime.run(AgentRunCommand.builder()
                    .modelRef(runtimeAgent.getModelRef())
                    .systemPrompt(runtimeAgent.getSystemPrompt())
                    .history(List.of())
                    .userMessage(row.getPrompt())
                    .tools(runtimeAgent.getTools())
                    .build());
            String text = result.getAssistantText();
            if (!StringUtils.hasText(text)) {
                text = "（自动化已执行，无文本输出）";
            }

            ImBotSendCommand send = new ImBotSendCommand();
            send.setBotCode(carrierBotCode(agent.getId()));
            send.setText("【自动化】" + row.getTitle() + "\n\n" + text.trim());
            send.setDedupeKey(manual
                    ? "auto-manual-" + row.getId() + "-" + System.currentTimeMillis()
                    : "auto-" + row.getId() + "-" + row.getNextRunAt());
            ImBotSendTarget target = new ImBotSendTarget();
            target.setScope(ImBotSendTarget.SCOPE_SINGLE);
            target.setTenantId(row.getTenantId());
            target.setUserId(row.getTargetUserId());
            send.setTarget(target);
            imBotApi.send(send);

            markRun(row, RUN_SUCCESS, null, manual);
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (!StringUtils.hasText(msg)) {
                msg = ex.getClass().getSimpleName();
            }
            if (msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            markRun(row, RUN_FAILED, msg, manual);
            if (manual) {
                throw new ServiceException(ErrorCodeConstants.AGENT_RUNTIME_FAILED.getCode(),
                        "自动化执行失败：" + msg);
            }
        } finally {
            if (previousTenant != null) {
                TenantContextHolder.set(previousTenant);
            } else {
                TenantContextHolder.clear();
            }
            SecurityContextHolder.getContext().setAuthentication(previousAuth);
        }
    }

    private void markRun(AgentAutomationDO row, String status, String error, boolean manual) {
        OffsetDateTime now = OffsetDateTime.now();
        row.setLastRunAt(now);
        row.setLastStatus(status);
        row.setLastError(error);
        if (!manual) {
            OffsetDateTime next = AutomationCronSupport.nextRunAfter(row.getCronExpr(), now);
            row.setNextRunAt(next);
        } else if (row.getNextRunAt() == null) {
            row.setNextRunAt(AutomationCronSupport.nextRunAfter(row.getCronExpr(), now));
        }
        automationMapper.updateById(row);
    }

    private void applyFields(AgentAutomationDO row, AgentAutomationSaveReqVO request, Long ownerUserId) {
        row.setTitle(request.getTitle().trim());
        row.setPrompt(request.getPrompt().trim());
        row.setAgentId(request.getAgentId());
        row.setCronExpr(request.getCronExpr().trim());
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : STATUS_ACTIVE;
        if (!STATUS_ACTIVE.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new ServiceException(ErrorCodeConstants.AGENT_AUTOMATION_INVALID);
        }
        row.setStatus(status);
        Long target = request.getTargetUserId() != null ? request.getTargetUserId() : ownerUserId;
        row.setTargetUserId(target);
        try {
            AutomationCronSupport.parse(row.getCronExpr());
        } catch (Exception ex) {
            throw new ServiceException(ErrorCodeConstants.AGENT_AUTOMATION_INVALID.getCode(),
                    "Cron 表达式无效：" + row.getCronExpr());
        }
    }

    private void validateSave(AgentAutomationSaveReqVO request) {
        if (request == null || !StringUtils.hasText(request.getTitle())
                || !StringUtils.hasText(request.getPrompt())
                || request.getAgentId() == null
                || !StringUtils.hasText(request.getCronExpr())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_AUTOMATION_INVALID);
        }
    }

    private AgentAutomationDO requireOwned(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        AgentAutomationDO row = automationMapper.selectOne(Wrappers.<AgentAutomationDO>lambdaQuery()
                .eq(AgentAutomationDO::getTenantId, tenantId)
                .eq(AgentAutomationDO::getId, id));
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_AUTOMATION_NOT_FOUND);
        }
        if (!Objects.equals(userId, row.getOwnerUserId())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_AUTOMATION_FORBIDDEN);
        }
        return row;
    }

    private AgentDefDO requireOwnedAgent(Long agentId, Long tenantId, Long userId) {
        AgentDefDO agent = agentDefMapper.selectOne(Wrappers.<AgentDefDO>lambdaQuery()
                .eq(AgentDefDO::getTenantId, tenantId)
                .eq(AgentDefDO::getId, agentId));
        if (agent == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_NOT_FOUND);
        }
        if (!Objects.equals(userId, agent.getOwnerUserId())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_FORBIDDEN);
        }
        return agent;
    }

    private static void requireCarrier(AgentDefDO agent) {
        if (agent.getCarrierBotId() == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_AUTOMATION_NO_CARRIER);
        }
    }

    static String carrierBotCode(Long agentId) {
        return "agent-carrier-" + agentId;
    }

    private Map<Long, AgentDefDO> loadAgents(Long tenantId, List<AgentAutomationDO> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = rows.stream().map(AgentAutomationDO::getAgentId).distinct().toList();
        return agentDefMapper.selectList(Wrappers.<AgentDefDO>lambdaQuery()
                        .eq(AgentDefDO::getTenantId, tenantId)
                        .in(AgentDefDO::getId, ids))
                .stream()
                .collect(Collectors.toMap(AgentDefDO::getId, Function.identity(), (a, b) -> a));
    }

    private AgentAutomationRespVO toVo(AgentAutomationDO row, AgentDefDO agent) {
        AgentAutomationRespVO vo = new AgentAutomationRespVO();
        vo.setId(row.getId());
        vo.setAgentId(row.getAgentId());
        vo.setAgentName(agent != null ? agent.getName() : null);
        vo.setTitle(row.getTitle());
        vo.setPrompt(row.getPrompt());
        vo.setCronExpr(row.getCronExpr());
        vo.setStatus(row.getStatus());
        vo.setTargetUserId(row.getTargetUserId());
        vo.setNextRunAt(row.getNextRunAt());
        vo.setLastRunAt(row.getLastRunAt());
        vo.setLastStatus(row.getLastStatus());
        vo.setLastError(row.getLastError());
        return vo;
    }
}
