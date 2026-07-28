package com.relayflow.module.agent.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.agent.dal.dataobject.AgentDefDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Configurable Agent definition; model_ref -> agent_llm_connection.code Mapper 接口
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Mapper
public interface AgentDefMapper extends BaseMapper<AgentDefDO> {

}

