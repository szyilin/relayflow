package com.relayflow.module.im.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.ConversationItemRespVO;
import com.relayflow.module.im.controller.app.vo.ConversationReadStatusRespVO;
import com.relayflow.module.im.controller.app.vo.MarkConversationReadReqVO;
import com.relayflow.module.im.service.conversation.ImConversationService;
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
@RequestMapping("/app-api/im/conversation")
public class ImConversationController {

    private final ImConversationService conversationService;

    @GetMapping("/list")
    public CommonResult<List<ConversationItemRespVO>> listConversations(
            @RequestParam(value = "keyword", required = false) String keyword) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(
                conversationService.listConversations(loginUser.getTenantId(), loginUser.getUserId(), keyword));
    }

    @PostMapping("/read")
    public CommonResult<Void> markConversationRead(@Valid @RequestBody MarkConversationReadReqVO request) {
        LoginUser loginUser = requireLoginUser();
        conversationService.markConversationRead(
                loginUser.getTenantId(),
                loginUser.getUserId(),
                request.getConversationId(),
                request.getReadSeq());
        return CommonResult.success(null);
    }

    @GetMapping("/read-status")
    public CommonResult<ConversationReadStatusRespVO> getReadStatus(@RequestParam Long conversationId) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(conversationService.getReadStatus(
                loginUser.getTenantId(), loginUser.getUserId(), conversationId));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
