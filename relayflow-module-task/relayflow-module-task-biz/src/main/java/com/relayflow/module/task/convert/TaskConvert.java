package com.relayflow.module.task.convert;

import com.relayflow.module.task.api.item.dto.TaskDueRangeRespDTO;
import com.relayflow.module.task.api.item.dto.TaskSearchRespDTO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;

@Mapper
public interface TaskConvert {

    TaskConvert INSTANCE = Mappers.getMapper(TaskConvert.class);

    TaskItemRespVO toResp(TaskItemDO row);

    @Mapping(target = "taskId", source = "id")
    TaskSearchRespDTO toSearchDto(TaskItemDO row);

    @Mapping(target = "taskId", source = "id")
    TaskDueRangeRespDTO toDueRangeDto(TaskItemDO row);

    default List<TaskItemRespVO> toRespList(List<TaskItemDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(this::toResp).toList();
    }
}
