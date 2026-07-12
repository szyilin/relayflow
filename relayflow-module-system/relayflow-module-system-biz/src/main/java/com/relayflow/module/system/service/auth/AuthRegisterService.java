package com.relayflow.module.system.service.auth;

import com.relayflow.module.system.controller.app.vo.AuthRegisterReqVO;
import com.relayflow.module.system.controller.app.vo.AuthRegisterRespVO;

public interface AuthRegisterService {

    AuthRegisterRespVO register(AuthRegisterReqVO request);
}
