package com.relayflow.module.system.controller.admin.enterprise;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseAddressRespVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseAddressSaveReqVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseAddressStatusReqVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseProfileRespVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseProfileUpdateReqVO;
import com.relayflow.module.system.service.enterprise.EnterpriseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/system/enterprise")
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @PreAuthorize("hasAuthority('system:enterprise:query')")
    @GetMapping("/profile")
    public CommonResult<EnterpriseProfileRespVO> getProfile() {
        return CommonResult.success(enterpriseService.getProfile());
    }

    @PreAuthorize("hasAuthority('system:enterprise:update')")
    @PutMapping("/profile")
    public CommonResult<EnterpriseProfileRespVO> updateProfile(
            @Valid @RequestBody EnterpriseProfileUpdateReqVO request) {
        return CommonResult.success(enterpriseService.updateProfile(request));
    }

    @PreAuthorize("hasAuthority('system:enterprise:query')")
    @GetMapping("/address/list")
    public CommonResult<List<EnterpriseAddressRespVO>> listAddresses(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "status", required = false) String status) {
        return CommonResult.success(enterpriseService.listAddresses(keyword, expandTypes(types), status));
    }

    @PreAuthorize("hasAuthority('system:enterprise:update')")
    @PostMapping("/address/create")
    public CommonResult<EnterpriseAddressRespVO> createAddress(
            @Valid @RequestBody EnterpriseAddressSaveReqVO request) {
        return CommonResult.success(enterpriseService.createAddress(request));
    }

    @PreAuthorize("hasAuthority('system:enterprise:update')")
    @PutMapping("/address/update")
    public CommonResult<EnterpriseAddressRespVO> updateAddress(
            @RequestParam("id") @NotNull Long id,
            @Valid @RequestBody EnterpriseAddressSaveReqVO request) {
        return CommonResult.success(enterpriseService.updateAddress(id, request));
    }

    @PreAuthorize("hasAuthority('system:enterprise:update')")
    @PutMapping("/address/status")
    public CommonResult<EnterpriseAddressRespVO> updateAddressStatus(
            @RequestParam("id") @NotNull Long id,
            @Valid @RequestBody EnterpriseAddressStatusReqVO request) {
        return CommonResult.success(enterpriseService.updateAddressStatus(id, request));
    }

    @PreAuthorize("hasAuthority('system:enterprise:update')")
    @DeleteMapping("/address/delete")
    public CommonResult<Boolean> deleteAddress(@RequestParam("id") @NotNull Long id) {
        enterpriseService.deleteAddress(id);
        return CommonResult.success(true);
    }

    private static List<String> expandTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return Collections.emptyList();
        }
        // Support both repeated params and comma-separated values
        return types.stream()
                .flatMap(item -> Arrays.stream(item.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
