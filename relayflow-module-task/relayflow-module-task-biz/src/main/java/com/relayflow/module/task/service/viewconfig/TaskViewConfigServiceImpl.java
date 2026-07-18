package com.relayflow.module.task.service.viewconfig;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.task.controller.app.vo.TaskViewConfigSaveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskViewConfigVO;
import com.relayflow.module.task.dal.dataobject.TaskListMemberDO;
import com.relayflow.module.task.dal.dataobject.TaskViewConfigDO;
import com.relayflow.module.task.dal.mapper.TaskViewConfigMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskListRole;
import com.relayflow.module.task.enums.TaskViewContextType;
import com.relayflow.module.task.service.access.TaskListAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskViewConfigServiceImpl implements TaskViewConfigService {

    private final TaskViewConfigMapper taskViewConfigMapper;
    private final TaskListAccessService taskListAccessService;
    private final ObjectMapper objectMapper;

    @Override
    public TaskViewConfigVO get(String contextType, Long contextId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        String type = requireValidType(contextType);
        if (TaskViewContextType.isList(type)) {
            requireListId(contextId);
            taskListAccessService.requireReadable(contextId, userId);
            TaskViewConfigDO row = findListRow(contextId);
            return row == null ? defaultConfig(type) : parseConfig(row.getConfigJson(), type);
        }
        TaskViewConfigDO row = findPersonalRow(type, userId);
        return row == null ? defaultConfig(type) : parseConfig(row.getConfigJson(), type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(TaskViewConfigSaveReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        String type = requireValidType(request.getContextType());
        TaskViewConfigVO config = request.getConfig();
        if (config == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN);
        }
        validateGroupBy(type, config.getGroupBy());
        String json = writeConfigJson(normalizeConfig(config, type));

        OffsetDateTime now = OffsetDateTime.now();
        if (TaskViewContextType.isList(type)) {
            Long listId = requireListId(request.getContextId());
            TaskListMemberDO member = taskListAccessService.requireMembership(listId, userId);
            if (!TaskListRole.canMutateTasks(member.getRole())) {
                throw new ServiceException(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN);
            }
            TaskViewConfigDO existing = findListRow(listId);
            if (existing == null) {
                TaskViewConfigDO row = new TaskViewConfigDO();
                row.setTenantId(tenantId);
                row.setContextType(TaskViewContextType.LIST);
                row.setContextId(listId);
                row.setOwnerUserId(null);
                row.setConfigJson(json);
                row.setCreator(userId);
                row.setCreateTime(now);
                row.setUpdater(userId);
                row.setUpdateTime(now);
                taskViewConfigMapper.insert(row);
            } else {
                existing.setConfigJson(json);
                existing.setUpdater(userId);
                existing.setUpdateTime(now);
                taskViewConfigMapper.updateById(existing);
            }
            return;
        }

        TaskViewConfigDO existing = findPersonalRow(type, userId);
        if (existing == null) {
            TaskViewConfigDO row = new TaskViewConfigDO();
            row.setTenantId(tenantId);
            row.setContextType(type);
            row.setContextId(null);
            row.setOwnerUserId(userId);
            row.setConfigJson(json);
            row.setCreator(userId);
            row.setCreateTime(now);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            taskViewConfigMapper.insert(row);
        } else {
            existing.setConfigJson(json);
            existing.setUpdater(userId);
            existing.setUpdateTime(now);
            taskViewConfigMapper.updateById(existing);
        }
    }

    private TaskViewConfigDO findPersonalRow(String type, Long userId) {
        return taskViewConfigMapper.selectOne(
                Wrappers.<TaskViewConfigDO>lambdaQuery()
                        .eq(TaskViewConfigDO::getContextType, type)
                        .eq(TaskViewConfigDO::getOwnerUserId, userId)
                        .isNull(TaskViewConfigDO::getContextId)
                        .last("LIMIT 1"));
    }

    private TaskViewConfigDO findListRow(Long listId) {
        return taskViewConfigMapper.selectOne(
                Wrappers.<TaskViewConfigDO>lambdaQuery()
                        .eq(TaskViewConfigDO::getContextType, TaskViewContextType.LIST)
                        .eq(TaskViewConfigDO::getContextId, listId)
                        .isNull(TaskViewConfigDO::getOwnerUserId)
                        .last("LIMIT 1"));
    }

    private static String requireValidType(String contextType) {
        String type = TaskViewContextType.normalize(contextType);
        if (!TaskViewContextType.isValid(type)) {
            throw new ServiceException(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN);
        }
        return type;
    }

    private static Long requireListId(Long contextId) {
        if (contextId == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FORBIDDEN);
        }
        return contextId;
    }

    private void validateGroupBy(String type, Object groupBy) {
        if (groupBy == null) {
            return;
        }
        if (!(groupBy instanceof Map<?, ?> map)) {
            throw new ServiceException(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN);
        }
        Object mode = map.get("mode");
        if (!(mode instanceof String modeStr)) {
            throw new ServiceException(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN);
        }
        if ("PERSONAL_CUSTOM".equals(modeStr) && !TaskViewContextType.MINE.equals(type)) {
            throw new ServiceException(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN);
        }
        if ("LIST_GROUP".equals(modeStr) && !TaskViewContextType.LIST.equals(type)) {
            throw new ServiceException(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN);
        }
        if ("FIELD".equals(modeStr)) {
            Object fieldKey = map.get("fieldKey");
            if (!(fieldKey instanceof String key)
                    || !List.of("status", "dueTime", "assigneeId").contains(key)) {
                throw new ServiceException(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN);
            }
        }
    }

    private TaskViewConfigVO normalizeConfig(TaskViewConfigVO input, String type) {
        TaskViewConfigVO defaults = defaultConfig(type);
        TaskViewConfigVO out = new TaskViewConfigVO();
        out.setDisplayMode(StringUtils.hasText(input.getDisplayMode())
                ? input.getDisplayMode().trim().toUpperCase()
                : defaults.getDisplayMode());
        if (!"LIST".equals(out.getDisplayMode()) && !"BOARD".equals(out.getDisplayMode())) {
            out.setDisplayMode(defaults.getDisplayMode());
        }
        out.setGroupBy(input.getGroupBy());
        out.setSort(input.getSort() != null ? input.getSort() : defaults.getSort());
        out.setFilters(input.getFilters() != null ? input.getFilters() : List.of());
        out.setVisibleFieldKeys(input.getVisibleFieldKeys() != null && !input.getVisibleFieldKeys().isEmpty()
                ? input.getVisibleFieldKeys()
                : defaults.getVisibleFieldKeys());
        return out;
    }

    static TaskViewConfigVO defaultConfig(String type) {
        TaskViewConfigVO vo = new TaskViewConfigVO();
        vo.setDisplayMode("LIST");
        vo.setFilters(List.of());
        vo.setVisibleFieldKeys(List.of("dueTime", "assignee", "status"));
        if (TaskViewContextType.LIST.equals(type)) {
            Map<String, Object> groupBy = new LinkedHashMap<>();
            groupBy.put("mode", "FIELD");
            groupBy.put("fieldKey", "status");
            vo.setGroupBy(groupBy);
            vo.setSort("MANUAL");
        } else {
            vo.setGroupBy(null);
            Map<String, Object> sort = new LinkedHashMap<>();
            sort.put("key", "createTime");
            sort.put("order", "DESC");
            vo.setSort(sort);
        }
        return vo;
    }

    private TaskViewConfigVO parseConfig(String json, String type) {
        if (!StringUtils.hasText(json)) {
            return defaultConfig(type);
        }
        try {
            TaskViewConfigVO vo = objectMapper.readValue(json, TaskViewConfigVO.class);
            return normalizeConfig(vo, type);
        } catch (JsonProcessingException ex) {
            return defaultConfig(type);
        }
    }

    private String writeConfigJson(TaskViewConfigVO config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(ErrorCodeConstants.TASK_VIEW_CONFIG_FORBIDDEN);
        }
    }
}
