package com.relayflow.module.calendar.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarUpdateReqVO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.enums.CalendarType;
import com.relayflow.module.calendar.service.calendar.CalCalendarService;
import com.relayflow.module.calendar.service.share.CalCalendarShareService;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/calendar/calendar")
public class CalCalendarController {

    private final CalCalendarService calCalendarService;
    private final CalCalendarShareService calCalendarShareService;

    @GetMapping("/list")
    public CommonResult<List<CalCalendarRespVO>> list() {
        List<CalCalendarRespVO> owned = calCalendarService.listMine();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Map<Long, String> permissions = calCalendarShareService.sharedCalendarPermissions(userId);
        List<CalCalendarDO> sharedCalendars = calCalendarShareService.loadSharedCalendars(userId);

        Set<Long> ownedIds = owned.stream()
                .map(CalCalendarRespVO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CalCalendarRespVO> result = new ArrayList<>(owned);
        for (CalCalendarDO calendar : sharedCalendars) {
            if (!ownedIds.contains(calendar.getId())) {
                result.add(toSharedResp(calendar, permissions.get(calendar.getId())));
            }
        }
        return CommonResult.success(result);
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

    private CalCalendarRespVO toSharedResp(CalCalendarDO calendar, String permission) {
        CalCalendarRespVO vo = new CalCalendarRespVO();
        vo.setId(calendar.getId());
        vo.setName(calendar.getName());
        vo.setColor(calendar.getColor());
        vo.setDescription(calendar.getDescription());
        vo.setType(CalendarType.SHARED.name());
        vo.setOwnerUserId(calendar.getOwnerUserId());
        vo.setPermission(permission != null ? permission : "READ");
        return vo;
    }
}
