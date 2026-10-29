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
import com.relayflow.module.docs.dal.dataobject.DocStarDO;
import com.relayflow.module.docs.dal.dataobject.DocUserVisitDO;
import com.relayflow.module.docs.dal.mapper.DocObjectMapper;
import com.relayflow.module.docs.dal.mapper.DocStarMapper;
import com.relayflow.module.docs.dal.mapper.DocUserVisitMapper;
import com.relayflow.module.docs.enums.DocConstants;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import com.relayflow.module.docs.service.access.DocAccessService;
import com.relayflow.module.docs.service.productsettings.DocProductSettingsService;
import com.relayflow.module.docs.service.visit.DocVisitService;
import com.relayflow.module.docs.support.DocJsonSupport;
import com.relayflow.module.docs.support.TipTapToMarkdown;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocDocumentServiceImpl implements DocDocumentService {

    private final DocObjectMapper docObjectMapper;
    private final com.relayflow.module.docs.dal.mapper.DocObjectExtMapper docObjectExtMapper;
    private final DocUserVisitMapper docUserVisitMapper;
    private final DocStarMapper docStarMapper;
    private final DocAccessService docAccessService;
    private final DocVisitService docVisitService;
    private final DocJsonSupport docJsonSupport;
    private final DocProductSettingsService docProductSettingsService;
    private final UserApi userApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocDocumentRespVO getDocument(Long objectId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        DocAccessService.AccessResult access = docAccessService.requireReadableRichDoc(objectId, userId);
        DocObjectDO row = access.getObject();

        OffsetDateTime now = OffsetDateTime.now();
        if (access.isOwner()) {
            row.setLastOpenedAt(now);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            docObjectExtMapper.touchOpened(tenantId, objectId, userId, now);
        }
        docVisitService.upsertVisit(tenantId, userId, row, access.isOwner());

        return toDocumentResp(row, access);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocDocumentBodySaveRespVO saveBody(Long objectId, DocDocumentBodySaveReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocAccessService.AccessResult access = docAccessService.requireEditableRichDoc(objectId, userId);
        DocObjectDO row = access.getObject();

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

        int updated = docObjectExtMapper.saveBody(SecurityFrameworkUtils.requireLoginTenantId(),
                objectId, expectedVersion, bodyJson, userId, now);
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
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        if (!docProductSettingsService.resolveMarkdownExportEnabled(tenantId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_MARKDOWN_EXPORT_DISABLED);
        }
        DocAccessService.AccessResult access = docAccessService.requireReadableRichDoc(objectId, userId);
        DocObjectDO row = access.getObject();

        DocExportMdRespVO resp = new DocExportMdRespVO();
        resp.setMarkdown(TipTapToMarkdown.convert(docJsonSupport.parseBody(row.getBody()), row.getTitle()));
        return resp;
    }

    @Override
    public List<DocRecentItemRespVO> listRecent(int limit) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        int effectiveLimit = clampLimit(limit);

        List<DocUserVisitDO> visits = docUserVisitMapper.selectList(
                Wrappers.<DocUserVisitDO>lambdaQuery()
                        .eq(DocUserVisitDO::getUserId, userId)
                        .orderByDesc(DocUserVisitDO::getLastOpenedAt)
                        .last("LIMIT " + effectiveLimit));

        Set<Long> objectIds = visits.stream().map(DocUserVisitDO::getObjectId).collect(Collectors.toCollection(HashSet::new));
        Set<Long> starredIds = objectIds.isEmpty()
                ? Set.of()
                : docStarMapper.selectList(
                                Wrappers.<DocStarDO>lambdaQuery()
                                        .eq(DocStarDO::getUserId, userId)
                                        .in(DocStarDO::getObjectId, objectIds))
                        .stream()
                        .map(DocStarDO::getObjectId)
                        .collect(Collectors.toCollection(HashSet::new));

        List<DocRecentItemRespVO> result = new ArrayList<>();
        for (DocUserVisitDO visit : visits) {
            result.add(toRecentItem(visit, userId, starredIds.contains(visit.getObjectId())));
        }
        return result.stream()
                .sorted(Comparator.comparing(DocRecentItemRespVO::getLastOpenedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRecent(Long objectId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        docVisitService.removeVisit(userId, objectId);
    }

    private DocDocumentRespVO toDocumentResp(DocObjectDO row, DocAccessService.AccessResult access) {
        DocDocumentRespVO vo = new DocDocumentRespVO();
        vo.setObjectId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setType(row.getType());
        vo.setBody(docJsonSupport.parseBodyMap(row.getBody()));
        vo.setBodyFormat(row.getBodyFormat());
        vo.setContentVersion(row.getContentVersion());
        vo.setLastOpenedAt(row.getLastOpenedAt());
        vo.setOwnerUserId(row.getOwnerUserId());
        vo.setOwnedByMe(access.isOwner());
        if (access.isOwner() || access.getLevel() == DocAccessService.AccessLevel.EDIT) {
            vo.setPermission(DocConstants.SHARE_PERMISSION_EDIT);
        } else {
            vo.setPermission(DocConstants.SHARE_PERMISSION_VIEW);
        }
        return vo;
    }

    private DocRecentItemRespVO toRecentItem(DocUserVisitDO visit, Long userId, boolean starred) {
        DocRecentItemRespVO vo = new DocRecentItemRespVO();
        vo.setObjectId(visit.getObjectId());
        vo.setTitleSnapshot(visit.getTitleSnapshot());
        vo.setLastOpenedAt(visit.getLastOpenedAt());
        vo.setStarred(starred);
        vo.setType(DocConstants.OBJECT_TYPE_RICH_DOC);

        DocObjectDO object = docObjectMapper.selectById(visit.getObjectId());
        if (object == null) {
            vo.setTitle(visit.getTitleSnapshot());
            vo.setAccessState(DocConstants.ACCESS_STATE_DELETED);
            vo.setLocationLabel(DocConstants.LOCATION_DELETED);
            vo.setOwnedByMe(DocConstants.VISIT_SOURCE_OWNER.equals(visit.getSource()));
            if (vo.getOwnedByMe()) {
                vo.setOwnerUserId(userId);
                vo.setOwnerDisplayName(resolveNickname(userId));
                vo.setOwnerAvatarFileId(resolveAvatarFileId(userId));
            }
            return vo;
        }

        vo.setTitle(object.getTitle());
        vo.setTitleSnapshot(StringUtils.hasText(visit.getTitleSnapshot()) ? visit.getTitleSnapshot() : object.getTitle());
        vo.setType(object.getType());
        vo.setOwnerUserId(object.getOwnerUserId());
        vo.setCreateTime(object.getCreateTime());
        boolean owned = Objects.equals(object.getOwnerUserId(), userId);
        vo.setOwnedByMe(owned);
        vo.setOwnerDisplayName(resolveNickname(object.getOwnerUserId()));
        vo.setOwnerAvatarFileId(resolveAvatarFileId(object.getOwnerUserId()));

        try {
            docAccessService.resolveAccess(object.getId(), userId);
            vo.setAccessState(DocConstants.ACCESS_STATE_OK);
            vo.setLocationLabel(docAccessService.resolveLocationLabel(object, userId, false));
        } catch (ServiceException ex) {
            vo.setAccessState(DocConstants.ACCESS_STATE_NO_ACCESS);
            vo.setLocationLabel(DocConstants.LOCATION_SHARED);
        }
        return vo;
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
            return 20;
        }
        return Math.min(limit, 50);
    }
}
