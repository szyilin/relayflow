package com.relayflow.module.task.dal.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Custom task_item queries (not codegen). Quick-view ALL visibility union.
 */
@Mapper
public interface TaskItemExtMapper {

    IPage<TaskItemDO> selectVisibleUnionPage(
            Page<TaskItemDO> page,
            @Param("userId") Long userId,
            @Param("status") String status);
}
