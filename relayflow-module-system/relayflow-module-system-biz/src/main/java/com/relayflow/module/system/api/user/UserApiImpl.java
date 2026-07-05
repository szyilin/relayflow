package com.relayflow.module.system.api.user;

import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserApiImpl implements UserApi {

    private final UserService userService;

    @Override
    public Long createUser(UserCreateReqDTO request) {
        return userService.createUser(request);
    }
}
