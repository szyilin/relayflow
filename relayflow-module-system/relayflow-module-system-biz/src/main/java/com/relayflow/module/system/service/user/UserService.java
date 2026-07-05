package com.relayflow.module.system.service.user;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.controller.admin.user.vo.UserPageReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserRespVO;

public interface UserService {

    Long createUser(UserCreateReqDTO request);

    PageResult<UserRespVO> getUserPage(UserPageReqVO request);
}
