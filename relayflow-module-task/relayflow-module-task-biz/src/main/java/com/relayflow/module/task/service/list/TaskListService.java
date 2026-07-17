package com.relayflow.module.task.service.list;

import com.relayflow.module.task.controller.app.vo.TaskListCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListIdReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberInviteReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberRemoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberUpdateRoleReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListUpdateReqVO;

import java.util.List;

public interface TaskListService {

    List<TaskListRespVO> listMine();

    TaskListRespVO get(Long id);

    Long create(TaskListCreateReqVO request);

    void update(TaskListUpdateReqVO request);

    void archive(TaskListIdReqVO request);

    List<TaskListMemberRespVO> listMembers(Long listId);

    void inviteMember(TaskListMemberInviteReqVO request);

    void updateMemberRole(TaskListMemberUpdateRoleReqVO request);

    void removeMember(TaskListMemberRemoveReqVO request);
}
