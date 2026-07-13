package com.relayflow.module.task.api.item;

import com.relayflow.module.task.api.item.dto.TaskSearchRespDTO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.service.item.TaskItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
                .map(this::toDto)
                .toList();
    }

    private TaskSearchRespDTO toDto(TaskItemRespVO item) {
        TaskSearchRespDTO dto = new TaskSearchRespDTO();
        dto.setTaskId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setStatus(item.getStatus());
        return dto;
    }
}
