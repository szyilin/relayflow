package com.relayflow.module.system.service.auth;

import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;

public interface AuthService {

    AuthLoginRespVO login(AuthLoginReqVO request);
}
