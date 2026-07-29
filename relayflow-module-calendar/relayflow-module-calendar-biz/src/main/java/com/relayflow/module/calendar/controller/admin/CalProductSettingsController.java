package com.relayflow.module.calendar.controller.admin;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.calendar.controller.admin.vo.CalAllStaffSaveReqVO;
import com.relayflow.module.calendar.controller.admin.vo.CalProductSettingsRespVO;
import com.relayflow.module.calendar.controller.admin.vo.CalProductSettingsUpdateReqVO;
import com.relayflow.module.calendar.service.allstaff.CalAllStaffService;
import com.relayflow.module.calendar.service.settings.CalTenantSettingsService;
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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/calendar/product-settings")
public class CalProductSettingsController {

    private final CalTenantSettingsService calTenantSettingsService;
    private final CalAllStaffService calAllStaffService;

    @PreAuthorize("hasAuthority('calendar:product-settings:query')")
    @GetMapping
    public CommonResult<CalProductSettingsRespVO> getSettings() {
        return CommonResult.success(calTenantSettingsService.getSettings());
    }

    @PreAuthorize("hasAuthority('calendar:product-settings:update')")
    @PutMapping
    public CommonResult<CalProductSettingsRespVO> updateSettings(
            @Valid @RequestBody CalProductSettingsUpdateReqVO request) {
        return CommonResult.success(calTenantSettingsService.updateSettings(request));
    }

    @PreAuthorize("hasAuthority('calendar:product-settings:update')")
    @PostMapping("/all-staff")
    public CommonResult<Long> createAllStaff(@Valid @RequestBody CalAllStaffSaveReqVO request) {
        return CommonResult.success(calAllStaffService.create(request));
    }

    @PreAuthorize("hasAuthority('calendar:product-settings:update')")
    @PutMapping("/all-staff")
    public CommonResult<Boolean> updateAllStaff(@Valid @RequestBody CalAllStaffSaveReqVO request) {
        calAllStaffService.update(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('calendar:product-settings:update')")
    @DeleteMapping("/all-staff")
    public CommonResult<Boolean> deleteAllStaff(@RequestParam @NotNull Long id) {
        calAllStaffService.delete(id);
        return CommonResult.success(true);
    }
}
