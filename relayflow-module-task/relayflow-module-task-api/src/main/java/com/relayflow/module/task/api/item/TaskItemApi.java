package com.relayflow.module.task.api.item;

import com.relayflow.module.task.api.item.dto.TaskDueRangeRespDTO;
import com.relayflow.module.task.api.item.dto.TaskSearchRespDTO;

import java.time.OffsetDateTime;
import java.util.List;

public interface TaskItemApi {

    List<TaskSearchRespDTO> searchTasks(Long tenantId, Long userId, String keyword, int limit);

    /**
     * TODO tasks assigned to {@code userId} with dueTime in {@code [from, to)}.
     * Callers MUST pass the intended assignee; do not use to read another user's tasks.
     */
    List<TaskDueRangeRespDTO> listDueRange(Long tenantId, Long userId,
                                           OffsetDateTime from, OffsetDateTime to, int limit);
}
