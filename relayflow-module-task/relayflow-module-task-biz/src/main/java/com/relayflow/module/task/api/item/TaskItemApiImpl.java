package com.relayflow.module.task.api.item;

import com.relayflow.module.task.api.item.dto.TaskDueRangeRespDTO;
import com.relayflow.module.task.api.item.dto.TaskSearchRespDTO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.service.item.TaskItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskItemApiImpl implements TaskItemApi {

    private final TaskItemService taskItemService;

    @Override
    public List<TaskSearchRespDTO> searchTasks(Long tenantId, Long userId, String keyword, int limit) {
        if (tenantId == null || userId == null || !StringUtils.hasText(keyword)) {
            return List.of();
        }
        return taskItemService.searchMyTasks(userId, keyword.trim(), limit).stream()
                .map(this::toSearchDto)
                .toList();
    }

    @Override
    public List<TaskDueRangeRespDTO> listDueRange(Long tenantId, Long userId,
                                                  OffsetDateTime from, OffsetDateTime to, int limit) {
        if (tenantId == null || userId == null || from == null || to == null) {
            return List.of();
        }
        return taskItemService.listDueRange(userId, from, to, limit).stream()
                .map(this::toDueRangeDto)
                .toList();
    }

    private TaskSearchRespDTO toSearchDto(TaskItemRespVO item) {
        TaskSearchRespDTO dto = new TaskSearchRespDTO();
        dto.setTaskId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setStatus(item.getStatus());
        return dto;
    }

    private TaskDueRangeRespDTO toDueRangeDto(TaskItemRespVO item) {
        TaskDueRangeRespDTO dto = new TaskDueRangeRespDTO();
        dto.setTaskId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setStatus(item.getStatus());
        dto.setDueTime(item.getDueTime());
        return dto;
    }
}
