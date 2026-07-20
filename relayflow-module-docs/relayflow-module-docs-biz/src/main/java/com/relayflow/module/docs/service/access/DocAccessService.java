package com.relayflow.module.docs.service.access;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.docs.dal.dataobject.DocLibraryNodeDO;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.mapper.DocLibraryNodeMapper;
import com.relayflow.module.docs.dal.mapper.DocObjectMapper;
import com.relayflow.module.docs.enums.DocConstants;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DocAccessService {

    private final DocLibraryNodeMapper docLibraryNodeMapper;
    private final DocObjectMapper docObjectMapper;

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
}
