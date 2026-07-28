package com.relayflow.module.agent.service.session;

import com.relayflow.module.agent.controller.app.session.vo.AgentSessionCreateReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionMessageRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendRespVO;

import java.util.List;

public interface AgentSessionService {

    Long create(AgentSessionCreateReqVO request);

    List<AgentSessionRespVO> listMine();

    AgentSessionRespVO getMine(Long id);

    List<AgentSessionMessageRespVO> listMessages(Long sessionId);

    AgentSessionSendRespVO send(AgentSessionSendReqVO request);
}
