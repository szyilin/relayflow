package com.relayflow.module.im.service.presence;

import com.relayflow.module.im.controller.app.vo.PresenceBatchRespVO;

import java.util.List;

public interface ImPresenceService {

    PresenceBatchRespVO batchPresence(List<Long> userIds);
}
