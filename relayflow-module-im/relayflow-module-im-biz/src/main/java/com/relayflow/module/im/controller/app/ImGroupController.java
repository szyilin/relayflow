package com.relayflow.module.im.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersReqVO;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersRespVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupReqVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupRespVO;
import com.relayflow.module.im.controller.app.vo.GroupBotAddRespVO;
import com.relayflow.module.im.controller.app.vo.GroupBotCatalogItemRespVO;
import com.relayflow.module.im.controller.app.vo.GroupBotMembershipReqVO;
import com.relayflow.module.im.controller.app.vo.GroupBotRemoveRespVO;
import com.relayflow.module.im.controller.app.vo.GroupInfoRespVO;
import com.relayflow.module.im.controller.app.vo.GroupMemberExportItemVO;
import com.relayflow.module.im.controller.app.vo.GroupMemberItemRespVO;
import com.relayflow.module.im.controller.app.vo.JoinGroupReqVO;
import com.relayflow.module.im.controller.app.vo.LeaveGroupReqVO;
import com.relayflow.module.im.controller.app.vo.PublicGroupSearchItemRespVO;
import com.relayflow.module.im.controller.app.vo.UpdateGroupPublicReqVO;
import com.relayflow.module.im.service.group.ImGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/im/group")
public class ImGroupController {

    private final ImGroupService groupService;

    @PostMapping("/create")
    public CommonResult<CreateGroupRespVO> createGroup(@Valid @RequestBody CreateGroupReqVO request) {
        return CommonResult.success(groupService.createGroup(request));
    }

    @PostMapping("/members/add")
    public CommonResult<AddGroupMembersRespVO> addMembers(@Valid @RequestBody AddGroupMembersReqVO request) {
        return CommonResult.success(groupService.addMembers(request));
    }

    @GetMapping("/members")
    public CommonResult<List<GroupMemberItemRespVO>> listMembers(
            @RequestParam("conversationId") Long conversationId) {
        return CommonResult.success(groupService.listMembers(conversationId));
    }

    @GetMapping("/members/export")
    public CommonResult<List<GroupMemberExportItemVO>> exportMembers(
            @RequestParam("conversationId") Long conversationId) {
        return CommonResult.success(groupService.exportMembers(conversationId));
    }

    @GetMapping("/info")
    public CommonResult<GroupInfoRespVO> getGroupInfo(@RequestParam("conversationId") Long conversationId) {
        return CommonResult.success(groupService.getGroupInfo(conversationId));
    }

    @PutMapping("/public")
    public CommonResult<Boolean> updatePublic(@Valid @RequestBody UpdateGroupPublicReqVO request) {
        groupService.updatePublic(request);
        return CommonResult.success(true);
    }

    @GetMapping("/search-public")
    public CommonResult<List<PublicGroupSearchItemRespVO>> searchPublicGroups(
            @RequestParam("keyword") String keyword) {
        return CommonResult.success(groupService.searchPublicGroups(keyword));
    }

    @PostMapping("/join")
    public CommonResult<Boolean> joinGroup(@Valid @RequestBody JoinGroupReqVO request) {
        groupService.joinGroup(request);
        return CommonResult.success(true);
    }

    @PostMapping("/leave")
    public CommonResult<Boolean> leaveGroup(@Valid @RequestBody LeaveGroupReqVO request) {
        groupService.leaveGroup(request);
        return CommonResult.success(true);
    }

    @GetMapping("/bots/catalog")
    public CommonResult<List<GroupBotCatalogItemRespVO>> listBotCatalog(
            @RequestParam("conversationId") Long conversationId) {
        return CommonResult.success(groupService.listBotCatalog(conversationId));
    }

    @PostMapping("/bots/add")
    public CommonResult<GroupBotAddRespVO> addBot(@Valid @RequestBody GroupBotMembershipReqVO request) {
        return CommonResult.success(groupService.addBot(request));
    }

    @PostMapping("/bots/remove")
    public CommonResult<GroupBotRemoveRespVO> removeBot(@Valid @RequestBody GroupBotMembershipReqVO request) {
        return CommonResult.success(groupService.removeBot(request));
    }
}
