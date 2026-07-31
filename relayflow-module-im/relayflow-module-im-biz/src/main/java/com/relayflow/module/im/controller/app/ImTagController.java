package com.relayflow.module.im.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.ImUserTagAssignReqVO;
import com.relayflow.module.im.controller.app.vo.ImUserTagCreateAndAssignReqVO;
import com.relayflow.module.im.controller.app.vo.ImUserTagCreateReqVO;
import com.relayflow.module.im.controller.app.vo.ImUserTagItemRespVO;
import com.relayflow.module.im.service.tag.ImUserTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/im/tag")
public class ImTagController {

    private final ImUserTagService userTagService;

    @GetMapping("/list")
    public CommonResult<List<ImUserTagItemRespVO>> listTags() {
        return CommonResult.success(userTagService.listMyTags());
    }

    @PostMapping("/create")
    public CommonResult<ImUserTagItemRespVO> createTag(@Valid @RequestBody ImUserTagCreateReqVO request) {
        return CommonResult.success(userTagService.createTag(request.getName()));
    }

    @PostMapping("/assign")
    public CommonResult<Void> assignTag(@Valid @RequestBody ImUserTagAssignReqVO request) {
        userTagService.assignTag(request.getConversationId(), request.getTagId());
        return CommonResult.success(null);
    }

    @PostMapping("/create-and-assign")
    public CommonResult<ImUserTagItemRespVO> createAndAssign(
            @Valid @RequestBody ImUserTagCreateAndAssignReqVO request) {
        return CommonResult.success(userTagService.createAndAssign(request.getConversationId(), request.getName()));
    }
}
