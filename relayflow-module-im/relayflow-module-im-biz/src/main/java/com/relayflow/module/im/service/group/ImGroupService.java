package com.relayflow.module.im.service.group;

import com.relayflow.module.im.controller.app.vo.AddGroupMembersReqVO;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersRespVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupReqVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupRespVO;
import com.relayflow.module.im.controller.app.vo.GroupMemberItemRespVO;

import java.util.List;

public interface ImGroupService {

    CreateGroupRespVO createGroup(CreateGroupReqVO request);

    AddGroupMembersRespVO addMembers(AddGroupMembersReqVO request);

    List<GroupMemberItemRespVO> listMembers(Long conversationId);
}
