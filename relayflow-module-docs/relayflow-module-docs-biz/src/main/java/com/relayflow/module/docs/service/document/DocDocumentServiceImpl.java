package com.relayflow.module.docs.service.document;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.docs.controller.app.vo.DocDocumentBodySaveReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDocumentBodySaveRespVO;
import com.relayflow.module.docs.controller.app.vo.DocDocumentRespVO;
import com.relayflow.module.docs.controller.app.vo.DocExportMdRespVO;
import com.relayflow.module.docs.controller.app.vo.DocRecentItemRespVO;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.mapper.DocObjectMapper;
import com.relayflow.module.docs.enums.DocConstants;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import com.relayflow.module.docs.service.access.DocAccessService;
import com.relayflow.module.docs.support.DocJsonSupport;
import com.relayflow.module.docs.support.TipTapToMarkdown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DocDocumentServiceImpl implements DocDocumentService {

    private final DocObjectMapper docObjectMapper;
    private final DocAccessService docAccessService;
    private final DocJsonSupport docJsonSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocDocumentRespVO getDocument(Long objectId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocObjectDO row = docAccessService.requireOwnedRichDoc(objectId, userId);

        OffsetDateTime now = OffsetDateTime.now();
        row.setLastOpenedAt(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        docObjectMapper.updateById(row);

        return toDocumentResp(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocDocumentBodySaveRespVO saveBody(Long objectId, DocDocumentBodySaveReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocObjectDO row = docAccessService.requireOwnedRichDoc(objectId, userId);

        Integer expectedVersion = request.getContentVersion();
        if (expectedVersion == null || !Objects.equals(row.getContentVersion(), expectedVersion)) {
            throw new ServiceException(ErrorCodeConstants.DOC_VERSION_CONFLICT);
        }

        docJsonSupport.requireTipTapDoc(request.getBody());
        if (!DocConstants.BODY_FORMAT_TIPTAP_JSON_V1.equals(row.getBodyFormat())) {
            throw new ServiceException(ErrorCodeConstants.DOC_TYPE_UNSUPPORTED);
        }

        String bodyJson = docJsonSupport.writeBody(request.getBody());
        OffsetDateTime now = OffsetDateTime.now();
        int nextVersion = row.getContentVersion() + 1;

        // Entity update applies JsonbTypeHandler; wrapper.set(String) binds varchar and fails on jsonb.
        DocObjectDO patch = new DocObjectDO();
        patch.setBody(bodyJson);
        patch.setContentVersion(nextVersion);
        patch.setUpdater(userId);
        patch.setUpdateTime(now);

        int updated = docObjectMapper.update(
                patch,
                Wrappers.<DocObjectDO>lambdaUpdate()
                        .eq(DocObjectDO::getId, objectId)
                        .eq(DocObjectDO::getOwnerUserId, userId)
                        .eq(DocObjectDO::getContentVersion, expectedVersion));
        if (updated == 0) {
            throw new ServiceException(ErrorCodeConstants.DOC_VERSION_CONFLICT);
        }

        DocDocumentBodySaveRespVO resp = new DocDocumentBodySaveRespVO();
        resp.setContentVersion(nextVersion);
        return resp;
    }

    @Override
    public DocExportMdRespVO exportMarkdown(Long objectId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocObjectDO row = docAccessService.requireOwnedRichDoc(objectId, userId);

        DocExportMdRespVO resp = new DocExportMdRespVO();
        resp.setMarkdown(TipTapToMarkdown.convert(docJsonSupport.parseBody(row.getBody()), row.getTitle()));
        return resp;
    }

    @Override
    public List<DocRecentItemRespVO> listRecent(int limit) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        int effectiveLimit = clampLimit(limit);

        return docObjectMapper.selectList(
                        Wrappers.<DocObjectDO>lambdaQuery()
                                .eq(DocObjectDO::getOwnerUserId, userId)
                                .eq(DocObjectDO::getType, DocConstants.OBJECT_TYPE_RICH_DOC)
                                .last("ORDER BY last_opened_at DESC NULLS LAST LIMIT " + effectiveLimit))
                .stream()
                .sorted(Comparator
                        .comparing(DocObjectDO::getLastOpenedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(effectiveLimit)
                .map(this::toRecentItem)
                .toList();
    }

    private DocDocumentRespVO toDocumentResp(DocObjectDO row) {
        DocDocumentRespVO vo = new DocDocumentRespVO();
        vo.setObjectId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setType(row.getType());
        vo.setBody(docJsonSupport.parseBodyMap(row.getBody()));
        vo.setBodyFormat(row.getBodyFormat());
        vo.setContentVersion(row.getContentVersion());
        vo.setLastOpenedAt(row.getLastOpenedAt());
        return vo;
    }

    private DocRecentItemRespVO toRecentItem(DocObjectDO row) {
        DocRecentItemRespVO vo = new DocRecentItemRespVO();
        vo.setObjectId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setLastOpenedAt(row.getLastOpenedAt());
        return vo;
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 50);
    }
}
