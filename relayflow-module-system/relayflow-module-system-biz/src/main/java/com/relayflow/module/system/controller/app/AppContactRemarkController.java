package com.relayflow.module.system.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.app.vo.AppContactRemarkRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactRemarkUpdateReqVO;
import com.relayflow.module.system.service.contactremark.ContactRemarkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/contact-remark")
public class AppContactRemarkController {

    private final ContactRemarkService contactRemarkService;

    @GetMapping("/{targetUserId}")
    public CommonResult<AppContactRemarkRespVO> getMyRemark(
            @PathVariable @NotNull Long targetUserId) {
        return CommonResult.success(contactRemarkService.getMyRemark(targetUserId));
    }

    @PutMapping("/{targetUserId}")
    public CommonResult<AppContactRemarkRespVO> updateMyRemark(
            @PathVariable @NotNull Long targetUserId,
            @Valid @RequestBody AppContactRemarkUpdateReqVO request) {
        return CommonResult.success(contactRemarkService.updateMyRemark(targetUserId, request));
    }
}
