package com.relayflow.module.task.api.item;

import com.relayflow.module.task.api.item.dto.TaskSearchRespDTO;

import java.util.List;

public interface TaskItemApi {

    List<TaskSearchRespDTO> searchTasks(Long tenantId, Long userId, String keyword, int limit);
}
