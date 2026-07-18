package com.relayflow.module.task.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.task.dal.dataobject.TaskListItemDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskListItemMapper extends BaseMapper<TaskListItemDO> {
}
