package com.relayflow.module.system.service.contact;

import com.relayflow.module.system.controller.app.vo.AppContactExternalItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactRequestItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactStarredItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactStarredUpdateReqVO;

import java.util.List;

public interface ContactEnrichService {

    List<AppContactExternalItemRespVO> listExternal();

    List<AppContactRequestItemRespVO> listRequests();

    AppContactRequestItemRespVO acceptRequest(Long requestId);

    List<AppContactStarredItemRespVO> listStarred();

    void updateStarred(Long userId, AppContactStarredUpdateReqVO request);
}
