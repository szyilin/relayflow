package com.relayflow.module.calendar.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarUpdateReqVO;
import com.relayflow.module.calendar.service.calendar.CalCalendarService;
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

    @GetMapping("/list")
    public CommonResult<List<CalCalendarRespVO>> list() {
        return CommonResult.success(calCalendarService.listVisible());
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
}
