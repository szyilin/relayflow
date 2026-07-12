package com.relayflow.module.im.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.MessageItemRespVO;
import com.relayflow.module.im.controller.app.vo.SendMessageReqVO;
import com.relayflow.module.im.controller.app.vo.SendMessageRespVO;
import com.relayflow.module.im.service.message.ImMessageService;
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
@RequestMapping("/app-api/im/message")
public class ImMessageController {

    private final ImMessageService messageService;

    @GetMapping("/list")
    public CommonResult<List<MessageItemRespVO>> listMessages(
            @RequestParam("conversationId") Long conversationId,
            @RequestParam(value = "afterSeq", required = false, defaultValue = "0") Long afterSeq) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(messageService.listMessages(
                loginUser.getTenantId(), loginUser.getUserId(), conversationId, afterSeq));
    }

    @PostMapping("/send")
    public CommonResult<SendMessageRespVO> sendMessage(@Valid @RequestBody SendMessageReqVO request) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(messageService.sendMessage(
                loginUser.getTenantId(), loginUser.getUserId(), request, null));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
