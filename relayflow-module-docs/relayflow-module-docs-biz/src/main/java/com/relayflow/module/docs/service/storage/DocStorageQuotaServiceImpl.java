package com.relayflow.module.docs.service.storage;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.docs.controller.admin.vo.DocStorageQuotaRespVO;
import com.relayflow.module.docs.controller.admin.vo.DocStorageQuotaRespVO.DocStorageQuotaUserRuleRespVO;
import com.relayflow.module.docs.controller.admin.vo.DocStorageUsagePageReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocStorageUsagePageRespVO;
import com.relayflow.module.docs.controller.admin.vo.DocStorageUsagePageRespVO.DocStorageUsageItemRespVO;
import com.relayflow.module.docs.dal.dataobject.DocOwnerStorageUsageRow;
import com.relayflow.module.docs.dal.dataobject.DocStorageQuotaUserRuleDO;
import com.relayflow.module.docs.dal.dataobject.DocTenantSettingsDO;
import com.relayflow.module.docs.dal.mapper.DocObjectExtMapper;
import com.relayflow.module.docs.dal.mapper.DocStorageQuotaUserRuleMapper;
import com.relayflow.module.docs.dal.mapper.DocTenantSettingsMapper;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.MemberUserApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.MemberSearchRespDTO;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocStorageQuotaServiceImpl implements DocStorageQuotaService {

    private final DocTenantSettingsMapper tenantSettingsMapper;
    private final DocStorageQuotaUserRuleMapper userRuleMapper;
    private final DocObjectExtMapper docObjectExtMapper;
    private final MemberUserApi memberUserApi;
    private final TenantMemberApi tenantMemberApi;
    private final UserApi userApi;

    @Override
    public DocStorageUsagePageRespVO pageUsage(DocStorageUsagePageReqVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 20 : request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;
        long total = memberUserApi.countActiveMembers(tenantId, keyword);
        int offset = (pageNo - 1) * pageSize;
        List<MemberSearchRespDTO> members = total == 0
                ? List.of()
                : memberUserApi.pageActiveMembers(tenantId, keyword, offset, pageSize);

        Long defaultQuota = resolveDefaultQuota(tenantId);
        Map<Long, Long> userRules = loadUserRuleMap(tenantId);
        Map<Long, Long> usedByOwner = loadUsageMap(tenantId, members.stream().map(MemberSearchRespDTO::getUserId).toList());

        List<DocStorageUsageItemRespVO> list = new ArrayList<>(members.size());
        for (MemberSearchRespDTO member : members) {
            Long userId = member.getUserId();
            DocStorageUsageItemRespVO item = new DocStorageUsageItemRespVO();
            item.setUserId(userId);
            item.setDisplayName(member.getNickname());
            long used = usedByOwner.getOrDefault(userId, 0L);
            item.setUsedBytes(used);
            Long quota = resolveEffectiveQuota(defaultQuota, userRules.get(userId), userRules.containsKey(userId));
            item.setQuotaBytes(quota);
            item.setUsageRatio(quota == null || quota <= 0 ? null : (double) used / (double) quota);
            list.add(item);
        }

        DocStorageUsagePageRespVO resp = new DocStorageUsagePageRespVO();
        resp.setList(list);
        resp.setTotal(total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);
        resp.setPageNo(pageNo);
        resp.setPageSize(pageSize);
        return resp;
    }

    @Override
    public DocStorageQuotaRespVO getQuota() {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        DocStorageQuotaRespVO resp = new DocStorageQuotaRespVO();
        resp.setDefaultQuotaBytes(resolveDefaultQuota(tenantId));
        List<DocStorageQuotaUserRuleDO> rules = userRuleMapper.selectList(
                Wrappers.<DocStorageQuotaUserRuleDO>lambdaQuery()
                        .eq(DocStorageQuotaUserRuleDO::getTenantId, tenantId)
                        .orderByAsc(DocStorageQuotaUserRuleDO::getId));
        List<DocStorageQuotaUserRuleRespVO> ruleVos = new ArrayList<>(rules.size());
        for (DocStorageQuotaUserRuleDO rule : rules) {
            DocStorageQuotaUserRuleRespVO vo = new DocStorageQuotaUserRuleRespVO();
            vo.setUserId(rule.getUserId());
            vo.setQuotaBytes(rule.getQuotaBytes());
            vo.setDisplayName(resolveNickname(rule.getUserId()));
            ruleVos.add(vo);
        }
        resp.setUserRules(ruleVos);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocStorageQuotaRespVO updateQuota(DocStorageQuotaRespVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long operatorId = SecurityFrameworkUtils.requireLoginUserId();
        OffsetDateTime now = OffsetDateTime.now();

        if (request.getDefaultQuotaBytes() != null && request.getDefaultQuotaBytes() < 0) {
            throw new ServiceException(ErrorCodeConstants.DOC_FILE_OWNER_INVALID);
        }

        List<DocStorageQuotaUserRuleRespVO> incoming =
                request.getUserRules() == null ? List.of() : request.getUserRules();
        LinkedHashMap<Long, Long> nextRules = new LinkedHashMap<>();
        for (DocStorageQuotaUserRuleRespVO rule : incoming) {
            if (rule.getUserId() == null) {
                continue;
            }
            if (rule.getQuotaBytes() != null && rule.getQuotaBytes() < 0) {
                throw new ServiceException(ErrorCodeConstants.DOC_FILE_OWNER_INVALID);
            }
            nextRules.put(rule.getUserId(), rule.getQuotaBytes());
        }
        if (!nextRules.isEmpty()) {
            Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, nextRules.keySet());
            if (active.size() != nextRules.size()) {
                throw new ServiceException(ErrorCodeConstants.DOC_FILE_OWNER_INVALID);
            }
        }

        upsertDefaultQuota(tenantId, operatorId, now, request.getDefaultQuotaBytes());

        List<DocStorageQuotaUserRuleDO> existing = userRuleMapper.selectList(
                Wrappers.<DocStorageQuotaUserRuleDO>lambdaQuery()
                        .eq(DocStorageQuotaUserRuleDO::getTenantId, tenantId));
        Map<Long, DocStorageQuotaUserRuleDO> existingByUser = new HashMap<>();
        for (DocStorageQuotaUserRuleDO row : existing) {
            existingByUser.put(row.getUserId(), row);
        }

        Set<Long> keep = new HashSet<>(nextRules.keySet());
        for (DocStorageQuotaUserRuleDO row : existing) {
            if (!keep.contains(row.getUserId())) {
                userRuleMapper.physicalDeleteById(tenantId, row.getId());
            }
        }
        for (Map.Entry<Long, Long> entry : nextRules.entrySet()) {
            DocStorageQuotaUserRuleDO row = existingByUser.get(entry.getKey());
            if (row == null) {
                row = new DocStorageQuotaUserRuleDO();
                row.setTenantId(tenantId);
                row.setUserId(entry.getKey());
                row.setQuotaBytes(entry.getValue());
                row.setCreator(operatorId);
                row.setCreateTime(now);
                row.setUpdater(operatorId);
                row.setUpdateTime(now);
                userRuleMapper.insert(row);
            } else {
                userRuleMapper.update(
                        null,
                        Wrappers.<DocStorageQuotaUserRuleDO>lambdaUpdate()
                                .eq(DocStorageQuotaUserRuleDO::getId, row.getId())
                                .set(DocStorageQuotaUserRuleDO::getQuotaBytes, entry.getValue())
                                .set(DocStorageQuotaUserRuleDO::getUpdater, operatorId)
                                .set(DocStorageQuotaUserRuleDO::getUpdateTime, now));
            }
        }
        return getQuota();
    }

    @Override
    public Long resolveEffectiveQuotaBytes(Long tenantId, Long userId) {
        Long defaultQuota = resolveDefaultQuota(tenantId);
        DocStorageQuotaUserRuleDO rule = userRuleMapper.selectOne(
                Wrappers.<DocStorageQuotaUserRuleDO>lambdaQuery()
                        .eq(DocStorageQuotaUserRuleDO::getTenantId, tenantId)
                        .eq(DocStorageQuotaUserRuleDO::getUserId, userId)
                        .last("LIMIT 1"));
        return resolveEffectiveQuota(defaultQuota, rule == null ? null : rule.getQuotaBytes(), rule != null);
    }

    @Override
    public long sumUsedBytes(Long tenantId, Long userId) {
        Long sum = docObjectExtMapper.sumActiveFileUsageForOwner(tenantId, userId);
        return sum == null ? 0L : sum;
    }

    @Override
    public void requireWithinQuota(Long tenantId, Long userId, long additionalBytes) {
        Long quota = resolveEffectiveQuotaBytes(tenantId, userId);
        if (quota == null) {
            return;
        }
        long used = sumUsedBytes(tenantId, userId);
        if (used + Math.max(0L, additionalBytes) > quota) {
            throw new ServiceException(ErrorCodeConstants.DOC_STORAGE_QUOTA_EXCEEDED);
        }
    }

    private void upsertDefaultQuota(Long tenantId, Long operatorId, OffsetDateTime now, Long defaultQuotaBytes) {
        DocTenantSettingsDO row = tenantSettingsMapper.selectById(tenantId);
        if (row == null) {
            row = new DocTenantSettingsDO();
            row.setTenantId(tenantId);
            row.setMemberShareEnabled(true);
            row.setMarkdownExportEnabled(true);
            row.setDriveUploadEnabled(true);
            row.setStorageQuotaDefaultBytes(defaultQuotaBytes);
            row.setCreator(operatorId);
            row.setCreateTime(now);
            row.setUpdater(operatorId);
            row.setUpdateTime(now);
            tenantSettingsMapper.insert(row);
            return;
        }
        tenantSettingsMapper.update(
                null,
                Wrappers.<DocTenantSettingsDO>lambdaUpdate()
                        .eq(DocTenantSettingsDO::getTenantId, tenantId)
                        .set(DocTenantSettingsDO::getStorageQuotaDefaultBytes, defaultQuotaBytes)
                        .set(DocTenantSettingsDO::getUpdater, operatorId)
                        .set(DocTenantSettingsDO::getUpdateTime, now));
    }

    private Long resolveDefaultQuota(Long tenantId) {
        DocTenantSettingsDO row = tenantSettingsMapper.selectById(tenantId);
        return row == null ? null : row.getStorageQuotaDefaultBytes();
    }

    /**
     * @param hasUserRule whether a user rule row exists (quota may still be null = unlimited)
     */
    private static Long resolveEffectiveQuota(Long defaultQuota, Long userRuleQuota, boolean hasUserRule) {
        if (hasUserRule) {
            return userRuleQuota;
        }
        return defaultQuota;
    }

    private Map<Long, Long> loadUserRuleMap(Long tenantId) {
        Map<Long, Long> map = new HashMap<>();
        for (DocStorageQuotaUserRuleDO rule : userRuleMapper.selectList(
                Wrappers.<DocStorageQuotaUserRuleDO>lambdaQuery()
                        .eq(DocStorageQuotaUserRuleDO::getTenantId, tenantId))) {
            map.put(rule.getUserId(), rule.getQuotaBytes());
        }
        return map;
    }

    private Map<Long, Long> loadUsageMap(Long tenantId, List<Long> ownerUserIds) {
        Map<Long, Long> map = new HashMap<>();
        if (ownerUserIds == null || ownerUserIds.isEmpty()) {
            return map;
        }
        List<Long> ids = ownerUserIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return map;
        }
        for (DocOwnerStorageUsageRow row : docObjectExtMapper.sumActiveFileUsageByOwners(tenantId, ids)) {
            if (row.getOwnerUserId() != null) {
                map.put(row.getOwnerUserId(), row.getUsedBytes() == null ? 0L : row.getUsedBytes());
            }
        }
        return map;
    }

    private String resolveNickname(Long userId) {
        try {
            UserBasicDTO basic = userApi.getUserBasic(userId);
            if (basic != null && StringUtils.hasText(basic.getNickname())) {
                return basic.getNickname();
            }
            if (basic != null && StringUtils.hasText(basic.getUsername())) {
                return basic.getUsername();
            }
        } catch (Exception ex) {
            log.warn("Resolve nickname failed (best-effort): userId={}", userId, ex);
        }
        return String.valueOf(userId);
    }
}
