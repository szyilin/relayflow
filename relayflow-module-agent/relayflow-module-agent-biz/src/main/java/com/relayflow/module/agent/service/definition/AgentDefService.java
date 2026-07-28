package com.relayflow.module.agent.service.definition;

import com.relayflow.module.agent.controller.app.def.vo.AgentDefRespVO;
import com.relayflow.module.agent.controller.app.def.vo.AgentDefSaveReqVO;

import java.util.List;

public interface AgentDefService {

    List<AgentDefRespVO> listMine();

    AgentDefRespVO getMine(Long id);

    Long create(AgentDefSaveReqVO request);

    void update(AgentDefSaveReqVO request);

    void delete(Long id);
}
