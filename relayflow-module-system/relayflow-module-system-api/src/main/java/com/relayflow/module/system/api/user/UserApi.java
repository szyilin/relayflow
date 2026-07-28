package com.relayflow.module.system.api.user;

import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;

import java.util.List;

public interface UserApi {

    Long createUser(UserCreateReqDTO request);

    UserBasicDTO getUserBasic(Long userId);

    List<Long> listUserDeptIds(Long userId);
}
