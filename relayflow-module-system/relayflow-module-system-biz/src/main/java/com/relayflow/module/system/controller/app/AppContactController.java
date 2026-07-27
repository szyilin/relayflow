package com.relayflow.module.system.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.app.vo.AppContactAccountSearchHitRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactExternalItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactRequestCreateReqVO;
import com.relayflow.module.system.controller.app.vo.AppContactRequestItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactStarredItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactStarredUpdateReqVO;
import com.relayflow.module.system.service.contact.ContactEnrichService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/contact")
public class AppContactController {

    private final ContactEnrichService contactEnrichService;

    @GetMapping("/external/list")
    public CommonResult<List<AppContactExternalItemRespVO>> listExternal() {
        return CommonResult.success(contactEnrichService.listExternal());
    }

    @GetMapping("/request/list")
    public CommonResult<List<AppContactRequestItemRespVO>> listRequests() {
        return CommonResult.success(contactEnrichService.listRequests());
    }

    @PostMapping("/request/{id}/accept")
    public CommonResult<AppContactRequestItemRespVO> acceptRequest(@PathVariable("id") @NotNull Long id) {
        return CommonResult.success(contactEnrichService.acceptRequest(id));
    }

    @PostMapping("/request/{id}/reject")
    public CommonResult<AppContactRequestItemRespVO> rejectRequest(@PathVariable("id") @NotNull Long id) {
        return CommonResult.success(contactEnrichService.rejectRequest(id));
    }

    @PostMapping("/request")
    public CommonResult<AppContactRequestItemRespVO> createRequest(
            @Valid @RequestBody AppContactRequestCreateReqVO request) {
        return CommonResult.success(contactEnrichService.createRequest(request));
    }

    @GetMapping("/search-account")
    public CommonResult<List<AppContactAccountSearchHitRespVO>> searchAccount(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String email) {
        return CommonResult.success(contactEnrichService.searchAccount(mobile, email));
    }

    @GetMapping("/starred/list")
    public CommonResult<List<AppContactStarredItemRespVO>> listStarred() {
        return CommonResult.success(contactEnrichService.listStarred());
    }

    @PutMapping("/starred/{userId}")
    public CommonResult<Boolean> updateStarred(
            @PathVariable @NotNull Long userId,
            @Valid @RequestBody AppContactStarredUpdateReqVO request) {
        contactEnrichService.updateStarred(userId, request);
        return CommonResult.success(true);
    }
}
