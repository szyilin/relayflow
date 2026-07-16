package com.relayflow.module.im.service.card;

import com.relayflow.module.im.controller.app.vo.CardActionReqVO;
import com.relayflow.module.im.controller.app.vo.CardActionRespVO;

public interface CardActionIngress {

    CardActionRespVO handle(CardActionReqVO request);
}
