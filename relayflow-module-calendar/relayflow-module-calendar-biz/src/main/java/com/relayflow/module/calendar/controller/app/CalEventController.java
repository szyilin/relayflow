package com.relayflow.module.calendar.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.calendar.controller.app.vo.CalEventCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRescheduleReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRespondReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalEventUpdateReqVO;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import com.relayflow.module.calendar.service.event.CalEventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/calendar/event")
public class CalEventController {

    private final CalEventService calEventService;

    @GetMapping("/list")
    public CommonResult<List<CalEventRespVO>> list(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "calendarIds", required = false) String calendarIds) {
        return CommonResult.success(calEventService.list(from, to, parseCalendarIds(calendarIds)));
    }

    @GetMapping("/get")
    public CommonResult<CalEventRespVO> get(@RequestParam @NotNull Long id) {
        return CommonResult.success(calEventService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody CalEventCreateReqVO request) {
        return CommonResult.success(calEventService.create(request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody CalEventUpdateReqVO request) {
        calEventService.update(request);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(
            @RequestParam @NotNull Long id,
            @RequestParam(value = "editScope", required = false) String editScope,
            @RequestParam(value = "instanceStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime instanceStart) {
        calEventService.delete(id, editScope, instanceStart);
        return CommonResult.success(true);
    }

    @PutMapping("/reschedule")
    public CommonResult<Boolean> reschedule(@Valid @RequestBody CalEventRescheduleReqVO request) {
        calEventService.reschedule(request);
        return CommonResult.success(true);
    }

    @PutMapping("/respond")
    public CommonResult<Boolean> respond(@Valid @RequestBody CalEventRespondReqVO request) {
        calEventService.respond(request);
        return CommonResult.success(true);
    }

    private Set<Long> parseCalendarIds(String calendarIds) {
        if (!StringUtils.hasText(calendarIds)) {
            return Set.of();
        }
        try {
            return Arrays.stream(calendarIds.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::valueOf)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (NumberFormatException ex) {
            throw new ServiceException(ErrorCodeConstants.EVENT_TIME_INVALID);
        }
    }
}
