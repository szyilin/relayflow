package com.relayflow.module.agent.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.agent.dal.dataobject.AgentSessionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent workspace chat session (not IM conversation) Mapper 接口
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSessionDO> {

}

