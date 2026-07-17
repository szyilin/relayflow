package com.relayflow.module.calendar.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarShareCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarShareRespVO;
import com.relayflow.module.calendar.service.share.CalCalendarShareService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/calendar/share")
public class CalCalendarShareController {

    private final CalCalendarShareService calCalendarShareService;

    @GetMapping("/list")
    public CommonResult<List<CalCalendarShareRespVO>> list() {
        return CommonResult.success(calCalendarShareService.listMine());
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody CalCalendarShareCreateReqVO request) {
        return CommonResult.success(calCalendarShareService.create(request));
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam @NotNull Long id) {
        calCalendarShareService.delete(id);
        return CommonResult.success(true);
    }
}
