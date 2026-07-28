package com.relayflow.module.im.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.EditMessageReqVO;
import com.relayflow.module.im.controller.app.vo.MessageItemRespVO;
import com.relayflow.module.im.controller.app.vo.RecallMessageReqVO;
import com.relayflow.module.im.controller.app.vo.SendMessageReqVO;
import com.relayflow.module.im.controller.app.vo.SendMessageRespVO;
import com.relayflow.module.im.service.message.ImMessageService;
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
        return CommonResult.success(messageService.listMessages(conversationId, afterSeq));
    }

    @PostMapping("/send")
    public CommonResult<SendMessageRespVO> sendMessage(@Valid @RequestBody SendMessageReqVO request) {
        return CommonResult.success(messageService.sendMyMessage(request));
    }

    @PostMapping("/recall")
    public CommonResult<Boolean> recallMessage(@Valid @RequestBody RecallMessageReqVO request) {
        messageService.recallMessage(request.getMessageId());
        return CommonResult.success(true);
    }

    @PostMapping("/edit")
    public CommonResult<Boolean> editMessage(@Valid @RequestBody EditMessageReqVO request) {
        messageService.editMessage(request.getMessageId(), request.getContent());
        return CommonResult.success(true);
    }
}
