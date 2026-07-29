package com.relayflow.module.calendar.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarDisplayColorReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarUpdateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalOrgPolicyRespVO;
import com.relayflow.module.calendar.service.calendar.CalCalendarService;
import com.relayflow.module.calendar.service.settings.CalTenantSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/app-api/calendar/calendar")
public class CalCalendarController {

    private final CalCalendarService calCalendarService;
    private final CalTenantSettingsService calTenantSettingsService;

    @GetMapping("/list")
    public CommonResult<List<CalCalendarRespVO>> list() {
        return CommonResult.success(calCalendarService.listVisible());
    }

    /** Org policy visible to members (for UI gates); write path still enforces server-side. */
    @GetMapping("/org-policy")
    public CommonResult<CalOrgPolicyRespVO> orgPolicy() {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        CalOrgPolicyRespVO vo = new CalOrgPolicyRespVO();
        vo.setAllowMemberCreateOwnedCalendar(calTenantSettingsService.allowMemberCreateOwnedCalendar(tenantId));
        return CommonResult.success(vo);
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody CalCalendarCreateReqVO request) {
        return CommonResult.success(calCalendarService.create(request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody CalCalendarUpdateReqVO request) {
        calCalendarService.update(request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        calCalendarService.delete(id);
        return CommonResult.success(true);
    }

    @PutMapping("/display-color")
    public CommonResult<Boolean> updateDisplayColor(@Valid @RequestBody CalCalendarDisplayColorReqVO request) {
        calCalendarService.updateDisplayColor(request.getId(), request.getDisplayColor());
        return CommonResult.success(true);
    }
}
