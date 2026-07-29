package com.relayflow.module.agent.service.session;

import com.relayflow.module.agent.controller.app.session.vo.AgentSessionCreateReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionMessageRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionRespVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendReqVO;
import com.relayflow.module.agent.controller.app.session.vo.AgentSessionSendRespVO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface AgentSessionService {

    Long create(AgentSessionCreateReqVO request);

    List<AgentSessionRespVO> listMine();

    AgentSessionRespVO getMine(Long id);

    List<AgentSessionMessageRespVO> listMessages(Long sessionId);

    AgentSessionSendRespVO send(AgentSessionSendReqVO request);

    /**
     * Stream a turn. Emits contract JSON maps (type + fields) via {@code onEvent}.
     * Runs synchronously on the calling thread (controller should offload).
     */
    void sendStream(AgentSessionSendReqVO request, Consumer<Map<String, Object>> onEvent, AtomicBoolean cancelFlag);
}
