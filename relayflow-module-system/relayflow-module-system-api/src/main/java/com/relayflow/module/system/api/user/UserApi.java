package com.relayflow.module.system.api.user;

import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;

public interface UserApi {

    Long createUser(UserCreateReqDTO request);

    UserBasicDTO getUserBasic(Long userId);
}
