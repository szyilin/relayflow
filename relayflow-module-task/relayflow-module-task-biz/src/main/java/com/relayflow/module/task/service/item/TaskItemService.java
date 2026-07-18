package com.relayflow.module.task.service.item;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.task.controller.app.vo.TaskItemBoardMoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemToggleDoneReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemUpdateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskSubtaskCreateReqVO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;

import java.time.OffsetDateTime;
import java.util.List;

public interface TaskItemService {

    PageResult<TaskItemRespVO> pageMyTasks(TaskItemPageReqVO request);

    List<TaskItemRespVO> searchMyTasks(String keyword, int limit);

    List<TaskItemDO> searchMyTasks(Long userId, String keyword, int limit);

    /**
     * Open (TODO / IN_PROGRESS) tasks assigned to the user with dueTime in {@code [from, to)}.
     */
    List<TaskItemRespVO> listDueRange(OffsetDateTime from, OffsetDateTime to, int limit);

    List<TaskItemDO> listDueRange(Long userId, OffsetDateTime from, OffsetDateTime to, int limit);

    TaskItemRespVO getTask(Long id);

    Long createTask(TaskItemCreateReqVO request);

    void updateTask(TaskItemUpdateReqVO request);

    void toggleDone(TaskItemToggleDoneReqVO request);

    void deleteTask(Long id);

    List<TaskItemRespVO> listSubtasks(Long parentId);

    Long createSubtask(TaskSubtaskCreateReqVO request);

    void boardMove(TaskItemBoardMoveReqVO request);
}
