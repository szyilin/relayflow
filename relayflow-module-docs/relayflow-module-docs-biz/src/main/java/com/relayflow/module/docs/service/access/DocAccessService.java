package com.relayflow.module.docs.service.access;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.docs.dal.dataobject.DocDriveFolderDO;
import com.relayflow.module.docs.dal.dataobject.DocDriveItemDO;
import com.relayflow.module.docs.dal.dataobject.DocLibraryNodeDO;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.dataobject.DocShareDO;
import com.relayflow.module.docs.dal.mapper.DocDriveFolderMapper;
import com.relayflow.module.docs.dal.mapper.DocDriveItemMapper;
import com.relayflow.module.docs.dal.mapper.DocLibraryNodeMapper;
import com.relayflow.module.docs.dal.mapper.DocObjectMapper;
import com.relayflow.module.docs.dal.mapper.DocShareMapper;
import com.relayflow.module.docs.enums.DocConstants;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DocAccessService {

    public enum AccessLevel {
        NONE,
        VIEW,
        EDIT,
        OWNER
    }

    @Getter
    @RequiredArgsConstructor
    public static final class AccessResult {
        private final DocObjectDO object;
        private final AccessLevel level;
        private final String sharePermission;

        public boolean canRead() {
            return level != AccessLevel.NONE;
        }

        public boolean canEdit() {
            return level == AccessLevel.EDIT || level == AccessLevel.OWNER;
        }

        public boolean isOwner() {
            return level == AccessLevel.OWNER;
        }
    }

    private final DocLibraryNodeMapper docLibraryNodeMapper;
    private final DocObjectMapper docObjectMapper;
    private final DocDriveFolderMapper docDriveFolderMapper;
    private final DocDriveItemMapper docDriveItemMapper;
    private final DocShareMapper docShareMapper;

    public DocLibraryNodeDO requireOwnedNode(Long nodeId, Long userId) {
        DocLibraryNodeDO row = docLibraryNodeMapper.selectById(nodeId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        if (!Objects.equals(row.getOwnerUserId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_FORBIDDEN);
        }
        return row;
    }

    public DocObjectDO requireOwnedObject(Long objectId, Long userId) {
        DocObjectDO row = docObjectMapper.selectById(objectId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        if (!Objects.equals(row.getOwnerUserId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_FORBIDDEN);
        }
        return row;
    }

    public DocObjectDO requireOwnedRichDoc(Long objectId, Long userId) {
        DocObjectDO row = requireOwnedObject(objectId, userId);
        if (!DocConstants.OBJECT_TYPE_RICH_DOC.equals(row.getType())) {
            throw new ServiceException(ErrorCodeConstants.DOC_TYPE_UNSUPPORTED);
        }
        return row;
    }

    public DocDriveFolderDO requireOwnedDriveFolder(Long folderId, Long userId) {
        DocDriveFolderDO row = docDriveFolderMapper.selectById(folderId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_DRIVE_FOLDER_INVALID);
        }
        if (!Objects.equals(row.getOwnerUserId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_FORBIDDEN);
        }
        return row;
    }

    public DocDriveItemDO requireOwnedDriveItem(Long itemId, Long userId) {
        DocDriveItemDO row = docDriveItemMapper.selectById(itemId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        if (!Objects.equals(row.getOwnerUserId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_FORBIDDEN);
        }
        return row;
    }

    public AccessResult resolveAccess(Long objectId, Long userId) {
        DocObjectDO row = docObjectMapper.selectById(objectId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        if (Objects.equals(row.getOwnerUserId(), userId)) {
            return new AccessResult(row, AccessLevel.OWNER, null);
        }
        DocShareDO share = docShareMapper.selectOne(
                Wrappers.<DocShareDO>lambdaQuery()
                        .eq(DocShareDO::getObjectId, objectId)
                        .eq(DocShareDO::getGranteeUserId, userId)
                        .last("LIMIT 1"));
        if (share == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_FORBIDDEN);
        }
        if (DocConstants.SHARE_PERMISSION_EDIT.equals(share.getPermission())) {
            return new AccessResult(row, AccessLevel.EDIT, share.getPermission());
        }
        return new AccessResult(row, AccessLevel.VIEW, DocConstants.SHARE_PERMISSION_VIEW);
    }

    public AccessResult requireReadableRichDoc(Long objectId, Long userId) {
        AccessResult access = resolveAccess(objectId, userId);
        if (!DocConstants.OBJECT_TYPE_RICH_DOC.equals(access.getObject().getType())) {
            throw new ServiceException(ErrorCodeConstants.DOC_TYPE_UNSUPPORTED);
        }
        return access;
    }

    public AccessResult requireEditableRichDoc(Long objectId, Long userId) {
        AccessResult access = requireReadableRichDoc(objectId, userId);
        if (!access.canEdit()) {
            throw new ServiceException(ErrorCodeConstants.DOC_FORBIDDEN);
        }
        return access;
    }

    public DocObjectDO requireOwnedShareableRichDoc(Long objectId, Long userId) {
        DocObjectDO row = requireOwnedObject(objectId, userId);
        if (!DocConstants.OBJECT_TYPE_RICH_DOC.equals(row.getType())) {
            throw new ServiceException(ErrorCodeConstants.DOC_SHARE_TYPE_UNSUPPORTED);
        }
        return row;
    }

    public String resolveLocationLabel(DocObjectDO object, Long viewerUserId, boolean deleted) {
        if (deleted || object == null) {
            return DocConstants.LOCATION_DELETED;
        }
        if (Objects.equals(object.getOwnerUserId(), viewerUserId)) {
            Long libraryCount = docLibraryNodeMapper.selectCount(
                    Wrappers.<DocLibraryNodeDO>lambdaQuery()
                            .eq(DocLibraryNodeDO::getObjectId, object.getId()));
            if (libraryCount != null && libraryCount > 0) {
                return DocConstants.LOCATION_LIBRARY;
            }
            Long driveCount = docDriveItemMapper.selectCount(
                    Wrappers.<DocDriveItemDO>lambdaQuery()
                            .eq(DocDriveItemDO::getObjectId, object.getId()));
            if (driveCount != null && driveCount > 0) {
                return DocConstants.LOCATION_DRIVE;
            }
            return DocConstants.LOCATION_LIBRARY;
        }
        return DocConstants.LOCATION_SHARED;
    }
}
