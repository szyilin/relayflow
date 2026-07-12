package com.relayflow.module.im.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersReqVO;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersRespVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupReqVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupRespVO;
import com.relayflow.module.im.controller.app.vo.GroupMemberItemRespVO;
import com.relayflow.module.im.service.group.ImGroupService;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(groupService.createGroup(
                loginUser.getTenantId(), loginUser.getUserId(), request));
    }

    @PostMapping("/members/add")
    public CommonResult<AddGroupMembersRespVO> addMembers(@Valid @RequestBody AddGroupMembersReqVO request) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(groupService.addMembers(
                loginUser.getTenantId(), loginUser.getUserId(), request));
    }

    @GetMapping("/members")
    public CommonResult<List<GroupMemberItemRespVO>> listMembers(
            @RequestParam("conversationId") Long conversationId) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(groupService.listMembers(
                loginUser.getTenantId(), loginUser.getUserId(), conversationId));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
