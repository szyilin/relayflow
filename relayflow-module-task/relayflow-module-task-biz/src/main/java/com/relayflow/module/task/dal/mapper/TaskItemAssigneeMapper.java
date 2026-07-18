package com.relayflow.module.task.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.task.dal.dataobject.TaskItemAssigneeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskItemAssigneeMapper extends BaseMapper<TaskItemAssigneeDO> {
}
