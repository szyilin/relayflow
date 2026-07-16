package com.relayflow.module.im.service.group;

import com.relayflow.module.im.controller.app.vo.AddGroupMembersReqVO;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersRespVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupReqVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupRespVO;
import com.relayflow.module.im.controller.app.vo.GroupBotAddRespVO;
import com.relayflow.module.im.controller.app.vo.GroupBotCatalogItemRespVO;
import com.relayflow.module.im.controller.app.vo.GroupBotMembershipReqVO;
import com.relayflow.module.im.controller.app.vo.GroupBotRemoveRespVO;
import com.relayflow.module.im.controller.app.vo.GroupMemberItemRespVO;

import java.util.List;

public interface ImGroupService {

    CreateGroupRespVO createGroup(CreateGroupReqVO request);

    AddGroupMembersRespVO addMembers(AddGroupMembersReqVO request);

    List<GroupMemberItemRespVO> listMembers(Long conversationId);

    List<GroupBotCatalogItemRespVO> listBotCatalog(Long conversationId);

    GroupBotAddRespVO addBot(GroupBotMembershipReqVO request);

    GroupBotRemoveRespVO removeBot(GroupBotMembershipReqVO request);
}
