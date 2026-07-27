package com.relayflow.module.docs.service.star;

import com.relayflow.module.docs.controller.app.vo.DocStarredListRespVO;

public interface DocStarService {

    DocStarredListRespVO listStarred(int limit);

    void star(Long objectId);

    void unstar(Long objectId);
}
