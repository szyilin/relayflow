package com.relayflow.module.agent.service.llm;

import com.relayflow.module.agent.controller.admin.llm.vo.LlmConnectionRespVO;
import com.relayflow.module.agent.controller.admin.llm.vo.LlmConnectionSaveReqVO;
import com.relayflow.module.agent.controller.admin.llm.vo.LlmConnectionTestReqVO;
import com.relayflow.module.agent.controller.app.llm.vo.LlmEnabledModelRespVO;

import java.util.List;

public interface LlmConnectionService {

    List<LlmConnectionRespVO> list();

    LlmConnectionRespVO get(Long id);

    Long create(LlmConnectionSaveReqVO request);

    void update(LlmConnectionSaveReqVO request);

    void delete(Long id);

    void testConnection(LlmConnectionTestReqVO request);

    List<LlmEnabledModelRespVO> listEnabledModels();
}
