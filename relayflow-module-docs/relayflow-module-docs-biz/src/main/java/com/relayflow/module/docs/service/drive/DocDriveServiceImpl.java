package com.relayflow.module.docs.service.drive;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.docs.controller.app.vo.DocDriveFileRegisterReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveFolderCreateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveFolderListRespVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveFolderRespVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveFolderUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveItemRespVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveItemUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveListingRespVO;
import com.relayflow.module.docs.controller.app.vo.DocPlacementMoveReqVO;
import com.relayflow.module.docs.controller.app.vo.DocPlacementMoveRespVO;
import com.relayflow.module.docs.dal.dataobject.DocDriveFolderDO;
import com.relayflow.module.docs.dal.dataobject.DocDriveItemDO;
import com.relayflow.module.docs.dal.dataobject.DocLibraryNodeDO;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.mapper.DocDriveFolderMapper;
import com.relayflow.module.docs.dal.mapper.DocDriveItemMapper;
import com.relayflow.module.docs.dal.mapper.DocLibraryNodeMapper;
import com.relayflow.module.docs.dal.mapper.DocObjectMapper;
import com.relayflow.module.docs.enums.DocConstants;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import com.relayflow.module.docs.service.access.DocAccessService;
import com.relayflow.module.infra.api.file.FileApi;
import com.relayflow.module.infra.api.file.dto.FileBindReqDTO;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocDriveServiceImpl implements DocDriveService {

    private final DocDriveFolderMapper docDriveFolderMapper;
    private final DocDriveItemMapper docDriveItemMapper;
    private final DocLibraryNodeMapper docLibraryNodeMapper;
    private final DocObjectMapper docObjectMapper;
    private final DocAccessService docAccessService;
    private final FileApi fileApi;

    @Override
    public DocDriveFolderListRespVO listFolders(Long parentId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        if (parentId != null) {
            docAccessService.requireOwnedDriveFolder(parentId, userId);
        }
        DocDriveFolderListRespVO resp = new DocDriveFolderListRespVO();
        resp.setFolders(loadChildFolders(userId, parentId).stream().map(this::toFolderResp).toList());
        return resp;
    }

    @Override
    public DocDriveListingRespVO listItems(Long folderId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        if (folderId != null) {
            docAccessService.requireOwnedDriveFolder(folderId, userId);
        }

        List<DocDriveFolderRespVO> folders = loadChildFolders(userId, folderId).stream()
                .map(this::toFolderResp)
                .toList();

        List<DocDriveItemDO> itemRows = loadChildItems(userId, folderId);
        Map<Long, DocObjectDO> objectsById = loadObjects(itemRows);
        List<DocDriveItemRespVO> items = new ArrayList<>(itemRows.size());
        for (DocDriveItemDO row : itemRows) {
            DocObjectDO object = objectsById.get(row.getObjectId());
            if (object == null) {
                continue;
            }
            items.add(toItemResp(row, object, resolveFileMeta(object)));
        }

        DocDriveListingRespVO resp = new DocDriveListingRespVO();
        resp.setFolders(folders);
        resp.setItems(items);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocDriveFolderRespVO createFolder(DocDriveFolderCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long parentId = request.getParentId();
        if (parentId != null) {
            docAccessService.requireOwnedDriveFolder(parentId, userId);
        }

        OffsetDateTime now = OffsetDateTime.now();
        DocDriveFolderDO folder = new DocDriveFolderDO();
        folder.setTenantId(tenantId);
        folder.setOwnerUserId(userId);
        folder.setParentId(parentId);
        folder.setName(resolveFolderName(request.getName()));
        folder.setSortOrder(nextFolderSortOrder(userId, parentId));
        folder.setCreator(userId);
        folder.setCreateTime(now);
        folder.setUpdater(userId);
        folder.setUpdateTime(now);
        docDriveFolderMapper.insert(folder);
        return toFolderResp(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocDriveFolderRespVO updateFolder(Long folderId, DocDriveFolderUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocDriveFolderDO folder = docAccessService.requireOwnedDriveFolder(folderId, userId);
        boolean changed = false;
        OffsetDateTime now = OffsetDateTime.now();

        if (request.getName() != null) {
            folder.setName(resolveFolderName(request.getName()));
            changed = true;
        }
        if (request.isParentIdSpecified()) {
            Long newParentId = request.getParentId();
            if (!Objects.equals(folder.getParentId(), newParentId)) {
                validateFolderMove(userId, folderId, newParentId);
                folder.setParentId(newParentId);
                changed = true;
            }
        }
        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
            changed = true;
        }
        if (changed) {
            folder.setUpdater(userId);
            folder.setUpdateTime(now);
            docDriveFolderMapper.updateById(folder);
        }
        return toFolderResp(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long folderId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        docAccessService.requireOwnedDriveFolder(folderId, userId);

        Long childFolderCount = docDriveFolderMapper.selectCount(
                Wrappers.<DocDriveFolderDO>lambdaQuery()
                        .eq(DocDriveFolderDO::getOwnerUserId, userId)
                        .eq(DocDriveFolderDO::getParentId, folderId));
        Long childItemCount = docDriveItemMapper.selectCount(
                Wrappers.<DocDriveItemDO>lambdaQuery()
                        .eq(DocDriveItemDO::getOwnerUserId, userId)
                        .eq(DocDriveItemDO::getFolderId, folderId));
        if ((childFolderCount != null && childFolderCount > 0)
                || (childItemCount != null && childItemCount > 0)) {
            throw new ServiceException(ErrorCodeConstants.DOC_DRIVE_FOLDER_NOT_EMPTY);
        }
        docDriveFolderMapper.deleteById(folderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocDriveItemRespVO registerFile(DocDriveFileRegisterReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long folderId = request.getFolderId();
        if (folderId != null) {
            docAccessService.requireOwnedDriveFolder(folderId, userId);
        }

        FileRespDTO file = requireOwnedStorageFile(request.getFileId(), userId);
        String title = resolveFileTitle(request.getTitle(), file.getOriginalName());
        OffsetDateTime now = OffsetDateTime.now();

        DocObjectDO object = new DocObjectDO();
        object.setTenantId(tenantId);
        object.setType(DocConstants.OBJECT_TYPE_FILE);
        object.setTitle(title);
        object.setBody(DocConstants.DEFAULT_BODY_JSON);
        object.setBodyFormat(DocConstants.BODY_FORMAT_TIPTAP_JSON_V1);
        object.setContentVersion(0);
        object.setOwnerUserId(userId);
        object.setStorageFileId(file.getId());
        object.setCreator(userId);
        object.setCreateTime(now);
        object.setUpdater(userId);
        object.setUpdateTime(now);
        docObjectMapper.insert(object);

        DocDriveItemDO item = new DocDriveItemDO();
        item.setTenantId(tenantId);
        item.setOwnerUserId(userId);
        item.setFolderId(folderId);
        item.setObjectId(object.getId());
        item.setSortOrder(nextItemSortOrder(userId, folderId));
        item.setCreator(userId);
        item.setCreateTime(now);
        item.setUpdater(userId);
        item.setUpdateTime(now);
        docDriveItemMapper.insert(item);

        FileBindReqDTO bind = new FileBindReqDTO();
        bind.setFileId(file.getId());
        bind.setBizType(DocConstants.FILE_BIND_BIZ_TYPE);
        bind.setBizId(object.getId());
        fileApi.bindFile(bind);

        return toItemResp(item, object, file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocDriveItemRespVO updateItem(Long itemId, DocDriveItemUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocDriveItemDO item = docAccessService.requireOwnedDriveItem(itemId, userId);
        DocObjectDO object = docAccessService.requireOwnedObject(item.getObjectId(), userId);
        boolean itemChanged = false;
        OffsetDateTime now = OffsetDateTime.now();

        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (!StringUtils.hasText(title)) {
                title = DocConstants.DEFAULT_TITLE;
            }
            if (!Objects.equals(object.getTitle(), title)) {
                object.setTitle(title);
                object.setUpdater(userId);
                object.setUpdateTime(now);
                docObjectMapper.updateById(object);
            }
        }

        if (request.isFolderIdSpecified()) {
            Long newFolderId = request.getFolderId();
            if (!Objects.equals(item.getFolderId(), newFolderId)) {
                if (newFolderId != null) {
                    docAccessService.requireOwnedDriveFolder(newFolderId, userId);
                }
                item.setFolderId(newFolderId);
                itemChanged = true;
            }
        }
        if (request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
            itemChanged = true;
        }
        if (itemChanged) {
            item.setUpdater(userId);
            item.setUpdateTime(now);
            docDriveItemMapper.updateById(item);
        }

        object = docObjectMapper.selectById(object.getId());
        return toItemResp(item, object, resolveFileMeta(object));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocDriveItemDO item = docAccessService.requireOwnedDriveItem(itemId, userId);
        Long objectId = item.getObjectId();
        docDriveItemMapper.deleteById(itemId);
        if (objectId != null) {
            docObjectMapper.deleteById(objectId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocPlacementMoveRespVO movePlacement(DocPlacementMoveReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        String target = request.getTarget() == null ? "" : request.getTarget().trim().toUpperCase();
        DocObjectDO object = docAccessService.requireOwnedObject(request.getObjectId(), userId);

        DocLibraryNodeDO libraryNode = findLibraryNodeByObjectId(userId, object.getId());
        DocDriveItemDO driveItem = findDriveItemByObjectId(userId, object.getId());

        if (DocConstants.PLACEMENT_TARGET_DRIVE.equals(target)) {
            if (driveItem != null) {
                throw new ServiceException(ErrorCodeConstants.DOC_PLACEMENT_INVALID);
            }
            if (libraryNode == null) {
                throw new ServiceException(ErrorCodeConstants.DOC_PLACEMENT_INVALID);
            }
            Long folderId = request.getFolderId();
            if (folderId != null) {
                docAccessService.requireOwnedDriveFolder(folderId, userId);
            }
            docLibraryNodeMapper.deleteById(libraryNode.getId());

            OffsetDateTime now = OffsetDateTime.now();
            DocDriveItemDO item = new DocDriveItemDO();
            item.setTenantId(tenantId);
            item.setOwnerUserId(userId);
            item.setFolderId(folderId);
            item.setObjectId(object.getId());
            item.setSortOrder(nextItemSortOrder(userId, folderId));
            item.setCreator(userId);
            item.setCreateTime(now);
            item.setUpdater(userId);
            item.setUpdateTime(now);
            docDriveItemMapper.insert(item);

            DocPlacementMoveRespVO resp = new DocPlacementMoveRespVO();
            resp.setObjectId(object.getId());
            resp.setTarget(DocConstants.PLACEMENT_TARGET_DRIVE);
            resp.setPlacementId(item.getId());
            return resp;
        }

        if (DocConstants.PLACEMENT_TARGET_LIBRARY.equals(target)) {
            if (libraryNode != null) {
                throw new ServiceException(ErrorCodeConstants.DOC_PLACEMENT_INVALID);
            }
            if (driveItem == null) {
                throw new ServiceException(ErrorCodeConstants.DOC_PLACEMENT_INVALID);
            }
            if (!DocConstants.OBJECT_TYPE_RICH_DOC.equals(object.getType())) {
                throw new ServiceException(ErrorCodeConstants.DOC_TYPE_UNSUPPORTED);
            }
            Long parentId = request.getParentId();
            if (parentId != null) {
                docAccessService.requireOwnedNode(parentId, userId);
            }
            docDriveItemMapper.deleteById(driveItem.getId());

            OffsetDateTime now = OffsetDateTime.now();
            DocLibraryNodeDO node = new DocLibraryNodeDO();
            node.setTenantId(tenantId);
            node.setOwnerUserId(userId);
            node.setParentId(parentId);
            node.setObjectId(object.getId());
            node.setSortOrder(nextLibrarySortOrder(userId, parentId));
            node.setCreator(userId);
            node.setCreateTime(now);
            node.setUpdater(userId);
            node.setUpdateTime(now);
            docLibraryNodeMapper.insert(node);

            DocPlacementMoveRespVO resp = new DocPlacementMoveRespVO();
            resp.setObjectId(object.getId());
            resp.setTarget(DocConstants.PLACEMENT_TARGET_LIBRARY);
            resp.setPlacementId(node.getId());
            return resp;
        }

        throw new ServiceException(ErrorCodeConstants.DOC_PLACEMENT_INVALID);
    }

    private DocLibraryNodeDO findLibraryNodeByObjectId(Long userId, Long objectId) {
        return docLibraryNodeMapper.selectOne(
                Wrappers.<DocLibraryNodeDO>lambdaQuery()
                        .eq(DocLibraryNodeDO::getOwnerUserId, userId)
                        .eq(DocLibraryNodeDO::getObjectId, objectId)
                        .last("LIMIT 1"));
    }

    private DocDriveItemDO findDriveItemByObjectId(Long userId, Long objectId) {
        return docDriveItemMapper.selectOne(
                Wrappers.<DocDriveItemDO>lambdaQuery()
                        .eq(DocDriveItemDO::getOwnerUserId, userId)
                        .eq(DocDriveItemDO::getObjectId, objectId)
                        .last("LIMIT 1"));
    }

    private int nextLibrarySortOrder(Long userId, Long parentId) {
        var query = Wrappers.<DocLibraryNodeDO>lambdaQuery()
                .eq(DocLibraryNodeDO::getOwnerUserId, userId);
        if (parentId == null) {
            query.isNull(DocLibraryNodeDO::getParentId);
        } else {
            query.eq(DocLibraryNodeDO::getParentId, parentId);
        }
        return docLibraryNodeMapper.selectList(query).stream()
                .map(DocLibraryNodeDO::getSortOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(-1) + 1;
    }

    private FileRespDTO requireOwnedStorageFile(Long fileId, Long userId) {
        if (fileId == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_STORAGE_FILE_INVALID);
        }
        FileRespDTO file;
        try {
            file = fileApi.getFile(fileId);
        } catch (ServiceException ex) {
            throw new ServiceException(ErrorCodeConstants.DOC_STORAGE_FILE_INVALID);
        }
        if (file == null || file.getId() == null || !Objects.equals(file.getCreator(), userId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_STORAGE_FILE_INVALID);
        }
        return file;
    }

    private FileRespDTO resolveFileMeta(DocObjectDO object) {
        if (object == null
                || !DocConstants.OBJECT_TYPE_FILE.equals(object.getType())
                || object.getStorageFileId() == null) {
            return null;
        }
        try {
            return fileApi.getFile(object.getStorageFileId());
        } catch (ServiceException ignored) {
            return null;
        }
    }

    private List<DocDriveFolderDO> loadChildFolders(Long userId, Long parentId) {
        var query = Wrappers.<DocDriveFolderDO>lambdaQuery()
                .eq(DocDriveFolderDO::getOwnerUserId, userId);
        if (parentId == null) {
            query.isNull(DocDriveFolderDO::getParentId);
        } else {
            query.eq(DocDriveFolderDO::getParentId, parentId);
        }
        return docDriveFolderMapper.selectList(
                        query.orderByAsc(DocDriveFolderDO::getSortOrder)
                                .orderByAsc(DocDriveFolderDO::getId))
                .stream()
                .sorted(Comparator
                        .comparing(DocDriveFolderDO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DocDriveFolderDO::getId))
                .toList();
    }

    private List<DocDriveItemDO> loadChildItems(Long userId, Long folderId) {
        var query = Wrappers.<DocDriveItemDO>lambdaQuery()
                .eq(DocDriveItemDO::getOwnerUserId, userId);
        if (folderId == null) {
            query.isNull(DocDriveItemDO::getFolderId);
        } else {
            query.eq(DocDriveItemDO::getFolderId, folderId);
        }
        return docDriveItemMapper.selectList(
                        query.orderByAsc(DocDriveItemDO::getSortOrder)
                                .orderByAsc(DocDriveItemDO::getId))
                .stream()
                .sorted(Comparator
                        .comparing(DocDriveItemDO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DocDriveItemDO::getId))
                .toList();
    }

    private Map<Long, DocObjectDO> loadObjects(List<DocDriveItemDO> items) {
        Set<Long> objectIds = items.stream()
                .map(DocDriveItemDO::getObjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (objectIds.isEmpty()) {
            return Map.of();
        }
        return docObjectMapper.selectBatchIds(objectIds).stream()
                .collect(Collectors.toMap(DocObjectDO::getId, row -> row));
    }

    private void validateFolderMove(Long userId, Long folderId, Long newParentId) {
        if (newParentId == null) {
            return;
        }
        if (Objects.equals(folderId, newParentId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_DRIVE_FOLDER_INVALID);
        }
        docAccessService.requireOwnedDriveFolder(newParentId, userId);

        List<DocDriveFolderDO> allFolders = docDriveFolderMapper.selectList(
                Wrappers.<DocDriveFolderDO>lambdaQuery()
                        .eq(DocDriveFolderDO::getOwnerUserId, userId));
        Map<Long, List<DocDriveFolderDO>> childrenByParent = new HashMap<>();
        for (DocDriveFolderDO folder : allFolders) {
            Long parentKey = folder.getParentId() == null ? 0L : folder.getParentId();
            childrenByParent.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(folder);
        }
        Set<Long> descendants = collectSubtreeFolderIds(folderId, childrenByParent);
        if (descendants.contains(newParentId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_DRIVE_FOLDER_INVALID);
        }
    }

    private Set<Long> collectSubtreeFolderIds(Long rootId, Map<Long, List<DocDriveFolderDO>> childrenByParent) {
        Set<Long> ids = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (!ids.add(current)) {
                continue;
            }
            for (DocDriveFolderDO child : childrenByParent.getOrDefault(current, List.of())) {
                queue.add(child.getId());
            }
        }
        return ids;
    }

    private int nextFolderSortOrder(Long userId, Long parentId) {
        return loadChildFolders(userId, parentId).stream()
                .map(DocDriveFolderDO::getSortOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(-1) + 1;
    }

    private int nextItemSortOrder(Long userId, Long folderId) {
        return loadChildItems(userId, folderId).stream()
                .map(DocDriveItemDO::getSortOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(-1) + 1;
    }

    private static String resolveFolderName(String name) {
        if (!StringUtils.hasText(name)) {
            return DocConstants.DEFAULT_FOLDER_NAME;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? DocConstants.DEFAULT_FOLDER_NAME : trimmed;
    }

    private static String resolveFileTitle(String title, String originalName) {
        if (StringUtils.hasText(title)) {
            String trimmed = title.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        if (StringUtils.hasText(originalName)) {
            return originalName.trim();
        }
        return DocConstants.DEFAULT_TITLE;
    }

    private DocDriveFolderRespVO toFolderResp(DocDriveFolderDO folder) {
        DocDriveFolderRespVO vo = new DocDriveFolderRespVO();
        vo.setFolderId(folder.getId());
        vo.setParentId(folder.getParentId());
        vo.setName(folder.getName());
        vo.setSortOrder(folder.getSortOrder());
        vo.setUpdateTime(folder.getUpdateTime());
        return vo;
    }

    private DocDriveItemRespVO toItemResp(DocDriveItemDO item, DocObjectDO object, FileRespDTO file) {
        DocDriveItemRespVO vo = new DocDriveItemRespVO();
        vo.setItemId(item.getId());
        vo.setFolderId(item.getFolderId());
        vo.setObjectId(item.getObjectId());
        vo.setType(object.getType());
        vo.setTitle(object.getTitle());
        vo.setStorageFileId(object.getStorageFileId());
        vo.setSortOrder(item.getSortOrder());
        vo.setUpdateTime(item.getUpdateTime());
        if (file != null) {
            vo.setSizeBytes(file.getSize());
            vo.setMimeType(file.getMimeType());
            if (vo.getStorageFileId() == null) {
                vo.setStorageFileId(file.getId());
            }
        }
        return vo;
    }
}
