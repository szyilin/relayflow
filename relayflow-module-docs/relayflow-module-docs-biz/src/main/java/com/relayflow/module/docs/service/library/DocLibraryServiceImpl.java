package com.relayflow.module.docs.service.library;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeCreateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeSummaryRespVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeTreeVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryTreeRespVO;
import com.relayflow.module.docs.dal.dataobject.DocLibraryNodeDO;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.mapper.DocLibraryNodeMapper;
import com.relayflow.module.docs.dal.mapper.DocObjectMapper;
import com.relayflow.module.docs.enums.DocConstants;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import com.relayflow.module.docs.service.access.DocAccessService;
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
public class DocLibraryServiceImpl implements DocLibraryService {

    private final DocLibraryNodeMapper docLibraryNodeMapper;
    private final DocObjectMapper docObjectMapper;
    private final DocAccessService docAccessService;

    @Override
    public DocLibraryTreeRespVO getTree() {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        List<DocLibraryNodeDO> nodes = docLibraryNodeMapper.selectList(
                Wrappers.<DocLibraryNodeDO>lambdaQuery()
                        .eq(DocLibraryNodeDO::getOwnerUserId, userId)
                        .orderByAsc(DocLibraryNodeDO::getSortOrder)
                        .orderByAsc(DocLibraryNodeDO::getId));
        if (nodes.isEmpty()) {
            DocLibraryTreeRespVO empty = new DocLibraryTreeRespVO();
            empty.setNodes(List.of());
            return empty;
        }

        Set<Long> objectIds = nodes.stream()
                .map(DocLibraryNodeDO::getObjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DocObjectDO> objectsById = docObjectMapper.selectBatchIds(objectIds).stream()
                .collect(Collectors.toMap(DocObjectDO::getId, row -> row));

        Map<Long, List<DocLibraryNodeDO>> childrenByParent = new HashMap<>();
        for (DocLibraryNodeDO node : nodes) {
            Long parentKey = node.getParentId() == null ? 0L : node.getParentId();
            childrenByParent.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(node);
        }
        for (List<DocLibraryNodeDO> siblings : childrenByParent.values()) {
            siblings.sort(Comparator
                    .comparing(DocLibraryNodeDO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(DocLibraryNodeDO::getId));
        }

        DocLibraryTreeRespVO resp = new DocLibraryTreeRespVO();
        resp.setNodes(buildTreeLevel(childrenByParent, objectsById, 0L));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocLibraryNodeSummaryRespVO createNode(DocLibraryNodeCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long parentId = request.getParentId();
        if (parentId != null) {
            docAccessService.requireOwnedNode(parentId, userId);
        }

        String title = resolveTitle(request.getTitle());
        OffsetDateTime now = OffsetDateTime.now();
        int sortOrder = nextSortOrder(userId, parentId);

        DocObjectDO object = new DocObjectDO();
        object.setTenantId(tenantId);
        object.setType(DocConstants.OBJECT_TYPE_RICH_DOC);
        object.setTitle(title);
        object.setBody(DocConstants.DEFAULT_BODY_JSON);
        object.setBodyFormat(DocConstants.BODY_FORMAT_TIPTAP_JSON_V1);
        object.setContentVersion(0);
        object.setOwnerUserId(userId);
        object.setCreator(userId);
        object.setCreateTime(now);
        object.setUpdater(userId);
        object.setUpdateTime(now);
        docObjectMapper.insert(object);

        DocLibraryNodeDO node = new DocLibraryNodeDO();
        node.setTenantId(tenantId);
        node.setOwnerUserId(userId);
        node.setParentId(parentId);
        node.setObjectId(object.getId());
        node.setSortOrder(sortOrder);
        node.setCreator(userId);
        node.setCreateTime(now);
        node.setUpdater(userId);
        node.setUpdateTime(now);
        docLibraryNodeMapper.insert(node);

        return toSummary(node, object);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocLibraryNodeSummaryRespVO updateNode(Long nodeId, DocLibraryNodeUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocLibraryNodeDO node = docAccessService.requireOwnedNode(nodeId, userId);
        DocObjectDO object = docAccessService.requireOwnedObject(node.getObjectId(), userId);

        boolean changed = false;
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
            changed = true;
        }

        if (request.isParentIdSpecified()) {
            Long newParentId = request.getParentId();
            if (!Objects.equals(node.getParentId(), newParentId)) {
                validateParentMove(userId, nodeId, newParentId);
                node.setParentId(newParentId);
                changed = true;
            }
        }

        if (request.getSortOrder() != null) {
            node.setSortOrder(request.getSortOrder());
            changed = true;
        }

        if (changed) {
            node.setUpdater(userId);
            node.setUpdateTime(now);
            docLibraryNodeMapper.updateById(node);
        }

        object = docObjectMapper.selectById(object.getId());
        return toSummary(node, object);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long nodeId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocLibraryNodeDO root = docAccessService.requireOwnedNode(nodeId, userId);

        List<DocLibraryNodeDO> allNodes = docLibraryNodeMapper.selectList(
                Wrappers.<DocLibraryNodeDO>lambdaQuery()
                        .eq(DocLibraryNodeDO::getOwnerUserId, userId));
        Map<Long, List<DocLibraryNodeDO>> childrenByParent = new HashMap<>();
        for (DocLibraryNodeDO node : allNodes) {
            Long parentKey = node.getParentId() == null ? 0L : node.getParentId();
            childrenByParent.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(node);
        }

        Set<Long> nodeIds = collectSubtreeNodeIds(nodeId, childrenByParent);
        for (Long id : nodeIds) {
            DocLibraryNodeDO node = allNodes.stream()
                    .filter(row -> Objects.equals(row.getId(), id))
                    .findFirst()
                    .orElse(null);
            if (node == null) {
                continue;
            }
            docLibraryNodeMapper.deleteById(id);
            if (node.getObjectId() != null) {
                docObjectMapper.deleteById(node.getObjectId());
            }
        }
    }

    private List<DocLibraryNodeTreeVO> buildTreeLevel(
            Map<Long, List<DocLibraryNodeDO>> childrenByParent,
            Map<Long, DocObjectDO> objectsById,
            Long parentKey) {
        List<DocLibraryNodeDO> siblings = childrenByParent.getOrDefault(parentKey, List.of());
        List<DocLibraryNodeTreeVO> result = new ArrayList<>(siblings.size());
        for (DocLibraryNodeDO node : siblings) {
            DocLibraryNodeTreeVO vo = new DocLibraryNodeTreeVO();
            vo.setNodeId(node.getId());
            vo.setParentId(node.getParentId());
            vo.setObjectId(node.getObjectId());
            vo.setSortOrder(node.getSortOrder());
            DocObjectDO object = objectsById.get(node.getObjectId());
            vo.setTitle(object != null ? object.getTitle() : DocConstants.DEFAULT_TITLE);
            vo.setChildren(buildTreeLevel(childrenByParent, objectsById, node.getId()));
            result.add(vo);
        }
        return result;
    }

    private DocLibraryNodeSummaryRespVO toSummary(DocLibraryNodeDO node, DocObjectDO object) {
        DocLibraryNodeSummaryRespVO vo = new DocLibraryNodeSummaryRespVO();
        vo.setNodeId(node.getId());
        vo.setObjectId(node.getObjectId());
        vo.setParentId(node.getParentId());
        vo.setSortOrder(node.getSortOrder());
        vo.setTitle(object.getTitle());
        vo.setContentVersion(object.getContentVersion());
        vo.setBodyFormat(object.getBodyFormat());
        return vo;
    }

    private static String resolveTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return DocConstants.DEFAULT_TITLE;
        }
        String trimmed = title.trim();
        return trimmed.isEmpty() ? DocConstants.DEFAULT_TITLE : trimmed;
    }

    private int nextSortOrder(Long userId, Long parentId) {
        return docLibraryNodeMapper.selectList(
                        Wrappers.<DocLibraryNodeDO>lambdaQuery()
                                .eq(DocLibraryNodeDO::getOwnerUserId, userId)
                                .eq(parentId == null, DocLibraryNodeDO::getParentId, (Object) null)
                                .eq(parentId != null, DocLibraryNodeDO::getParentId, parentId))
                .stream()
                .map(DocLibraryNodeDO::getSortOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(-1) + 1;
    }

    private void validateParentMove(Long userId, Long nodeId, Long newParentId) {
        if (newParentId == null) {
            return;
        }
        if (Objects.equals(nodeId, newParentId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_PARENT_INVALID);
        }
        DocLibraryNodeDO parent = docAccessService.requireOwnedNode(newParentId, userId);

        List<DocLibraryNodeDO> allNodes = docLibraryNodeMapper.selectList(
                Wrappers.<DocLibraryNodeDO>lambdaQuery()
                        .eq(DocLibraryNodeDO::getOwnerUserId, userId));
        Map<Long, List<DocLibraryNodeDO>> childrenByParent = new HashMap<>();
        for (DocLibraryNodeDO node : allNodes) {
            Long parentKey = node.getParentId() == null ? 0L : node.getParentId();
            childrenByParent.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(node);
        }
        Set<Long> descendants = collectSubtreeNodeIds(nodeId, childrenByParent);
        if (descendants.contains(newParentId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_PARENT_INVALID);
        }
        if (!Objects.equals(parent.getOwnerUserId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.DOC_PARENT_INVALID);
        }
    }

    private Set<Long> collectSubtreeNodeIds(Long rootId, Map<Long, List<DocLibraryNodeDO>> childrenByParent) {
        Set<Long> ids = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (!ids.add(current)) {
                continue;
            }
            for (DocLibraryNodeDO child : childrenByParent.getOrDefault(current, List.of())) {
                queue.add(child.getId());
            }
        }
        return ids;
    }
}
