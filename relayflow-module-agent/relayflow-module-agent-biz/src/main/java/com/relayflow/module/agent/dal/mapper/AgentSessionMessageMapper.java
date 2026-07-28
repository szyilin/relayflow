package com.relayflow.module.agent.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.agent.dal.dataobject.AgentSessionMessageDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent session turns; never written to im_message by this path Mapper 接口
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-28
 */
@Mapper
public interface AgentSessionMessageMapper extends BaseMapper<AgentSessionMessageDO> {

}

