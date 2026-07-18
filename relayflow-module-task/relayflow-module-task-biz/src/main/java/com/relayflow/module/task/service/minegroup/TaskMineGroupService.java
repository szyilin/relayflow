package com.relayflow.module.task.service.minegroup;

import com.relayflow.module.task.controller.app.vo.TaskMineGroupCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupMoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupRespVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupUpdateReqVO;

public interface TaskMineGroupService {

    TaskMineGroupListRespVO list();

    TaskMineGroupRespVO create(TaskMineGroupCreateReqVO request);

    void update(TaskMineGroupUpdateReqVO request);

    void delete(Long id);

    void move(TaskMineGroupMoveReqVO request);

    /**
     * Ensure the task is in the user's default personal group (idempotent).
     */
    void ensureMembershipInDefault(Long taskId, Long userId, Long tenantId);
}
