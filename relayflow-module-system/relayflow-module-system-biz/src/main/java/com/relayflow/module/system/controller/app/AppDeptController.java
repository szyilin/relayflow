package com.relayflow.module.system.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.admin.dept.vo.DeptRespVO;
import com.relayflow.module.system.service.dept.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/dept")
public class AppDeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public CommonResult<List<DeptRespVO>> getDeptTree() {
        return CommonResult.success(deptService.getEnabledDeptList());
    }
}
