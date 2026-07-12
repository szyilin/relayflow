package com.relayflow.module.im.service.group;

import com.relayflow.module.im.controller.app.vo.AddGroupMembersReqVO;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersRespVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupReqVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupRespVO;
import com.relayflow.module.im.controller.app.vo.GroupMemberItemRespVO;

import java.util.List;

public interface ImGroupService {

    CreateGroupRespVO createGroup(Long tenantId, Long userId, CreateGroupReqVO request);

    AddGroupMembersRespVO addMembers(Long tenantId, Long userId, AddGroupMembersReqVO request);

    List<GroupMemberItemRespVO> listMembers(Long tenantId, Long userId, Long conversationId);
}
