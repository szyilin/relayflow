package com.relayflow.module.system.service.user;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.api.user.dto.UserInviteReqDTO;
import com.relayflow.module.system.controller.admin.user.vo.UserGetRespVO;
import com.relayflow.module.system.controller.admin.user.vo.UserPageReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserRespVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateDeptReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateRoleReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateStatusReqVO;
import com.relayflow.module.system.controller.app.vo.AppContactItemRespVO;

import java.util.List;

public interface UserService {

    Long createUser(UserCreateReqDTO request);

    Long inviteMember(UserInviteReqDTO request);

    UserBasicDTO getUserBasic(Long userId);

    UserGetRespVO getUser(Long id);

    void updateUser(UserUpdateReqVO request);

    void updateUserStatus(UserUpdateStatusReqVO request);

    void updateUserDept(UserUpdateDeptReqVO request);

    void updateUserRole(UserUpdateRoleReqVO request);

    PageResult<UserRespVO> getUserPage(UserPageReqVO request);

    List<AppContactItemRespVO> listContactsByDept(Long deptId, String keyword);
}
