package com.relayflow.module.task.service.item;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskSubtaskCreateReqVO;

import java.time.OffsetDateTime;
import java.util.List;

public interface TaskItemService {

    PageResult<TaskItemRespVO> pageMyTasks(TaskItemPageReqVO request);

    List<TaskItemRespVO> searchMyTasks(String keyword, int limit);

    List<TaskItemRespVO> searchMyTasks(Long userId, String keyword, int limit);

    /**
     * TODO tasks assigned to the user with dueTime in {@code [from, to)}.
     */
    List<TaskItemRespVO> listDueRange(OffsetDateTime from, OffsetDateTime to, int limit);

    List<TaskItemRespVO> listDueRange(Long userId, OffsetDateTime from, OffsetDateTime to, int limit);

    TaskItemRespVO getTask(Long id);

    Long createTask(TaskItemCreateReqVO request);

    void updateTask(TaskItemUpdateReqVO request);

    void toggleDone(TaskItemToggleDoneReqVO request);

    void deleteTask(Long id);

    List<TaskItemRespVO> listSubtasks(Long parentId);

    Long createSubtask(TaskSubtaskCreateReqVO request);
}
