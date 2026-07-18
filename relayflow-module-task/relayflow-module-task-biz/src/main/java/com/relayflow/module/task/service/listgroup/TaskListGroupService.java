package com.relayflow.module.task.service.listgroup;

import com.relayflow.module.task.controller.app.vo.TaskListGroupCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupMoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupUpdateReqVO;

public interface TaskListGroupService {

    TaskListGroupListRespVO list(Long listId);

    TaskListGroupRespVO create(TaskListGroupCreateReqVO request);

    void update(TaskListGroupUpdateReqVO request);

    void delete(Long id);

    void move(TaskListGroupMoveReqVO request);

    /** Ensure default group exists; return its id. */
    Long ensureDefaultGroupId(Long listId, Long tenantId, Long operatorUserId);
}
