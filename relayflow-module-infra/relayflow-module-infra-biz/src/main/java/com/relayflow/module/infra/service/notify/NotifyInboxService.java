package com.relayflow.module.infra.service.notify;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;
import com.relayflow.module.infra.dal.dataobject.InfraNotifyDO;

import java.util.List;

public interface NotifyInboxService {

    Long push(NotifyItemCommand command);

    void backfillUserIdByMobile(String mobile, Long userId);

    List<InfraNotifyDO> listUnreadByMobile(String mobile);

    List<InfraNotifyDO> listByUserId(Long userId);

    long countUnreadByUserId(Long userId);

    PageResult<InfraNotifyDO> pageByUserId(Long userId, int pageNo, int pageSize);

    void markReadByIds(Long userId, List<Long> ids);
}
