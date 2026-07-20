package com.relayflow.module.task.service.listfield;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.task.controller.app.vo.TaskListFieldCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldOptionCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldOptionRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldOptionUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldValuePutReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldValueRespVO;
import com.relayflow.module.task.dal.dataobject.TaskItemFieldValueDO;
import com.relayflow.module.task.dal.dataobject.TaskListFieldDO;
import com.relayflow.module.task.dal.dataobject.TaskListFieldOptionDO;
import com.relayflow.module.task.dal.dataobject.TaskListItemDO;
import com.relayflow.module.task.dal.mapper.TaskItemFieldValueMapper;
import com.relayflow.module.task.dal.mapper.TaskListFieldMapper;
import com.relayflow.module.task.dal.mapper.TaskListFieldOptionMapper;
import com.relayflow.module.task.dal.mapper.TaskListItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.service.access.TaskAccessService;
import com.relayflow.module.task.service.access.TaskListAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskListFieldServiceImpl implements TaskListFieldService {

    public static final String FIELD_TYPE_SINGLE_SELECT = "SINGLE_SELECT";
    public static final String CUSTOM_PREFIX = "custom:";
    public static final String EMPTY_GROUP_KEY = "__empty__";

    private final TaskListFieldMapper taskListFieldMapper;
    private final TaskListFieldOptionMapper taskListFieldOptionMapper;
    private final TaskItemFieldValueMapper taskItemFieldValueMapper;
    private final TaskListItemMapper taskListItemMapper;
    private final TaskListAccessService taskListAccessService;
    private final TaskAccessService taskAccessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskListFieldListRespVO list(Long listId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        taskListAccessService.requireReadable(listId, userId);

        List<TaskListFieldDO> fields = taskListFieldMapper.selectList(
                Wrappers.<TaskListFieldDO>lambdaQuery()
                        .eq(TaskListFieldDO::getListId, listId)
                        .orderByAsc(TaskListFieldDO::getRank)
                        .orderByAsc(TaskListFieldDO::getId));

        Set<Long> fieldIds = fields.stream().map(TaskListFieldDO::getId).collect(Collectors.toSet());
        Map<Long, List<TaskListFieldOptionDO>> optionsByField = Map.of();
        List<TaskItemFieldValueDO> values = List.of();
        if (!fieldIds.isEmpty()) {
            List<TaskListFieldOptionDO> options = taskListFieldOptionMapper.selectList(
                    Wrappers.<TaskListFieldOptionDO>lambdaQuery()
                            .in(TaskListFieldOptionDO::getFieldId, fieldIds)
                            .orderByAsc(TaskListFieldOptionDO::getRank)
                            .orderByAsc(TaskListFieldOptionDO::getId));
            optionsByField = options.stream().collect(Collectors.groupingBy(TaskListFieldOptionDO::getFieldId));
            values = taskItemFieldValueMapper.selectList(
                    Wrappers.<TaskItemFieldValueDO>lambdaQuery()
                            .in(TaskItemFieldValueDO::getFieldId, fieldIds));
        }

        Map<Long, List<TaskListFieldOptionDO>> finalOptions = optionsByField;
        TaskListFieldListRespVO resp = new TaskListFieldListRespVO();
        resp.setFields(fields.stream().map(f -> toFieldVo(f, finalOptions.getOrDefault(f.getId(), List.of()))).toList());
        resp.setValues(values.stream().map(this::toValueVo).toList());
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskListFieldRespVO create(TaskListFieldCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        taskListAccessService.requireCanMutateTasks(request.getListId(), userId);

        String name = request.getName() == null ? "" : request.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_NAME_EMPTY);
        }
        String fieldType = StringUtils.hasText(request.getFieldType())
                ? request.getFieldType().trim().toUpperCase(Locale.ROOT)
                : FIELD_TYPE_SINGLE_SELECT;
        if (!FIELD_TYPE_SINGLE_SELECT.equals(fieldType)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_FORBIDDEN);
        }

        List<TaskListFieldCreateReqVO.OptionDraft> drafts = request.getOptions() == null
                ? List.of()
                : request.getOptions();
        List<TaskListFieldCreateReqVO.OptionDraft> validDrafts = drafts.stream()
                .filter(d -> d != null && StringUtils.hasText(d.getLabel() == null ? "" : d.getLabel().trim()))
                .toList();
        if (validDrafts.size() < 2) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_OPTIONS_MIN);
        }

        int nextRank = taskListFieldMapper.selectList(
                        Wrappers.<TaskListFieldDO>lambdaQuery()
                                .eq(TaskListFieldDO::getListId, request.getListId()))
                .stream()
                .map(TaskListFieldDO::getRank)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(-1) + 1;

        OffsetDateTime now = OffsetDateTime.now();
        TaskListFieldDO field = new TaskListFieldDO();
        field.setTenantId(tenantId);
        field.setListId(request.getListId());
        field.setName(name);
        field.setFieldKey("pending");
        field.setFieldType(FIELD_TYPE_SINGLE_SELECT);
        field.setRank(nextRank);
        field.setCreator(userId);
        field.setCreateTime(now);
        field.setUpdater(userId);
        field.setUpdateTime(now);
        taskListFieldMapper.insert(field);

        String fieldKey = CUSTOM_PREFIX + field.getId();
        field.setFieldKey(fieldKey);
        taskListFieldMapper.updateById(field);

        List<TaskListFieldOptionDO> options = new ArrayList<>();
        for (int i = 0; i < validDrafts.size(); i++) {
            TaskListFieldCreateReqVO.OptionDraft draft = validDrafts.get(i);
            String label = draft.getLabel().trim();
            String valueKey = StringUtils.hasText(draft.getValueKey())
                    ? draft.getValueKey().trim()
                    : generateValueKey(label, i);
            TaskListFieldOptionDO opt = newOption(tenantId, field.getId(), valueKey, label, i, userId, now);
            taskListFieldOptionMapper.insert(opt);
            options.add(opt);
        }
        return toFieldVo(field, options);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TaskListFieldUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskListFieldDO field = requireField(request.getId());
        taskListAccessService.requireCanMutateTasks(field.getListId(), userId);

        boolean changed = false;
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (!StringUtils.hasText(name)) {
                throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_NAME_EMPTY);
            }
            field.setName(name);
            changed = true;
        }
        if (request.getRank() != null) {
            field.setRank(request.getRank());
            changed = true;
        }
        if (!changed) {
            return;
        }
        field.setUpdater(userId);
        field.setUpdateTime(OffsetDateTime.now());
        taskListFieldMapper.updateById(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskListFieldDO field = requireField(id);
        taskListAccessService.requireCanMutateTasks(field.getListId(), userId);

        List<TaskListFieldOptionDO> options = taskListFieldOptionMapper.selectList(
                Wrappers.<TaskListFieldOptionDO>lambdaQuery()
                        .eq(TaskListFieldOptionDO::getFieldId, id));
        for (TaskListFieldOptionDO opt : options) {
            taskListFieldOptionMapper.deleteById(opt.getId());
        }
        List<TaskItemFieldValueDO> values = taskItemFieldValueMapper.selectList(
                Wrappers.<TaskItemFieldValueDO>lambdaQuery()
                        .eq(TaskItemFieldValueDO::getFieldId, id));
        for (TaskItemFieldValueDO value : values) {
            taskItemFieldValueMapper.deleteById(value.getId());
        }
        taskListFieldMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskListFieldOptionRespVO createOption(TaskListFieldOptionCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        TaskListFieldDO field = requireField(request.getFieldId());
        taskListAccessService.requireCanMutateTasks(field.getListId(), userId);

        String label = request.getLabel() == null ? "" : request.getLabel().trim();
        if (!StringUtils.hasText(label)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_NAME_EMPTY);
        }
        int nextRank = request.getRank() != null
                ? request.getRank()
                : taskListFieldOptionMapper.selectList(
                                Wrappers.<TaskListFieldOptionDO>lambdaQuery()
                                        .eq(TaskListFieldOptionDO::getFieldId, field.getId()))
                        .stream()
                        .map(TaskListFieldOptionDO::getRank)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(-1) + 1;
        String valueKey = StringUtils.hasText(request.getValueKey())
                ? request.getValueKey().trim()
                : generateValueKey(label, nextRank);
        OffsetDateTime now = OffsetDateTime.now();
        TaskListFieldOptionDO opt = newOption(tenantId, field.getId(), valueKey, label, nextRank, userId, now);
        taskListFieldOptionMapper.insert(opt);
        return toOptionVo(opt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOption(TaskListFieldOptionUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskListFieldOptionDO opt = requireOption(request.getId());
        TaskListFieldDO field = requireField(opt.getFieldId());
        taskListAccessService.requireCanMutateTasks(field.getListId(), userId);

        boolean changed = false;
        if (request.getLabel() != null) {
            String label = request.getLabel().trim();
            if (!StringUtils.hasText(label)) {
                throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_NAME_EMPTY);
            }
            opt.setLabel(label);
            changed = true;
        }
        if (request.getRank() != null) {
            opt.setRank(request.getRank());
            changed = true;
        }
        if (request.getColor() != null) {
            opt.setColor(request.getColor().trim());
            changed = true;
        }
        if (!changed) {
            return;
        }
        opt.setUpdater(userId);
        opt.setUpdateTime(OffsetDateTime.now());
        taskListFieldOptionMapper.updateById(opt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOption(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskListFieldOptionDO opt = requireOption(id);
        TaskListFieldDO field = requireField(opt.getFieldId());
        taskListAccessService.requireCanMutateTasks(field.getListId(), userId);

        long remaining = taskListFieldOptionMapper.selectCount(
                Wrappers.<TaskListFieldOptionDO>lambdaQuery()
                        .eq(TaskListFieldOptionDO::getFieldId, field.getId()));
        if (remaining <= 2) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_OPTIONS_MIN);
        }

        List<TaskItemFieldValueDO> values = taskItemFieldValueMapper.selectList(
                Wrappers.<TaskItemFieldValueDO>lambdaQuery()
                        .eq(TaskItemFieldValueDO::getFieldId, field.getId())
                        .eq(TaskItemFieldValueDO::getOptionId, id));
        OffsetDateTime now = OffsetDateTime.now();
        for (TaskItemFieldValueDO value : values) {
            value.setOptionId(null);
            value.setUpdater(userId);
            value.setUpdateTime(now);
            taskItemFieldValueMapper.updateById(value);
        }
        taskListFieldOptionMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void putValue(TaskListFieldValuePutReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        taskListAccessService.requireCanMutateTasks(request.getListId(), userId);
        taskAccessService.requireEditable(request.getItemId(), userId);
        requireMembership(request.getListId(), request.getItemId());

        TaskListFieldDO field = requireField(request.getFieldId());
        if (!Objects.equals(field.getListId(), request.getListId())) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_NOT_FOUND);
        }
        if (request.getOptionId() != null) {
            TaskListFieldOptionDO opt = requireOption(request.getOptionId());
            if (!Objects.equals(opt.getFieldId(), field.getId())) {
                throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_OPTION_NOT_FOUND);
            }
        }
        upsertValue(tenantId, request.getItemId(), field.getId(), request.getOptionId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyGroupMove(Long listId, Long itemId, String fieldKey, String value, Long operatorUserId) {
        if (listId == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_GROUP_MOVE_INVALID);
        }
        Long fieldId = parseCustomFieldId(fieldKey);
        if (fieldId == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_GROUP_MOVE_INVALID);
        }
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        taskListAccessService.requireCanMutateTasks(listId, operatorUserId);
        requireMembership(listId, itemId);

        TaskListFieldDO field = requireField(fieldId);
        if (!Objects.equals(field.getListId(), listId)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_NOT_FOUND);
        }

        Long optionId = null;
        if (!isEmptyGroupValue(value)) {
            String valueKey = value.trim();
            TaskListFieldOptionDO opt = taskListFieldOptionMapper.selectOne(
                    Wrappers.<TaskListFieldOptionDO>lambdaQuery()
                            .eq(TaskListFieldOptionDO::getFieldId, fieldId)
                            .eq(TaskListFieldOptionDO::getValueKey, valueKey)
                            .last("LIMIT 1"));
            if (opt == null) {
                throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_OPTION_NOT_FOUND);
            }
            optionId = opt.getId();
        }
        upsertValue(tenantId, itemId, fieldId, optionId, operatorUserId);
    }

    public static Long parseCustomFieldId(String fieldKey) {
        if (!StringUtils.hasText(fieldKey) || !fieldKey.startsWith(CUSTOM_PREFIX)) {
            return null;
        }
        String raw = fieldKey.substring(CUSTOM_PREFIX.length()).trim();
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean isCustomFieldKey(String fieldKey) {
        return parseCustomFieldId(fieldKey) != null;
    }

    private void upsertValue(Long tenantId, Long itemId, Long fieldId, Long optionId, Long userId) {
        TaskItemFieldValueDO existing = taskItemFieldValueMapper.selectOne(
                Wrappers.<TaskItemFieldValueDO>lambdaQuery()
                        .eq(TaskItemFieldValueDO::getItemId, itemId)
                        .eq(TaskItemFieldValueDO::getFieldId, fieldId)
                        .last("LIMIT 1"));
        OffsetDateTime now = OffsetDateTime.now();
        if (existing == null) {
            TaskItemFieldValueDO row = new TaskItemFieldValueDO();
            row.setTenantId(tenantId);
            row.setItemId(itemId);
            row.setFieldId(fieldId);
            row.setOptionId(optionId);
            row.setCreator(userId);
            row.setCreateTime(now);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            taskItemFieldValueMapper.insert(row);
            return;
        }
        existing.setOptionId(optionId);
        existing.setUpdater(userId);
        existing.setUpdateTime(now);
        taskItemFieldValueMapper.updateById(existing);
    }

    private void requireMembership(Long listId, Long itemId) {
        TaskListItemDO membership = taskListItemMapper.selectOne(
                Wrappers.<TaskListItemDO>lambdaQuery()
                        .eq(TaskListItemDO::getListId, listId)
                        .eq(TaskListItemDO::getTaskId, itemId)
                        .last("LIMIT 1"));
        if (membership == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_FORBIDDEN);
        }
    }

    private TaskListFieldDO requireField(Long id) {
        TaskListFieldDO field = taskListFieldMapper.selectById(id);
        if (field == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_NOT_FOUND);
        }
        return field;
    }

    private TaskListFieldOptionDO requireOption(Long id) {
        TaskListFieldOptionDO opt = taskListFieldOptionMapper.selectById(id);
        if (opt == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FIELD_OPTION_NOT_FOUND);
        }
        return opt;
    }

    private static boolean isEmptyGroupValue(String rawValue) {
        return !StringUtils.hasText(rawValue) || EMPTY_GROUP_KEY.equals(rawValue.trim());
    }

    private static String generateValueKey(String label, int rank) {
        String slug = label.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fff]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(slug)) {
            slug = "opt";
        }
        if (slug.length() > 40) {
            slug = slug.substring(0, 40);
        }
        return slug + "_" + rank;
    }

    private static TaskListFieldOptionDO newOption(
            Long tenantId, Long fieldId, String valueKey, String label, int rank, Long userId, OffsetDateTime now) {
        TaskListFieldOptionDO opt = new TaskListFieldOptionDO();
        opt.setTenantId(tenantId);
        opt.setFieldId(fieldId);
        opt.setValueKey(valueKey);
        opt.setLabel(label);
        opt.setRank(rank);
        opt.setCreator(userId);
        opt.setCreateTime(now);
        opt.setUpdater(userId);
        opt.setUpdateTime(now);
        return opt;
    }

    private TaskListFieldRespVO toFieldVo(TaskListFieldDO field, List<TaskListFieldOptionDO> options) {
        TaskListFieldRespVO vo = new TaskListFieldRespVO();
        vo.setId(field.getId());
        vo.setListId(field.getListId());
        vo.setName(field.getName());
        vo.setFieldKey(field.getFieldKey());
        vo.setFieldType(field.getFieldType());
        vo.setRank(field.getRank());
        vo.setOptions(options.stream()
                .sorted(Comparator.comparing(TaskListFieldOptionDO::getRank, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TaskListFieldOptionDO::getId))
                .map(this::toOptionVo)
                .toList());
        return vo;
    }

    private TaskListFieldOptionRespVO toOptionVo(TaskListFieldOptionDO opt) {
        TaskListFieldOptionRespVO vo = new TaskListFieldOptionRespVO();
        vo.setId(opt.getId());
        vo.setValueKey(opt.getValueKey());
        vo.setLabel(opt.getLabel());
        vo.setRank(opt.getRank());
        vo.setColor(opt.getColor());
        return vo;
    }

    private TaskListFieldValueRespVO toValueVo(TaskItemFieldValueDO row) {
        TaskListFieldValueRespVO vo = new TaskListFieldValueRespVO();
        vo.setItemId(row.getItemId());
        vo.setFieldId(row.getFieldId());
        vo.setOptionId(row.getOptionId());
        return vo;
    }
}
