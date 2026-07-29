package com.relayflow.module.docs.service.admin;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.docs.controller.admin.vo.DocAdminFilePageReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminFilePageRespVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminFilePageRespVO.DocAdminFileItemRespVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminObjectIdsReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminRestoreReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminTransferOwnerReqVO;
import com.relayflow.module.docs.dal.dataobject.DocDriveItemDO;
import com.relayflow.module.docs.dal.dataobject.DocLibraryNodeDO;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.mapper.DocDriveItemMapper;
import com.relayflow.module.docs.dal.mapper.DocLibraryNodeMapper;
import com.relayflow.module.docs.dal.mapper.DocObjectExtMapper;
import com.relayflow.module.docs.dal.mapper.DocPlacementExtMapper;
import com.relayflow.module.docs.enums.DocConstants;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import com.relayflow.module.infra.api.file.FileApi;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Admin enterprise file ops. Responses never include document body.
 * <p>
 * Transfer / restore placement freeze (V1): keep current surface —
 * library node → new owner's Library root ({@code parent_id=null});
 * drive item → new owner's Drive root ({@code folder_id=null}).
 * If neither placement exists, create Library root for {@code RICH_DOC}, Drive root for {@code FILE}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocAdminFilesServiceImpl implements DocAdminFilesService {

    private final DocObjectExtMapper docObjectExtMapper;
    private final DocPlacementExtMapper docPlacementExtMapper;
    private final DocLibraryNodeMapper docLibraryNodeMapper;
    private final DocDriveItemMapper docDriveItemMapper;
    private final TenantMemberApi tenantMemberApi;
    private final UserApi userApi;
    private final FileApi fileApi;

    @Override
    public DocAdminFilePageRespVO page(DocAdminFilePageReqVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 20 : request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;
        String type = StringUtils.hasText(request.getType()) ? request.getType().trim().toUpperCase() : null;
        String lifecycle = StringUtils.hasText(request.getLifecycle())
                ? request.getLifecycle().trim().toUpperCase()
                : null;

        long total = docObjectExtMapper.countAdminPage(
                tenantId, keyword, type, lifecycle, request.getOwnerUserId());
        long offset = (long) (pageNo - 1) * pageSize;
        List<DocObjectDO> rows = total == 0
                ? List.of()
                : docObjectExtMapper.selectAdminPage(
                        tenantId, keyword, type, lifecycle, request.getOwnerUserId(), offset, pageSize);

        Map<Long, String> nicknames = resolveNicknames(rows);
        Map<Long, Long> sizes = resolveFileSizes(rows);

        OffsetDateTime now = OffsetDateTime.now();
        List<DocAdminFileItemRespVO> list = new ArrayList<>(rows.size());
        for (DocObjectDO row : rows) {
            DocAdminFileItemRespVO item = new DocAdminFileItemRespVO();
            item.setObjectId(row.getId());
            item.setTitle(row.getTitle());
            item.setType(row.getType());
            item.setLifecycle(row.getLifecycle());
            item.setOwnerUserId(row.getOwnerUserId());
            item.setOwnerDisplayName(nicknames.getOrDefault(
                    row.getOwnerUserId(),
                    row.getOwnerUserId() == null ? "" : String.valueOf(row.getOwnerUserId())));
            item.setUpdatedAt(row.getUpdateTime());
            item.setSizeBytes(sizes.get(row.getId()));
            item.setDeletedAt(row.getDeletedAt());
            item.setDaysUntilPurge(computeDaysUntilPurge(row, now));
            list.add(item);
        }

        DocAdminFilePageRespVO resp = new DocAdminFilePageRespVO();
        resp.setList(list);
        resp.setTotal(total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);
        resp.setPageNo(pageNo);
        resp.setPageSize(pageSize);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(DocAdminTransferOwnerReqVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long operatorId = SecurityFrameworkUtils.requireLoginUserId();
        Long newOwnerUserId = request.getNewOwnerUserId();
        requireActiveMember(tenantId, newOwnerUserId);

        List<DocObjectDO> objects = loadRequiredObjects(tenantId, request.getObjectIds());
        OffsetDateTime now = OffsetDateTime.now();
        for (DocObjectDO object : objects) {
            if (!DocConstants.LIFECYCLE_ACTIVE.equals(object.getLifecycle())) {
                throw new ServiceException(ErrorCodeConstants.DOC_FILE_LIFECYCLE_INVALID);
            }
            applyOwnerAndPlacement(tenantId, object, newOwnerUserId, 0, operatorId, now);
            docObjectExtMapper.updateLifecycleAndOwner(
                    tenantId,
                    object.getId(),
                    DocConstants.LIFECYCLE_ACTIVE,
                    0,
                    null,
                    newOwnerUserId,
                    operatorId,
                    now);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(DocAdminObjectIdsReqVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long operatorId = SecurityFrameworkUtils.requireLoginUserId();
        List<DocObjectDO> objects = loadRequiredObjects(tenantId, request.getObjectIds());
        OffsetDateTime now = OffsetDateTime.now();
        for (DocObjectDO object : objects) {
            if (!DocConstants.LIFECYCLE_ACTIVE.equals(object.getLifecycle())) {
                throw new ServiceException(ErrorCodeConstants.DOC_FILE_LIFECYCLE_INVALID);
            }
            softDeletePlacements(tenantId, object.getId(), operatorId, now);
            docObjectExtMapper.updateLifecycleAndOwner(
                    tenantId,
                    object.getId(),
                    DocConstants.LIFECYCLE_DELETED,
                    1,
                    now,
                    object.getOwnerUserId(),
                    operatorId,
                    now);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(DocAdminRestoreReqVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long operatorId = SecurityFrameworkUtils.requireLoginUserId();
        List<DocObjectDO> objects = loadRequiredObjects(tenantId, request.getObjectIds());
        OffsetDateTime now = OffsetDateTime.now();
        Long overrideOwner = request.getNewOwnerUserId();
        if (overrideOwner != null) {
            requireActiveMember(tenantId, overrideOwner);
        }

        for (DocObjectDO object : objects) {
            if (!DocConstants.LIFECYCLE_DELETED.equals(object.getLifecycle())) {
                throw new ServiceException(ErrorCodeConstants.DOC_FILE_LIFECYCLE_INVALID);
            }
            if (object.getDeletedAt() == null
                    || ChronoUnit.DAYS.between(object.getDeletedAt(), now) > DocConstants.RESTORE_WINDOW_DAYS) {
                throw new ServiceException(ErrorCodeConstants.DOC_FILE_RESTORE_EXPIRED);
            }
            Long ownerId = overrideOwner != null ? overrideOwner : object.getOwnerUserId();
            if (overrideOwner == null) {
                requireActiveMember(tenantId, ownerId);
            }
            applyOwnerAndPlacement(tenantId, object, ownerId, 0, operatorId, now);
            docObjectExtMapper.updateLifecycleAndOwner(
                    tenantId,
                    object.getId(),
                    DocConstants.LIFECYCLE_ACTIVE,
                    0,
                    null,
                    ownerId,
                    operatorId,
                    now);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purge(DocAdminObjectIdsReqVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long operatorId = SecurityFrameworkUtils.requireLoginUserId();
        List<DocObjectDO> objects = loadRequiredObjects(tenantId, request.getObjectIds());
        OffsetDateTime now = OffsetDateTime.now();
        for (DocObjectDO object : objects) {
            if (!DocConstants.LIFECYCLE_DELETED.equals(object.getLifecycle())) {
                throw new ServiceException(ErrorCodeConstants.DOC_FILE_LIFECYCLE_INVALID);
            }
            softDeletePlacements(tenantId, object.getId(), operatorId, now);
            int updated = docObjectExtMapper.purgeObject(tenantId, object.getId(), operatorId, now);
            if (updated != 1) {
                throw new ServiceException(ErrorCodeConstants.DOC_FILE_LIFECYCLE_INVALID);
            }
        }
    }

    private void applyOwnerAndPlacement(
            Long tenantId,
            DocObjectDO object,
            Long newOwnerUserId,
            int deleted,
            Long updater,
            OffsetDateTime now) {
        DocLibraryNodeDO libraryNode =
                docPlacementExtMapper.selectLibraryNodeByObjectIdIncludeDeleted(tenantId, object.getId());
        DocDriveItemDO driveItem =
                docPlacementExtMapper.selectDriveItemByObjectIdIncludeDeleted(tenantId, object.getId());

        boolean libraryLive = isLive(libraryNode);
        boolean driveLive = isLive(driveItem);
        if (libraryLive) {
            docPlacementExtMapper.updateLibraryNodeOwnerRoot(
                    tenantId, libraryNode.getId(), newOwnerUserId, deleted, updater, now);
            return;
        }
        if (driveLive) {
            docPlacementExtMapper.updateDriveItemOwnerRoot(
                    tenantId, driveItem.getId(), newOwnerUserId, deleted, updater, now);
            return;
        }
        if (libraryNode != null) {
            docPlacementExtMapper.updateLibraryNodeOwnerRoot(
                    tenantId, libraryNode.getId(), newOwnerUserId, deleted, updater, now);
            return;
        }
        if (driveItem != null) {
            docPlacementExtMapper.updateDriveItemOwnerRoot(
                    tenantId, driveItem.getId(), newOwnerUserId, deleted, updater, now);
            return;
        }

        // No placement row: create root placement by type
        if (DocConstants.OBJECT_TYPE_FILE.equals(object.getType())) {
            DocDriveItemDO created = new DocDriveItemDO();
            created.setTenantId(tenantId);
            created.setOwnerUserId(newOwnerUserId);
            created.setFolderId(null);
            created.setObjectId(object.getId());
            created.setSortOrder(0);
            created.setCreator(updater);
            created.setUpdater(updater);
            created.setCreateTime(now);
            created.setUpdateTime(now);
            created.setDeleted(deleted);
            docDriveItemMapper.insert(created);
            return;
        }
        DocLibraryNodeDO created = new DocLibraryNodeDO();
        created.setTenantId(tenantId);
        created.setOwnerUserId(newOwnerUserId);
        created.setParentId(null);
        created.setObjectId(object.getId());
        created.setSortOrder(0);
        created.setCreator(updater);
        created.setUpdater(updater);
        created.setCreateTime(now);
        created.setUpdateTime(now);
        created.setDeleted(deleted);
        docLibraryNodeMapper.insert(created);
    }

    private static boolean isLive(DocLibraryNodeDO node) {
        return node != null && (node.getDeleted() == null || Objects.equals(node.getDeleted(), 0));
    }

    private static boolean isLive(DocDriveItemDO item) {
        return item != null && (item.getDeleted() == null || Objects.equals(item.getDeleted(), 0));
    }

    private void softDeletePlacements(Long tenantId, Long objectId, Long updater, OffsetDateTime now) {
        DocLibraryNodeDO libraryNode =
                docPlacementExtMapper.selectLibraryNodeByObjectIdIncludeDeleted(tenantId, objectId);
        if (isLive(libraryNode)) {
            docPlacementExtMapper.softDeleteLibraryNode(tenantId, libraryNode.getId(), updater, now);
        }
        DocDriveItemDO driveItem =
                docPlacementExtMapper.selectDriveItemByObjectIdIncludeDeleted(tenantId, objectId);
        if (isLive(driveItem)) {
            docPlacementExtMapper.softDeleteDriveItem(tenantId, driveItem.getId(), updater, now);
        }
    }

    private List<DocObjectDO> loadRequiredObjects(Long tenantId, List<Long> objectIds) {
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long id : objectIds) {
            if (id != null) {
                unique.add(id);
            }
        }
        if (unique.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        List<DocObjectDO> rows = docObjectExtMapper.selectByIdsIncludeDeleted(tenantId, new ArrayList<>(unique));
        if (rows.size() != unique.size()) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        // Preserve request order
        Map<Long, DocObjectDO> byId = new HashMap<>();
        for (DocObjectDO row : rows) {
            byId.put(row.getId(), row);
        }
        List<DocObjectDO> ordered = new ArrayList<>(unique.size());
        for (Long id : unique) {
            ordered.add(byId.get(id));
        }
        return ordered;
    }

    private void requireActiveMember(Long tenantId, Long userId) {
        if (userId == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_FILE_OWNER_INVALID);
        }
        Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, Set.of(userId));
        if (!active.contains(userId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_FILE_OWNER_INVALID);
        }
    }

    private Map<Long, String> resolveNicknames(List<DocObjectDO> rows) {
        Map<Long, String> map = new HashMap<>();
        Set<Long> userIds = new HashSet<>();
        for (DocObjectDO row : rows) {
            if (row.getOwnerUserId() != null) {
                userIds.add(row.getOwnerUserId());
            }
        }
        for (Long userId : userIds) {
            map.put(userId, resolveNickname(userId));
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

    private Map<Long, Long> resolveFileSizes(List<DocObjectDO> rows) {
        Map<Long, Long> sizes = new HashMap<>();
        for (DocObjectDO row : rows) {
            if (!DocConstants.OBJECT_TYPE_FILE.equals(row.getType()) || row.getStorageFileId() == null) {
                continue;
            }
            try {
                FileRespDTO file = fileApi.getFile(row.getStorageFileId());
                if (file != null && file.getSize() != null) {
                    sizes.put(row.getId(), file.getSize());
                }
            } catch (Exception ex) {
                log.warn("Resolve file size failed (best-effort): objectId={} fileId={}",
                        row.getId(), row.getStorageFileId(), ex);
            }
        }
        return sizes;
    }

    private static Integer computeDaysUntilPurge(DocObjectDO row, OffsetDateTime now) {
        if (!DocConstants.LIFECYCLE_DELETED.equals(row.getLifecycle()) || row.getDeletedAt() == null) {
            return null;
        }
        OffsetDateTime deadline = row.getDeletedAt().plusDays(DocConstants.RESTORE_WINDOW_DAYS);
        long days = ChronoUnit.DAYS.between(now, deadline);
        if (days < 0) {
            return 0;
        }
        return (int) Math.min(days, Integer.MAX_VALUE);
    }
}
