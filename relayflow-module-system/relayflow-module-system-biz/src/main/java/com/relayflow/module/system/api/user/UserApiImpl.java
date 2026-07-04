package com.relayflow.module.system.api.user;

import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.service.user.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserApiImpl implements UserApi {

    private final UserService userService;

    public UserApiImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Long createUser(UserCreateReqDTO request) {
        return userService.createUser(request);
    }
}
