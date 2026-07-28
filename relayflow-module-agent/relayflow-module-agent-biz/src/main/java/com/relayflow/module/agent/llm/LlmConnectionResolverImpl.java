package com.relayflow.module.agent.llm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.encrypt.AesGcmEncryptor;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.dal.dataobject.AgentLlmConnectionDO;
import com.relayflow.module.agent.dal.mapper.AgentLlmConnectionMapper;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class LlmConnectionResolverImpl implements LlmConnectionResolver {

    private final AgentLlmConnectionMapper connectionMapper;
    private final AesGcmEncryptor encryptor;

    @Override
    public ResolvedLlmConnection requireEnabled(String modelRef) {
        if (!StringUtils.hasText(modelRef)) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_INVALID);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        AgentLlmConnectionDO row = connectionMapper.selectOne(Wrappers.<AgentLlmConnectionDO>lambdaQuery()
                .eq(AgentLlmConnectionDO::getTenantId, tenantId)
                .eq(AgentLlmConnectionDO::getCode, modelRef.trim()));
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_NOT_FOUND);
        }
        if (row.getEnabled() == null || row.getEnabled() != 1) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_DISABLED);
        }
        if (!StringUtils.hasText(row.getApiKeyEnc())) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_INVALID);
        }
        return ResolvedLlmConnection.builder()
                .code(row.getCode())
                .name(row.getName())
                .providerType(row.getProviderType())
                .baseUrl(row.getBaseUrl())
                .apiKey(encryptor.decrypt(row.getApiKeyEnc()))
                .modelName(row.getModelName())
                .build();
    }
}
