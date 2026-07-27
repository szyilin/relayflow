package com.relayflow.module.docs.service.share;

import com.relayflow.module.docs.controller.app.vo.DocShareCreateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocShareItemRespVO;
import com.relayflow.module.docs.controller.app.vo.DocShareListRespVO;
import com.relayflow.module.docs.controller.app.vo.DocShareUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocSharedListRespVO;

public interface DocShareService {

    DocShareListRespVO listShares(Long objectId);

    DocShareItemRespVO createShare(Long objectId, DocShareCreateReqVO request);

    DocShareItemRespVO updateShare(Long objectId, Long shareId, DocShareUpdateReqVO request);

    void revokeShare(Long objectId, Long shareId);

    DocSharedListRespVO listSharedWithMe(int limit);

    void leaveShared(Long objectId);
}
