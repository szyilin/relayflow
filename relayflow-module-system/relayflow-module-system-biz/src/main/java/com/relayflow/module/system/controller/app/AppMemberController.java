package com.relayflow.module.system.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.api.user.dto.MemberSearchRespDTO;
import com.relayflow.module.system.controller.app.vo.MemberSearchItemRespVO;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.service.member.AppMemberSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/member")
public class AppMemberController {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;

    private final AppMemberSearchService appMemberSearchService;

    @GetMapping("/search")
    public CommonResult<List<MemberSearchItemRespVO>> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        String trimmed = requireKeyword(keyword);
        List<MemberSearchRespDTO> items = appMemberSearchService.searchMembers(trimmed, clampLimit(limit));
        return CommonResult.success(items.stream().map(this::toResp).toList());
    }

    private MemberSearchItemRespVO toResp(MemberSearchRespDTO item) {
        MemberSearchItemRespVO vo = new MemberSearchItemRespVO();
        vo.setId(item.getUserId());
        vo.setTitle(item.getNickname());
        vo.setSubtitle(item.getDeptName());
        vo.setRoute("/app/contacts?memberId=" + item.getUserId());
        vo.setEntityType("member");
        vo.setEntityId(String.valueOf(item.getUserId()));
        vo.setDeptId(item.getDeptId());
        return vo;
    }

    private String requireKeyword(String keyword) {
        if (!StringUtils.hasText(keyword) || !StringUtils.hasText(keyword.trim())) {
            throw new ServiceException(ErrorCodeConstants.SEARCH_KEYWORD_REQUIRED);
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > 50) {
            trimmed = trimmed.substring(0, 50);
        }
        return trimmed;
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
