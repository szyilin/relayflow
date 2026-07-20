package com.relayflow.module.task.service.listfield;

import com.relayflow.module.task.controller.app.vo.TaskListFieldCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldOptionCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldOptionRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldOptionUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldValuePutReqVO;

public interface TaskListFieldService {

    TaskListFieldListRespVO list(Long listId);

    TaskListFieldRespVO create(TaskListFieldCreateReqVO request);

    void update(TaskListFieldUpdateReqVO request);

    void delete(Long id);

    TaskListFieldOptionRespVO createOption(TaskListFieldOptionCreateReqVO request);

    void updateOption(TaskListFieldOptionUpdateReqVO request);

    void deleteOption(Long id);

    void putValue(TaskListFieldValuePutReqVO request);

    /**
     * Persist custom field bucket for group-move.
     *
     * @param fieldKey {@code custom:{fieldId}}
     * @param value    option value_key, or null / {@code __empty__} to clear
     */
    void applyGroupMove(Long listId, Long itemId, String fieldKey, String value, Long operatorUserId);
}
