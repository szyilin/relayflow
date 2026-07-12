package com.relayflow.module.infra.service.notify;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;
import com.relayflow.module.infra.dal.dataobject.InfraNotifyDO;
import com.relayflow.module.infra.dal.mysql.InfraNotifyMapper;
import com.relayflow.module.infra.dal.mysql.InfraNotifyPublicMapper;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotifyInboxServiceImpl implements NotifyInboxService {

    private static final int READ_FLAG_UNREAD = 0;

    private final InfraNotifyMapper notifyMapper;
    private final InfraNotifyPublicMapper notifyPublicMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Long push(NotifyItemCommand command) {
        validateReceiver(command);
        InfraNotifyDO existing = findUnreadDuplicate(command);
        OffsetDateTime now = OffsetDateTime.now();
        if (existing != null) {
            existing.setTitle(command.getTitle());
            existing.setBody(command.getBody());
            existing.setPayloadJson(toPayloadJson(command.getPayload()));
            if (command.getUserId() != null) {
                existing.setUserId(command.getUserId());
            }
            if (StringUtils.hasText(command.getMobile())) {
                existing.setMobile(command.getMobile());
            }
            existing.setUpdateTime(now);
            notifyMapper.updateById(existing);
            return existing.getId();
        }

        InfraNotifyDO row = new InfraNotifyDO();
        row.setTenantId(command.getTenantId());
        row.setUserId(command.getUserId());
        row.setMobile(command.getMobile());
        row.setType(command.getType());
        row.setTitle(command.getTitle());
        row.setBody(command.getBody());
        row.setPayloadJson(toPayloadJson(command.getPayload()));
        row.setReadFlag(READ_FLAG_UNREAD);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        notifyMapper.insert(row);
        return row.getId();
    }

    @Override
    public void backfillUserIdByMobile(String mobile, Long userId) {
        if (!StringUtils.hasText(mobile) || userId == null) {
            return;
        }
        notifyPublicMapper.updateUserIdByMobile(mobile, userId, OffsetDateTime.now());
    }

    @Override
    public List<InfraNotifyDO> listUnreadByMobile(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return List.of();
        }
        return notifyPublicMapper.selectUnreadByMobile(mobile);
    }

    @Override
    public List<InfraNotifyDO> listByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return notifyPublicMapper.selectByUserId(userId);
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return notifyPublicMapper.countUnreadByUserId(userId);
    }

    @Override
    public PageResult<InfraNotifyDO> pageByUserId(Long userId, int pageNo, int pageSize) {
        if (userId == null) {
            return PageResult.empty();
        }
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        long offset = (long) (safePageNo - 1) * safePageSize;
        long total = notifyPublicMapper.countByUserId(userId);
        List<InfraNotifyDO> list = notifyPublicMapper.selectPageByUserId(userId, safePageSize, offset);
        return PageResult.of(list, total);
    }

    @Override
    public void markReadByIds(Long userId, List<Long> ids) {
        if (userId == null || ids == null || ids.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            notifyPublicMapper.markReadById(id, userId, now);
        }
    }

    private InfraNotifyDO findUnreadDuplicate(NotifyItemCommand command) {
        LambdaQueryWrapper<InfraNotifyDO> wrapper = new LambdaQueryWrapper<InfraNotifyDO>()
                .eq(InfraNotifyDO::getTenantId, command.getTenantId())
                .eq(InfraNotifyDO::getType, command.getType())
                .eq(InfraNotifyDO::getReadFlag, READ_FLAG_UNREAD)
                .last("LIMIT 1");
        if (command.getUserId() != null) {
            wrapper.eq(InfraNotifyDO::getUserId, command.getUserId());
        } else {
            wrapper.eq(InfraNotifyDO::getMobile, command.getMobile())
                    .isNull(InfraNotifyDO::getUserId);
        }
        return notifyMapper.selectOne(wrapper);
    }

    private void validateReceiver(NotifyItemCommand command) {
        if (command.getTenantId() == null) {
            throw new ServiceException(ErrorCodeConstants.NOTIFY_TENANT_REQUIRED);
        }
        if (!StringUtils.hasText(command.getType())) {
            throw new ServiceException(ErrorCodeConstants.NOTIFY_TYPE_REQUIRED);
        }
        if (!StringUtils.hasText(command.getTitle())) {
            throw new ServiceException(ErrorCodeConstants.NOTIFY_TITLE_REQUIRED);
        }
        if (command.getUserId() == null && !StringUtils.hasText(command.getMobile())) {
            throw new ServiceException(ErrorCodeConstants.NOTIFY_RECEIVER_REQUIRED);
        }
    }

    private String toPayloadJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(ErrorCodeConstants.NOTIFY_PAYLOAD_INVALID);
        }
    }
}
