package com.relayflow.module.system.service.contactremark;

import com.relayflow.module.system.controller.app.vo.AppContactRemarkRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactRemarkUpdateReqVO;

public interface ContactRemarkService {

    AppContactRemarkRespVO getMyRemark(Long targetUserId);

    AppContactRemarkRespVO updateMyRemark(Long targetUserId, AppContactRemarkUpdateReqVO request);
}
