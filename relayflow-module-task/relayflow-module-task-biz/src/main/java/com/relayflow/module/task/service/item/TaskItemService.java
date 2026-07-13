package com.relayflow.module.task.service.item;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;

import java.util.List;

public interface TaskItemService {

    PageResult<TaskItemRespVO> pageMyTasks(Long userId, TaskItemPageReqVO request);

    List<TaskItemRespVO> searchMyTasks(Long userId, String keyword, int limit);

    Long createTask(Long userId, Long tenantId, TaskItemCreateReqVO request);

    void updateTask(Long userId, TaskItemUpdateReqVO request);

    void toggleDone(Long userId, TaskItemToggleDoneReqVO request);

    void deleteTask(Long userId, Long id);
}
