package com.relayflow.module.docs.service.star;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.docs.controller.app.vo.DocStarredItemRespVO;
import com.relayflow.module.docs.controller.app.vo.DocStarredListRespVO;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.dataobject.DocStarDO;
import com.relayflow.module.docs.dal.mapper.DocStarMapper;
import com.relayflow.module.docs.enums.DocConstants;
import com.relayflow.module.docs.service.access.DocAccessService;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocStarServiceImpl implements DocStarService {

    private final DocStarMapper docStarMapper;
    private final DocAccessService docAccessService;
    private final UserApi userApi;

    @Override
    public DocStarredListRespVO listStarred(int limit) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        int effective = clampLimit(limit);
        List<DocStarDO> stars = docStarMapper.selectList(
                Wrappers.<DocStarDO>lambdaQuery()
                        .eq(DocStarDO::getUserId, userId)
                        .orderByDesc(DocStarDO::getCreateTime)
                        .last("LIMIT " + Math.max(effective * 2, effective)));

        List<DocStarredItemRespVO> items = new ArrayList<>();
        for (DocStarDO star : stars) {
            try {
                DocAccessService.AccessResult access =
                        docAccessService.requireReadableRichDoc(star.getObjectId(), userId);
                DocObjectDO object = access.getObject();
                DocStarredItemRespVO item = new DocStarredItemRespVO();
                item.setObjectId(object.getId());
                item.setTitle(object.getTitle());
                item.setType(object.getType());
                item.setStarredAt(star.getCreateTime());
                item.setAccessState(DocConstants.ACCESS_STATE_OK);
                item.setOwnerUserId(object.getOwnerUserId());
                item.setOwnerDisplayName(resolveNickname(object.getOwnerUserId()));
                item.setLocationLabel(docAccessService.resolveLocationLabel(object, userId, false));
                items.add(item);
                if (items.size() >= effective) {
                    break;
                }
            } catch (Exception ignored) {
                // V1: omit inaccessible stars
            }
        }
        DocStarredListRespVO resp = new DocStarredListRespVO();
        resp.setItems(items);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void star(Long objectId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        docAccessService.requireReadableRichDoc(objectId, userId);

        DocStarDO existing = docStarMapper.selectOne(
                Wrappers.<DocStarDO>lambdaQuery()
                        .eq(DocStarDO::getUserId, userId)
                        .eq(DocStarDO::getObjectId, objectId)
                        .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        DocStarDO row = new DocStarDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setObjectId(objectId);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        docStarMapper.insert(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unstar(Long objectId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        DocStarDO existing = docStarMapper.selectOne(
                Wrappers.<DocStarDO>lambdaQuery()
                        .eq(DocStarDO::getUserId, userId)
                        .eq(DocStarDO::getObjectId, objectId)
                        .last("LIMIT 1"));
        if (existing != null) {
            docStarMapper.deleteById(existing.getId());
        }
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

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }
}
