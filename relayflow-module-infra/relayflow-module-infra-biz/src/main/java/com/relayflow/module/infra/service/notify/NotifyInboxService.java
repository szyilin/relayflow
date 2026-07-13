package com.relayflow.module.infra.service.notify;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;
import com.relayflow.module.infra.dal.dataobject.InfraNotifyDO;

import java.util.List;
import java.util.Map;

public interface NotifyInboxService {

    Long push(NotifyItemCommand command);

    void backfillUserIdByMobile(String mobile, Long userId);

    List<InfraNotifyDO> listUnreadByMobile(String mobile);

    List<InfraNotifyDO> listByUserId(Long userId);

    long countUnreadByUserId(Long userId);

    PageResult<InfraNotifyDO> pageByUserId(Long userId, String type, int pageNo, int pageSize);

    void markReadByIds(Long userId, List<Long> ids);

    void markAllReadByUserId(Long userId, String type);

    Map<String, Long> countUnreadGroupByType(Long userId);

    boolean hasUnreadDedupe(Long tenantId, Long userId, String type, String dedupeKey);
}
