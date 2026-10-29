package com.relayflow.module.im.service.message;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.infra.api.file.*;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.service.conversation.ImConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ImFileAccessPolicy implements FileAccessPolicy, FileHistorySource {
    private final ImMessageMapper messages;
    private final ImConversationService conversations;
    private final ImContentHelper content;
    private final FileApi files;
    public String bizType() { return "im_message"; }
    public boolean canRead(Long tenantId, Long userId, Long fileId, Long bizId) {
        var message = messages.selectById(bizId);
        if (!valid(message, tenantId, fileId)) return false;
        conversations.requireMembership(tenantId, message.getConversationId(), userId);
        return true;
    }
    private boolean valid(ImMessageDO message, Long tenantId, Long fileId) {
        return message != null && Objects.equals(message.getTenantId(), tenantId) && message.getRecalledAt() == null
                && content.fileIds(content.fromJson(message.getContentJson())).contains(fileId);
    }
    public Page scan(Long tenantId, long afterId, int limit) {
        var rows = messages.selectList(Wrappers.<ImMessageDO>lambdaQuery().eq(ImMessageDO::getTenantId, tenantId)
                .gt(ImMessageDO::getId, afterId).orderByAsc(ImMessageDO::getId).last("LIMIT " + Math.clamp(limit, 1, 500)));
        List<Reference> refs = new ArrayList<>();
        for (var row : rows) if (row.getRecalledAt() == null) {
            try {
                for (var id : content.fileIds(content.fromJson(row.getContentJson()))) refs.add(new Reference(row.getId(), id, row.getSenderId()));
            } catch (RuntimeException ex) { refs.add(new Reference(row.getId(), null, row.getSenderId())); }
        }
        return new Page(rows.isEmpty() ? afterId : rows.getLast().getId(), rows.size() < limit, refs);
    }
    public boolean trusted(Long tenantId, Reference ref) {
        var file = files.getFile(ref.fileId());
        if (!Objects.equals(file.getTenantId(), tenantId) || file.getCreator() == null) return false;
        Long id = ref.bizId();
        Set<Long> seen = new HashSet<>();
        while (id != null && seen.size() < 32 && seen.add(id)) {
            var row = messages.selectById(id);
            if (!valid(row, tenantId, ref.fileId())) return false;
            if ("user".equals(row.getSenderType()) && Objects.equals(row.getSenderId(), file.getCreator())) return true;
            var from = content.fromJson(row.getContentJson()).getForwardedFrom();
            id = from == null ? null : from.getMessageId();
            if (id != null) {
                var origin = messages.selectById(id);
                if (!valid(origin, tenantId, ref.fileId()) || !"user".equals(row.getSenderType())) return false;
                conversations.requireMembership(tenantId, origin.getConversationId(), row.getSenderId());
            }
        }
        return false;
    }
}
