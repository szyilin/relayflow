package com.relayflow.module.im.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.EditMessageReqVO;
import com.relayflow.module.im.controller.app.vo.ForwardMessageReqVO;
import com.relayflow.module.im.controller.app.vo.MessageItemRespVO;
import com.relayflow.module.im.controller.app.vo.RecallMessageReqVO;
import com.relayflow.module.im.controller.app.vo.SendMessageReqVO;
import com.relayflow.module.im.controller.app.vo.SendMessageRespVO;
import com.relayflow.module.im.controller.app.vo.ToggleReactionReqVO;
import com.relayflow.module.im.controller.app.vo.ToggleReactionRespVO;
import com.relayflow.module.im.service.message.ImMessageReactionService;
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
    private final ImMessageReactionService reactionService;

    @GetMapping("/list")
    public CommonResult<List<MessageItemRespVO>> listMessages(
            @RequestParam("conversationId") Long conversationId,
            @RequestParam(value = "afterSeq", required = false) Long afterSeq,
            @RequestParam(value = "beforeSeq", required = false) Long beforeSeq,
            @RequestParam(value = "aroundSeq", required = false) Long aroundSeq,
            @RequestParam(value = "before", required = false) Integer before,
            @RequestParam(value = "after", required = false) Integer after,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return CommonResult.success(messageService.listMessages(
                conversationId, afterSeq, beforeSeq, aroundSeq, before, after, limit));
    }

    @PostMapping("/send")
    public CommonResult<SendMessageRespVO> sendMessage(@Valid @RequestBody SendMessageReqVO request) {
        return CommonResult.success(messageService.sendMyMessage(request));
    }

    @PostMapping("/forward")
    public CommonResult<MessageItemRespVO> forwardMessage(@Valid @RequestBody ForwardMessageReqVO request) {
        return CommonResult.success(messageService.forwardMessage(request));
    }

    @PostMapping("/reaction/toggle")
    public CommonResult<ToggleReactionRespVO> toggleReaction(@Valid @RequestBody ToggleReactionReqVO request) {
        return CommonResult.success(reactionService.toggle(request.getMessageId(), request.getEmoji()));
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
