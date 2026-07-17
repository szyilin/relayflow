package com.relayflow.module.task.service.collab;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.task.controller.app.vo.TaskActivityRespVO;
import com.relayflow.module.task.controller.app.vo.TaskAssignReqVO;
import com.relayflow.module.task.controller.app.vo.TaskCommentCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskCommentRespVO;
import com.relayflow.module.task.controller.app.vo.TaskFollowerRespVO;
import com.relayflow.module.task.controller.app.vo.TaskItemPageReqVO;
import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;

import java.util.List;

public interface TaskCollabService {

    void follow(Long taskId);

    void unfollow(Long taskId);

    List<TaskFollowerRespVO> listFollowers(Long taskId);

    PageResult<TaskItemRespVO> pageFollowing(TaskItemPageReqVO request);

    void assign(TaskAssignReqVO request);

    List<TaskCommentRespVO> listComments(Long taskId);

    Long createComment(TaskCommentCreateReqVO request);

    List<TaskActivityRespVO> listActivities(Long taskId);

    List<TaskActivityRespVO> listActivityFeed(int limit);
}
