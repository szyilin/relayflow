package com.relayflow.module.agent.service.llm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.encrypt.AesGcmEncryptor;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.controller.admin.llm.vo.LlmConnectionRespVO;
import com.relayflow.module.agent.controller.admin.llm.vo.LlmConnectionSaveReqVO;
import com.relayflow.module.agent.controller.admin.llm.vo.LlmConnectionTestReqVO;
import com.relayflow.module.agent.controller.app.llm.vo.LlmEnabledModelRespVO;
import com.relayflow.module.agent.dal.dataobject.AgentLlmConnectionDO;
import com.relayflow.module.agent.dal.mapper.AgentLlmConnectionMapper;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmConnectionServiceImpl implements LlmConnectionService {

    private static final String PROVIDER_OPENAI_COMPATIBLE = "openai_compatible";

    private final AgentLlmConnectionMapper connectionMapper;
    private final AesGcmEncryptor encryptor;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public List<LlmConnectionRespVO> list() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return connectionMapper.selectList(Wrappers.<AgentLlmConnectionDO>lambdaQuery()
                        .eq(AgentLlmConnectionDO::getTenantId, tenantId)
                        .orderByAsc(AgentLlmConnectionDO::getCode))
                .stream()
                .map(this::toAdminVo)
                .toList();
    }

    @Override
    public LlmConnectionRespVO get(Long id) {
        return toAdminVo(requireConnection(id));
    }

    @Override
    @Transactional
    public Long create(LlmConnectionSaveReqVO request) {
        validateProvider(request.getProviderType());
        if (!StringUtils.hasText(request.getApiKey())) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_INVALID);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        assertCodeUnique(tenantId, request.getCode().trim(), null);

        AgentLlmConnectionDO row = new AgentLlmConnectionDO();
        row.setTenantId(tenantId);
        applySaveFields(row, request, true);
        connectionMapper.insert(row);
        return row.getId();
    }

    @Override
    @Transactional
    public void update(LlmConnectionSaveReqVO request) {
        if (request.getId() == null) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_INVALID);
        }
        validateProvider(request.getProviderType());
        AgentLlmConnectionDO existing = requireConnection(request.getId());
        assertCodeUnique(existing.getTenantId(), request.getCode().trim(), existing.getId());
        applySaveFields(existing, request, false);
        connectionMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AgentLlmConnectionDO existing = requireConnection(id);
        connectionMapper.deleteById(existing.getId());
    }

    @Override
    public void testConnection(LlmConnectionTestReqVO request) {
        String baseUrl;
        String apiKey;
        String providerType;

        if (request.getId() != null) {
            AgentLlmConnectionDO row = requireConnection(request.getId());
            baseUrl = StringUtils.hasText(request.getBaseUrl()) ? request.getBaseUrl() : row.getBaseUrl();
            providerType = StringUtils.hasText(request.getProviderType()) ? request.getProviderType() : row.getProviderType();
            if (StringUtils.hasText(request.getApiKey())) {
                apiKey = request.getApiKey().trim();
            } else if (StringUtils.hasText(row.getApiKeyEnc())) {
                apiKey = encryptor.decrypt(row.getApiKeyEnc());
            } else {
                throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_INVALID);
            }
        } else {
            baseUrl = request.getBaseUrl();
            apiKey = request.getApiKey();
            providerType = request.getProviderType();
            if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey)) {
                throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_INVALID);
            }
        }

        validateProvider(providerType);
        probeModelsEndpoint(normalizeBaseUrl(baseUrl.trim()), apiKey.trim());
    }

    @Override
    public List<LlmEnabledModelRespVO> listEnabledModels() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return connectionMapper.selectList(Wrappers.<AgentLlmConnectionDO>lambdaQuery()
                        .eq(AgentLlmConnectionDO::getTenantId, tenantId)
                        .eq(AgentLlmConnectionDO::getEnabled, 1)
                        .orderByAsc(AgentLlmConnectionDO::getCode))
                .stream()
                .map(row -> {
                    LlmEnabledModelRespVO vo = new LlmEnabledModelRespVO();
                    vo.setCode(row.getCode());
                    vo.setName(row.getName());
                    vo.setModelName(row.getModelName());
                    vo.setProviderType(row.getProviderType());
                    return vo;
                })
                .toList();
    }

    private void applySaveFields(AgentLlmConnectionDO row, LlmConnectionSaveReqVO request, boolean creating) {
        row.setCode(request.getCode().trim());
        row.setName(request.getName().trim());
        row.setProviderType(normalizeProvider(request.getProviderType()));
        row.setBaseUrl(normalizeBaseUrl(request.getBaseUrl().trim()));
        row.setModelName(request.getModelName().trim());
        row.setEnabled(Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0);
        if (StringUtils.hasText(request.getApiKey())) {
            row.setApiKeyEnc(encryptor.encrypt(request.getApiKey().trim()));
        } else if (creating) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_INVALID);
        }
    }

    private AgentLlmConnectionDO requireConnection(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        AgentLlmConnectionDO row = connectionMapper.selectOne(Wrappers.<AgentLlmConnectionDO>lambdaQuery()
                .eq(AgentLlmConnectionDO::getTenantId, tenantId)
                .eq(AgentLlmConnectionDO::getId, id));
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_NOT_FOUND);
        }
        return row;
    }

    private void assertCodeUnique(Long tenantId, String code, Long excludeId) {
        Long count = connectionMapper.selectCount(Wrappers.<AgentLlmConnectionDO>lambdaQuery()
                .eq(AgentLlmConnectionDO::getTenantId, tenantId)
                .eq(AgentLlmConnectionDO::getCode, code)
                .ne(excludeId != null, AgentLlmConnectionDO::getId, excludeId));
        if (count != null && count > 0) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_CODE_DUPLICATE);
        }
    }

    private void validateProvider(String providerType) {
        String normalized = normalizeProvider(providerType);
        if (!PROVIDER_OPENAI_COMPATIBLE.equals(normalized)) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_UNSUPPORTED);
        }
    }

    private String normalizeProvider(String providerType) {
        if (!StringUtils.hasText(providerType)) {
            return PROVIDER_OPENAI_COMPATIBLE;
        }
        return providerType.trim();
    }

    static String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private void probeModelsEndpoint(String baseUrl, String apiKey) {
        String modelsUrl = baseUrl.endsWith("/v1")
                ? baseUrl + "/models"
                : baseUrl + "/v1/models";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_TEST_FAILED.getCode(),
                        "模型连通性测试失败：HTTP " + status);
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ErrorCodeConstants.LLM_CONNECTION_TEST_FAILED.getCode(),
                    "模型连通性测试失败：" + ex.getMessage());
        }
    }

    private LlmConnectionRespVO toAdminVo(AgentLlmConnectionDO row) {
        LlmConnectionRespVO vo = new LlmConnectionRespVO();
        vo.setId(row.getId());
        vo.setCode(row.getCode());
        vo.setName(row.getName());
        vo.setProviderType(row.getProviderType());
        vo.setBaseUrl(row.getBaseUrl());
        vo.setModelName(row.getModelName());
        vo.setEnabled(row.getEnabled() != null && row.getEnabled() == 1);
        vo.setApiKeyConfigured(StringUtils.hasText(row.getApiKeyEnc()));
        return vo;
    }
}
