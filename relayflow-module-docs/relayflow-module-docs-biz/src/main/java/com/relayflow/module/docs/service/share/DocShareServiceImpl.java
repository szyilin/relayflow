package com.relayflow.module.docs.service.share;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.docs.controller.app.vo.DocShareCreateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocShareItemRespVO;
import com.relayflow.module.docs.controller.app.vo.DocShareListRespVO;
import com.relayflow.module.docs.controller.app.vo.DocShareUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocSharedItemRespVO;
import com.relayflow.module.docs.controller.app.vo.DocSharedListRespVO;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.dataobject.DocShareDO;
import com.relayflow.module.docs.dal.dataobject.DocStarDO;
import com.relayflow.module.docs.dal.dataobject.DocUserVisitDO;
import com.relayflow.module.docs.dal.mapper.DocObjectMapper;
import com.relayflow.module.docs.dal.mapper.DocShareMapper;
import com.relayflow.module.docs.dal.mapper.DocStarMapper;
import com.relayflow.module.docs.dal.mapper.DocUserVisitMapper;
import com.relayflow.module.docs.enums.DocConstants;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import com.relayflow.module.docs.service.access.DocAccessService;
import com.relayflow.module.docs.service.notify.DocsBotNotifyService;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocShareServiceImpl implements DocShareService {

    private final DocShareMapper docShareMapper;
    private final DocObjectMapper docObjectMapper;
    private final DocStarMapper docStarMapper;
    private final DocUserVisitMapper docUserVisitMapper;
    private final DocAccessService docAccessService;
    private final TenantMemberApi tenantMemberApi;
    private final UserApi userApi;
    private final DocsBotNotifyService docsBotNotifyService;

    @Override
    public DocShareListRespVO listShares(Long objectId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        docAccessService.requireOwnedShareableRichDoc(objectId, userId);
        List<DocShareDO> rows = docShareMapper.selectList(
                Wrappers.<DocShareDO>lambdaQuery()
                        .eq(DocShareDO::getObjectId, objectId)
                        .orderByDesc(DocShareDO::getCreateTime));
        DocShareListRespVO resp = new DocShareListRespVO();
        resp.setShares(rows.stream().map(this::toShareItem).toList());
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocShareItemRespVO createShare(Long objectId, DocShareCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        DocObjectDO object = docAccessService.requireOwnedShareableRichDoc(objectId, userId);

        Long grantee = request.getGranteeUserId();
        if (Objects.equals(grantee, userId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_SHARE_SELF);
        }
        Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, Set.of(grantee));
        if (!active.contains(grantee)) {
            throw new ServiceException(ErrorCodeConstants.DOC_GRANTEE_INVALID);
        }
        String permission = normalizePermission(request.getPermission());

        DocShareDO existing = docShareMapper.selectOne(
                Wrappers.<DocShareDO>lambdaQuery()
                        .eq(DocShareDO::getObjectId, objectId)
                        .eq(DocShareDO::getGranteeUserId, grantee)
                        .last("LIMIT 1"));
        OffsetDateTime now = OffsetDateTime.now();
        if (existing != null) {
            existing.setPermission(permission);
            existing.setGrantedByUserId(userId);
            existing.setUpdater(userId);
            existing.setUpdateTime(now);
            docShareMapper.updateById(existing);
            docsBotNotifyService.notifyShared(object, grantee, permission);
            return toShareItem(existing);
        }

        DocShareDO row = new DocShareDO();
        row.setTenantId(tenantId);
        row.setObjectId(objectId);
        row.setGranteeUserId(grantee);
        row.setPermission(permission);
        row.setGrantedByUserId(userId);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        docShareMapper.insert(row);
        docsBotNotifyService.notifyShared(object, grantee, permission);
        return toShareItem(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocShareItemRespVO updateShare(Long objectId, Long shareId, DocShareUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocObjectDO object = docAccessService.requireOwnedShareableRichDoc(objectId, userId);
        DocShareDO row = requireShareOnObject(objectId, shareId);
        String permission = normalizePermission(request.getPermission());
        OffsetDateTime now = OffsetDateTime.now();
        row.setPermission(permission);
        row.setGrantedByUserId(userId);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        docShareMapper.updateById(row);
        docsBotNotifyService.notifyShared(object, row.getGranteeUserId(), permission);
        return toShareItem(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeShare(Long objectId, Long shareId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        docAccessService.requireOwnedShareableRichDoc(objectId, userId);
        DocShareDO row = requireShareOnObject(objectId, shareId);
        docShareMapper.deleteById(row.getId());
    }

    @Override
    public DocSharedListRespVO listSharedWithMe(int limit) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        int effective = clampLimit(limit);
        List<DocShareDO> shares = docShareMapper.selectList(
                Wrappers.<DocShareDO>lambdaQuery()
                        .eq(DocShareDO::getGranteeUserId, userId)
                        .orderByDesc(DocShareDO::getCreateTime)
                        .last("LIMIT " + effective));
        Set<Long> objectIds = shares.stream().map(DocShareDO::getObjectId).collect(Collectors.toCollection(HashSet::new));
        Set<Long> starredIds = loadStarredObjectIds(userId, objectIds);
        Map<Long, OffsetDateTime> lastOpenedByObject = loadLastOpenedAt(userId, objectIds);

        List<DocSharedItemRespVO> items = new ArrayList<>();
        for (DocShareDO share : shares) {
            DocObjectDO object = docObjectMapper.selectById(share.getObjectId());
            if (object == null || !DocConstants.OBJECT_TYPE_RICH_DOC.equals(object.getType())) {
                continue;
            }
            DocSharedItemRespVO item = new DocSharedItemRespVO();
            item.setObjectId(object.getId());
            item.setTitle(object.getTitle());
            item.setType(object.getType());
            item.setPermission(share.getPermission());
            item.setOwnerUserId(object.getOwnerUserId());
            item.setOwnerDisplayName(resolveNickname(object.getOwnerUserId()));
            item.setOwnerAvatarFileId(resolveAvatarFileId(object.getOwnerUserId()));
            item.setOwnedByMe(Objects.equals(object.getOwnerUserId(), userId));
            item.setGrantedByUserId(share.getGrantedByUserId());
            item.setGrantedByDisplayName(resolveNickname(share.getGrantedByUserId()));
            item.setSharedAt(share.getCreateTime());
            item.setCreateTime(object.getCreateTime());
            item.setLastOpenedAt(lastOpenedByObject.get(object.getId()));
            item.setLocationLabel(DocConstants.LOCATION_SHARED);
            item.setStarred(starredIds.contains(object.getId()));
            items.add(item);
        }
        DocSharedListRespVO resp = new DocSharedListRespVO();
        resp.setItems(items);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveShared(Long objectId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocShareDO row = docShareMapper.selectOne(
                Wrappers.<DocShareDO>lambdaQuery()
                        .eq(DocShareDO::getObjectId, objectId)
                        .eq(DocShareDO::getGranteeUserId, userId)
                        .last("LIMIT 1"));
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        docShareMapper.deleteById(row.getId());
    }

    private DocShareDO requireShareOnObject(Long objectId, Long shareId) {
        DocShareDO row = docShareMapper.selectById(shareId);
        if (row == null || !Objects.equals(row.getObjectId(), objectId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        return row;
    }

    private String normalizePermission(String raw) {
        String permission = StringUtils.hasText(raw) ? raw.trim().toUpperCase() : DocConstants.SHARE_PERMISSION_VIEW;
        if (!DocConstants.SHARE_PERMISSION_VIEW.equals(permission)
                && !DocConstants.SHARE_PERMISSION_EDIT.equals(permission)) {
            throw new ServiceException(ErrorCodeConstants.DOC_GRANTEE_INVALID);
        }
        return permission;
    }

    private DocShareItemRespVO toShareItem(DocShareDO row) {
        DocShareItemRespVO vo = new DocShareItemRespVO();
        vo.setShareId(row.getId());
        vo.setGranteeUserId(row.getGranteeUserId());
        vo.setGranteeDisplayName(resolveNickname(row.getGranteeUserId()));
        vo.setPermission(row.getPermission());
        vo.setGrantedByUserId(row.getGrantedByUserId());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    private Set<Long> loadStarredObjectIds(Long userId, Set<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) {
            return Set.of();
        }
        return docStarMapper.selectList(
                        Wrappers.<DocStarDO>lambdaQuery()
                                .eq(DocStarDO::getUserId, userId)
                                .in(DocStarDO::getObjectId, objectIds))
                .stream()
                .map(DocStarDO::getObjectId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Map<Long, OffsetDateTime> loadLastOpenedAt(Long userId, Set<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, OffsetDateTime> map = new HashMap<>();
        for (DocUserVisitDO visit : docUserVisitMapper.selectList(
                Wrappers.<DocUserVisitDO>lambdaQuery()
                        .eq(DocUserVisitDO::getUserId, userId)
                        .in(DocUserVisitDO::getObjectId, objectIds))) {
            map.put(visit.getObjectId(), visit.getLastOpenedAt());
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

    private String resolveAvatarFileId(Long userId) {
        try {
            UserBasicDTO basic = userApi.getUserBasic(userId);
            if (basic != null && StringUtils.hasText(basic.getAvatarFileId())) {
                return basic.getAvatarFileId();
            }
        } catch (Exception ex) {
            log.warn("Resolve avatar failed (best-effort): userId={}", userId, ex);
        }
        return null;
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }
}
