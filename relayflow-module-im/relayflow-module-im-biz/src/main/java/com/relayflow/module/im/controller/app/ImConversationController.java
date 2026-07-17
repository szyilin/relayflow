package com.relayflow.module.im.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.ConversationItemRespVO;
import com.relayflow.module.im.controller.app.vo.ConversationReadStatusRespVO;
import com.relayflow.module.im.controller.app.vo.MarkConversationReadReqVO;
import com.relayflow.module.im.controller.app.vo.ConversationSearchItemRespVO;
import com.relayflow.module.im.convert.ImConversationConvert;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.conversation.model.ConversationListItem;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
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
        return CommonResult.success(ImConversationConvert.INSTANCE.toRespList(
                conversationService.listMyConversations(keyword)));
    }

    @GetMapping("/search")
    public CommonResult<List<ConversationSearchItemRespVO>> searchConversations(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        String trimmed = requireKeyword(keyword);
        return CommonResult.success(conversationService.listMyConversations(trimmed).stream()
                .limit(clampLimit(limit))
                .map(this::toSearchItem)
                .toList());
    }

    private ConversationSearchItemRespVO toSearchItem(ConversationListItem item) {
        ConversationSearchItemRespVO vo = new ConversationSearchItemRespVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setSubtitle(item.getLastMsgPreview());
        vo.setRoute("/app/messages?conversationId=" + item.getId());
        vo.setEntityType("conversation");
        vo.setEntityId(String.valueOf(item.getId()));
        return vo;
    }

    private String requireKeyword(String keyword) {
        if (!StringUtils.hasText(keyword) || !StringUtils.hasText(keyword.trim())) {
            throw new ServiceException(ErrorCodeConstants.SEARCH_KEYWORD_REQUIRED);
        }
        return keyword.trim();
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, 10);
    }

    @PostMapping("/read")
    public CommonResult<Void> markConversationRead(@Valid @RequestBody MarkConversationReadReqVO request) {
        conversationService.markConversationRead(request.getConversationId(), request.getReadSeq());
        return CommonResult.success(null);
    }

    @GetMapping("/read-status")
    public CommonResult<ConversationReadStatusRespVO> getReadStatus(@RequestParam Long conversationId) {
        return CommonResult.success(conversationService.getReadStatus(conversationId));
    }
}
