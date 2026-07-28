package com.relayflow.module.im.service.productsettings;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.im.controller.admin.productsettings.vo.GroupSizeGroupRuleVO;
import com.relayflow.module.im.controller.admin.productsettings.vo.GroupSizeUserRuleVO;
import com.relayflow.module.im.controller.admin.productsettings.vo.ImGroupSearchItemRespVO;
import com.relayflow.module.im.controller.admin.productsettings.vo.ImProductSettingsRespVO;
import com.relayflow.module.im.controller.app.vo.ImEffectiveSettingsRespVO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImGroupDO;
import com.relayflow.module.im.dal.dataobject.ImGroupSizeGroupRuleDO;
import com.relayflow.module.im.dal.dataobject.ImGroupSizeUserRuleDO;
import com.relayflow.module.im.dal.dataobject.ImTenantSettingsDO;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImGroupMapper;
import com.relayflow.module.im.dal.mapper.ImGroupSizeGroupRuleMapper;
import com.relayflow.module.im.dal.mapper.ImGroupSizeUserRuleMapper;
import com.relayflow.module.im.dal.mapper.ImTenantSettingsMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.system.api.dept.DeptApi;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImProductSettingsServiceImpl implements ImProductSettingsService {

    static final int DEFAULT_GROUP_MEMBER_LIMIT = 50;
    static final boolean DEFAULT_ALLOW_PUBLIC_GROUP = true;
    static final String DEFAULT_DEPT_GROUP_LEAVE_POLICY = "ALLOW";
    static final String DEFAULT_MEMBER_EXPORT_POLICY = "DENY_ALL";
    static final boolean DEFAULT_RECALL_ENABLED = true;
    static final int DEFAULT_RECALL_WITHIN_HOURS = 24;
    static final boolean DEFAULT_EDIT_ENABLED = true;
    static final int DEFAULT_EDIT_WITHIN_HOURS = 24;

    private static final Set<String> SUBJECT_TYPES = Set.of("DEPT", "USER");
    private static final Set<String> LEAVE_POLICIES = Set.of("ALLOW", "DENY");
    private static final Set<String> EXPORT_POLICIES = Set.of(
            "ALLOW_OWNER_ADMIN", "DENY_ALL", "ALLOW_DESIGNATED");
    private static final Set<Integer> WINDOW_HOURS = Set.of(2, 24, 168);

    private final ImTenantSettingsMapper tenantSettingsMapper;
    private final ImGroupSizeUserRuleMapper userRuleMapper;
    private final ImGroupSizeGroupRuleMapper groupRuleMapper;
    private final ImGroupMapper groupMapper;
    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final UserApi userApi;
    private final DeptApi deptApi;
    private final TenantMemberApi tenantMemberApi;
    private final ObjectMapper objectMapper;

    @Override
    public ImProductSettingsRespVO getSettings() {
        return getSettings(TenantContextHolder.getRequiredTenantId());
    }

    @Override
    public ImProductSettingsRespVO getSettings(Long tenantId) {
        ImTenantSettingsDO settings = findSettings(tenantId);
        List<ImGroupSizeUserRuleDO> userRules = listUserRules(tenantId);
        List<ImGroupSizeGroupRuleDO> groupRules = listGroupRules(tenantId);
        return toResp(tenantId, settings, userRules, groupRules);
    }

    @Override
    @Transactional
    public ImProductSettingsRespVO updateSettings(ImProductSettingsRespVO request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        validate(request, tenantId);

        ImTenantSettingsDO settings = findSettings(tenantId);
        if (settings == null) {
            settings = new ImTenantSettingsDO();
            settings.setTenantId(tenantId);
            applySettings(settings, request);
            tenantSettingsMapper.insert(settings);
        } else {
            applySettings(settings, request);
            tenantSettingsMapper.updateById(settings);
        }

        userRuleMapper.delete(Wrappers.<ImGroupSizeUserRuleDO>lambdaQuery()
                .eq(ImGroupSizeUserRuleDO::getTenantId, tenantId));
        groupRuleMapper.delete(Wrappers.<ImGroupSizeGroupRuleDO>lambdaQuery()
                .eq(ImGroupSizeGroupRuleDO::getTenantId, tenantId));

        int sortOrder = 0;
        for (GroupSizeUserRuleVO rule : nullSafeUserRules(request)) {
            ImGroupSizeUserRuleDO row = new ImGroupSizeUserRuleDO();
            row.setTenantId(tenantId);
            row.setSubjectType(rule.getSubjectType());
            row.setSubjectId(parsePositiveLong(rule.getSubjectId(), ErrorCodeConstants.PRODUCT_SETTINGS_INVALID));
            row.setMemberLimit(rule.getMemberLimit());
            row.setSortOrder(sortOrder++);
            userRuleMapper.insert(row);
        }

        sortOrder = 0;
        for (GroupSizeGroupRuleVO rule : nullSafeGroupRules(request)) {
            ImGroupSizeGroupRuleDO row = new ImGroupSizeGroupRuleDO();
            row.setTenantId(tenantId);
            row.setConversationId(parsePositiveLong(rule.getConversationId(), ErrorCodeConstants.PRODUCT_SETTINGS_INVALID));
            row.setMemberLimit(rule.getMemberLimit());
            row.setSortOrder(sortOrder++);
            groupRuleMapper.insert(row);
        }

        return getSettings(tenantId);
    }

    @Override
    public List<ImGroupSearchItemRespVO> searchGroups(String keyword) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        List<ImGroupDO> groups = groupMapper.selectList(
                Wrappers.<ImGroupDO>lambdaQuery()
                        .eq(ImGroupDO::getTenantId, tenantId)
                        .like(trimmedKeyword != null, ImGroupDO::getName, trimmedKeyword)
                        .orderByAsc(ImGroupDO::getName)
                        .last("LIMIT 50"));

        if (groups.isEmpty()) {
            return List.of();
        }

        Set<Long> conversationIds = groups.stream()
                .map(ImGroupDO::getConversationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ImConversationDO> conversations = conversationMapper.selectBatchIds(conversationIds).stream()
                .filter(conversation -> ImConversationType.GROUP.equals(conversation.getType()))
                .collect(Collectors.toMap(ImConversationDO::getId, conversation -> conversation));

        List<ImGroupSearchItemRespVO> items = new ArrayList<>();
        for (ImGroupDO group : groups) {
            if (!conversations.containsKey(group.getConversationId())) {
                continue;
            }
            ImGroupSearchItemRespVO item = new ImGroupSearchItemRespVO();
            item.setConversationId(String.valueOf(group.getConversationId()));
            item.setName(group.getName());
            item.setMemberCount(countUserMembers(tenantId, group.getConversationId()));
            items.add(item);
        }
        return items;
    }

    @Override
    public ImEffectiveSettingsRespVO getEffectiveSettings() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ImTenantSettingsDO settings = findSettings(tenantId);
        ImEffectiveSettingsRespVO resp = new ImEffectiveSettingsRespVO();
        resp.setGroupMemberLimitDefault(resolveDefaultLimit(settings));
        resp.setAllowPublicGroup(resolveAllowPublicGroup(settings));
        resp.setDeptGroupLeavePolicy(resolveLeavePolicy(settings));
        resp.setMemberExportPolicy(resolveExportPolicy(settings));
        resp.setRecallEnabled(resolveRecallEnabled(settings));
        resp.setRecallWithinHours(resolveRecallWithinHours(settings));
        resp.setEditEnabled(resolveEditEnabled(settings));
        resp.setEditWithinHours(resolveEditWithinHours(settings));
        return resp;
    }

    @Override
    public int resolveMemberLimit(Long tenantId, Long conversationId, Long userId) {
        if (conversationId != null) {
            ImGroupSizeGroupRuleDO groupRule = groupRuleMapper.selectOne(
                    Wrappers.<ImGroupSizeGroupRuleDO>lambdaQuery()
                            .eq(ImGroupSizeGroupRuleDO::getTenantId, tenantId)
                            .eq(ImGroupSizeGroupRuleDO::getConversationId, conversationId)
                            .last("LIMIT 1"));
            if (groupRule != null && groupRule.getMemberLimit() != null) {
                return groupRule.getMemberLimit();
            }
        }

        List<ImGroupSizeUserRuleDO> userRules = listUserRules(tenantId);
        if (!userRules.isEmpty() && userId != null) {
            Integer userRuleLimit = null;
            Integer deptRuleLimit = null;
            Set<Long> userDeptIds = new HashSet<>(userApi.listUserDeptIds(userId));

            for (ImGroupSizeUserRuleDO rule : userRules) {
                if ("USER".equals(rule.getSubjectType()) && Objects.equals(rule.getSubjectId(), userId)) {
                    userRuleLimit = rule.getMemberLimit();
                    break;
                }
            }

            if (userRuleLimit == null) {
                for (ImGroupSizeUserRuleDO rule : userRules) {
                    if ("DEPT".equals(rule.getSubjectType())
                            && rule.getSubjectId() != null
                            && userDeptIds.contains(rule.getSubjectId())
                            && rule.getMemberLimit() != null) {
                        deptRuleLimit = deptRuleLimit == null
                                ? rule.getMemberLimit()
                                : Math.max(deptRuleLimit, rule.getMemberLimit());
                    }
                }
            }

            if (userRuleLimit != null) {
                return userRuleLimit;
            }
            if (deptRuleLimit != null) {
                return deptRuleLimit;
            }
        }

        return resolveDefaultLimit(findSettings(tenantId));
    }

    private void validate(ImProductSettingsRespVO request, Long tenantId) {
        if (request == null
                || request.getGroupMemberLimitDefault() == null
                || request.getGroupMemberLimitDefault() < 2
                || request.getAllowPublicGroup() == null
                || request.getRecallEnabled() == null
                || request.getEditEnabled() == null) {
            throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
        }

        String leavePolicy = normalize(request.getDeptGroupLeavePolicy());
        String exportPolicy = normalize(request.getMemberExportPolicy());
        if (!LEAVE_POLICIES.contains(leavePolicy) || !EXPORT_POLICIES.contains(exportPolicy)) {
            throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
        }

        if (request.getRecallWithinHours() == null
                || request.getEditWithinHours() == null
                || !WINDOW_HOURS.contains(request.getRecallWithinHours())
                || !WINDOW_HOURS.contains(request.getEditWithinHours())) {
            throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
        }

        List<String> designatedUserIds = request.getMemberExportDesignatedUserIds();
        if (designatedUserIds == null) {
            throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
        }
        if ("ALLOW_DESIGNATED".equals(exportPolicy) && designatedUserIds.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
        }

        Set<String> seenUserSubjects = new HashSet<>();
        for (GroupSizeUserRuleVO rule : nullSafeUserRules(request)) {
            String subjectType = normalize(rule.getSubjectType());
            if (!SUBJECT_TYPES.contains(subjectType)
                    || rule.getMemberLimit() == null
                    || rule.getMemberLimit() < 2
                    || !StringUtils.hasText(rule.getSubjectId())) {
                throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            }
            String dedupeKey = subjectType + ":" + rule.getSubjectId().trim();
            if (!seenUserSubjects.add(dedupeKey)) {
                throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            }

            Long subjectId = parsePositiveLong(rule.getSubjectId(), ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            if ("USER".equals(subjectType)) {
                try {
                    userApi.getUserBasic(subjectId);
                } catch (RuntimeException ex) {
                    throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
                }
                Set<Long> activeMembers = tenantMemberApi.filterActiveMemberUserIds(tenantId, List.of(subjectId));
                if (!activeMembers.contains(subjectId)) {
                    throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
                }
            } else if (!deptApi.existsInTenant(tenantId, subjectId)) {
                throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            }
            rule.setSubjectType(subjectType);
        }

        Set<Long> seenConversationIds = new HashSet<>();
        for (GroupSizeGroupRuleVO rule : nullSafeGroupRules(request)) {
            if (rule.getMemberLimit() == null
                    || rule.getMemberLimit() < 2
                    || !StringUtils.hasText(rule.getConversationId())) {
                throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            }
            Long conversationId = parsePositiveLong(rule.getConversationId(), ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            if (!seenConversationIds.add(conversationId)) {
                throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            }
            requireTenantGroupConversation(tenantId, conversationId);
        }

        for (String userIdText : designatedUserIds) {
            if (!StringUtils.hasText(userIdText)) {
                throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            }
            Long userId = parsePositiveLong(userIdText, ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            Set<Long> activeMembers = tenantMemberApi.filterActiveMemberUserIds(tenantId, List.of(userId));
            if (!activeMembers.contains(userId)) {
                throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
            }
        }

        request.setDeptGroupLeavePolicy(leavePolicy);
        request.setMemberExportPolicy(exportPolicy);
    }

    private void requireTenantGroupConversation(Long tenantId, Long conversationId) {
        ImConversationDO conversation = conversationMapper.selectOne(
                Wrappers.<ImConversationDO>lambdaQuery()
                        .eq(ImConversationDO::getTenantId, tenantId)
                        .eq(ImConversationDO::getId, conversationId)
                        .last("LIMIT 1"));
        if (conversation == null || !ImConversationType.GROUP.equals(conversation.getType())) {
            throw new ServiceException(ErrorCodeConstants.PRODUCT_SETTINGS_INVALID);
        }
    }

    private ImProductSettingsRespVO toResp(Long tenantId,
                                           ImTenantSettingsDO settings,
                                           List<ImGroupSizeUserRuleDO> userRules,
                                           List<ImGroupSizeGroupRuleDO> groupRules) {
        ImProductSettingsRespVO resp = new ImProductSettingsRespVO();
        resp.setGroupMemberLimitDefault(resolveDefaultLimit(settings));
        resp.setAllowPublicGroup(resolveAllowPublicGroup(settings));
        resp.setDeptGroupLeavePolicy(resolveLeavePolicy(settings));
        resp.setMemberExportPolicy(resolveExportPolicy(settings));
        resp.setMemberExportDesignatedUserIds(readDesignatedUserIds(settings));
        resp.setRecallEnabled(resolveRecallEnabled(settings));
        resp.setRecallWithinHours(resolveRecallWithinHours(settings));
        resp.setEditEnabled(resolveEditEnabled(settings));
        resp.setEditWithinHours(resolveEditWithinHours(settings));
        resp.setUserRules(toUserRuleVos(tenantId, userRules));
        resp.setGroupRules(toGroupRuleVos(groupRules));
        return resp;
    }

    private List<GroupSizeUserRuleVO> toUserRuleVos(Long tenantId, List<ImGroupSizeUserRuleDO> userRules) {
        List<GroupSizeUserRuleVO> items = new ArrayList<>();
        for (ImGroupSizeUserRuleDO rule : userRules) {
            GroupSizeUserRuleVO item = new GroupSizeUserRuleVO();
            item.setSubjectType(rule.getSubjectType());
            item.setSubjectId(String.valueOf(rule.getSubjectId()));
            item.setMemberLimit(rule.getMemberLimit());
            item.setSubjectLabel(resolveUserRuleLabel(tenantId, rule));
            items.add(item);
        }
        return items;
    }

    private String resolveUserRuleLabel(Long tenantId, ImGroupSizeUserRuleDO rule) {
        if ("USER".equals(rule.getSubjectType())) {
            try {
                UserBasicDTO user = userApi.getUserBasic(rule.getSubjectId());
                return user.getNickname();
            } catch (RuntimeException ex) {
                return String.valueOf(rule.getSubjectId());
            }
        }
        String deptName = deptApi.getDeptName(tenantId, rule.getSubjectId());
        return deptName != null ? deptName : String.valueOf(rule.getSubjectId());
    }

    private List<GroupSizeGroupRuleVO> toGroupRuleVos(List<ImGroupSizeGroupRuleDO> groupRules) {
        if (groupRules.isEmpty()) {
            return List.of();
        }

        Set<Long> conversationIds = groupRules.stream()
                .map(ImGroupSizeGroupRuleDO::getConversationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ImGroupDO> groupsByConversationId = groupMapper.selectList(
                        Wrappers.<ImGroupDO>lambdaQuery().in(ImGroupDO::getConversationId, conversationIds))
                .stream()
                .collect(Collectors.toMap(ImGroupDO::getConversationId, group -> group, (left, right) -> left));

        List<GroupSizeGroupRuleVO> items = new ArrayList<>();
        for (ImGroupSizeGroupRuleDO rule : groupRules) {
            GroupSizeGroupRuleVO item = new GroupSizeGroupRuleVO();
            item.setConversationId(String.valueOf(rule.getConversationId()));
            item.setMemberLimit(rule.getMemberLimit());
            ImGroupDO group = groupsByConversationId.get(rule.getConversationId());
            item.setGroupName(group != null ? group.getName() : String.valueOf(rule.getConversationId()));
            items.add(item);
        }
        return items;
    }

    private void applySettings(ImTenantSettingsDO settings, ImProductSettingsRespVO request) {
        settings.setGroupMemberLimitDefault(request.getGroupMemberLimitDefault());
        settings.setAllowPublicGroup(request.getAllowPublicGroup());
        settings.setDeptGroupLeavePolicy(request.getDeptGroupLeavePolicy());
        settings.setMemberExportPolicy(request.getMemberExportPolicy());
        settings.setMemberExportDesignatedJson(writeDesignatedUserIds(request.getMemberExportDesignatedUserIds()));
        settings.setRecallEnabled(request.getRecallEnabled());
        settings.setRecallWithinHours(request.getRecallWithinHours());
        settings.setEditEnabled(request.getEditEnabled());
        settings.setEditWithinHours(request.getEditWithinHours());
    }

    private ImTenantSettingsDO findSettings(Long tenantId) {
        return tenantSettingsMapper.selectById(tenantId);
    }

    private List<ImGroupSizeUserRuleDO> listUserRules(Long tenantId) {
        return userRuleMapper.selectList(Wrappers.<ImGroupSizeUserRuleDO>lambdaQuery()
                .eq(ImGroupSizeUserRuleDO::getTenantId, tenantId)
                .orderByAsc(ImGroupSizeUserRuleDO::getSortOrder)
                .orderByAsc(ImGroupSizeUserRuleDO::getId));
    }

    private List<ImGroupSizeGroupRuleDO> listGroupRules(Long tenantId) {
        return groupRuleMapper.selectList(Wrappers.<ImGroupSizeGroupRuleDO>lambdaQuery()
                .eq(ImGroupSizeGroupRuleDO::getTenantId, tenantId)
                .orderByAsc(ImGroupSizeGroupRuleDO::getSortOrder)
                .orderByAsc(ImGroupSizeGroupRuleDO::getId));
    }

    private int countUserMembers(Long tenantId, Long conversationId) {
        Long count = conversationMemberMapper.selectCount(
                Wrappers.<com.relayflow.module.im.dal.dataobject.ImConversationMemberDO>lambdaQuery()
                        .eq(com.relayflow.module.im.dal.dataobject.ImConversationMemberDO::getTenantId, tenantId)
                        .eq(com.relayflow.module.im.dal.dataobject.ImConversationMemberDO::getConversationId, conversationId)
                        .eq(com.relayflow.module.im.dal.dataobject.ImConversationMemberDO::getSubjectType,
                                ImMemberSubjectType.USER));
        return count != null ? count.intValue() : 0;
    }

    private int resolveDefaultLimit(ImTenantSettingsDO settings) {
        return settings != null && settings.getGroupMemberLimitDefault() != null
                ? settings.getGroupMemberLimitDefault()
                : DEFAULT_GROUP_MEMBER_LIMIT;
    }

    private boolean resolveAllowPublicGroup(ImTenantSettingsDO settings) {
        return settings == null || settings.getAllowPublicGroup() == null
                ? DEFAULT_ALLOW_PUBLIC_GROUP
                : settings.getAllowPublicGroup();
    }

    private String resolveLeavePolicy(ImTenantSettingsDO settings) {
        return settings != null && StringUtils.hasText(settings.getDeptGroupLeavePolicy())
                ? settings.getDeptGroupLeavePolicy()
                : DEFAULT_DEPT_GROUP_LEAVE_POLICY;
    }

    private String resolveExportPolicy(ImTenantSettingsDO settings) {
        return settings != null && StringUtils.hasText(settings.getMemberExportPolicy())
                ? settings.getMemberExportPolicy()
                : DEFAULT_MEMBER_EXPORT_POLICY;
    }

    private boolean resolveRecallEnabled(ImTenantSettingsDO settings) {
        return settings == null || settings.getRecallEnabled() == null
                ? DEFAULT_RECALL_ENABLED
                : settings.getRecallEnabled();
    }

    private int resolveRecallWithinHours(ImTenantSettingsDO settings) {
        return settings != null && settings.getRecallWithinHours() != null
                ? settings.getRecallWithinHours()
                : DEFAULT_RECALL_WITHIN_HOURS;
    }

    private boolean resolveEditEnabled(ImTenantSettingsDO settings) {
        return settings == null || settings.getEditEnabled() == null
                ? DEFAULT_EDIT_ENABLED
                : settings.getEditEnabled();
    }

    private int resolveEditWithinHours(ImTenantSettingsDO settings) {
        return settings != null && settings.getEditWithinHours() != null
                ? settings.getEditWithinHours()
                : DEFAULT_EDIT_WITHIN_HOURS;
    }

    private List<String> readDesignatedUserIds(ImTenantSettingsDO settings) {
        if (settings == null || !StringUtils.hasText(settings.getMemberExportDesignatedJson())) {
            return new ArrayList<>();
        }
        try {
            List<String> ids = objectMapper.readValue(settings.getMemberExportDesignatedJson(), new TypeReference<>() {
            });
            return ids != null ? new ArrayList<>(ids) : new ArrayList<>();
        } catch (Exception ex) {
            throw new IllegalStateException("parse member_export_designated_json failed", ex);
        }
    }

    private String writeDesignatedUserIds(List<String> userIds) {
        try {
            return objectMapper.writeValueAsString(userIds != null ? userIds : List.of());
        } catch (Exception ex) {
            throw new IllegalStateException("serialize member_export_designated_json failed", ex);
        }
    }

    private static List<GroupSizeUserRuleVO> nullSafeUserRules(ImProductSettingsRespVO request) {
        return request.getUserRules() != null ? request.getUserRules() : List.of();
    }

    private static List<GroupSizeGroupRuleVO> nullSafeGroupRules(ImProductSettingsRespVO request) {
        return request.getGroupRules() != null ? request.getGroupRules() : List.of();
    }

    private static Long parsePositiveLong(String value, ErrorCodeConstants errorCode) {
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(errorCode);
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                throw new ServiceException(errorCode);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new ServiceException(errorCode);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
