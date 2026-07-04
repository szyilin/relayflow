package com.relayflow.module.system.service.user;

import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;

public interface UserService {

    Long createUser(UserCreateReqDTO request);
}
