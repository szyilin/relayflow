package com.relayflow.module.docs.service.storage;

import com.relayflow.module.docs.controller.admin.vo.DocStorageQuotaRespVO;
import com.relayflow.module.docs.controller.admin.vo.DocStorageUsagePageReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocStorageUsagePageRespVO;

public interface DocStorageQuotaService {

    DocStorageUsagePageRespVO pageUsage(DocStorageUsagePageReqVO request);

    DocStorageQuotaRespVO getQuota();

    DocStorageQuotaRespVO updateQuota(DocStorageQuotaRespVO request);

    /**
     * Effective quota for user: user rule &gt; tenant default &gt; unlimited ({@code null}).
     */
    Long resolveEffectiveQuotaBytes(Long tenantId, Long userId);

    long sumUsedBytes(Long tenantId, Long userId);

    void requireWithinQuota(Long tenantId, Long userId, long additionalBytes);
}
