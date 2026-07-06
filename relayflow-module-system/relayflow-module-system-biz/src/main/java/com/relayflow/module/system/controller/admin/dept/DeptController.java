package com.relayflow.module.system.controller.admin.dept;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.admin.dept.vo.DeptCreateReqVO;
import com.relayflow.module.system.controller.admin.dept.vo.DeptRespVO;
import com.relayflow.module.system.controller.admin.dept.vo.DeptUpdateReqVO;
import com.relayflow.module.system.service.dept.DeptService;
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

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/system/dept")
public class DeptController {

    private final DeptService deptService;

    @PreAuthorize("hasAuthority('system:dept:list')")
    @GetMapping("/list")
    public CommonResult<List<DeptRespVO>> getDeptList() {
        return CommonResult.success(deptService.getDeptList());
    }

    @PreAuthorize("hasAuthority('system:dept:query')")
    @GetMapping("/get")
    public CommonResult<DeptRespVO> getDept(@RequestParam @NotNull Long id) {
        return CommonResult.success(deptService.getDept(id));
    }

    @PreAuthorize("hasAuthority('system:dept:create')")
    @PostMapping("/create")
    public CommonResult<Long> createDept(@Valid @RequestBody DeptCreateReqVO request) {
        return CommonResult.success(deptService.createDept(request));
    }

    @PreAuthorize("hasAuthority('system:dept:update')")
    @PutMapping("/update")
    public CommonResult<Boolean> updateDept(@Valid @RequestBody DeptUpdateReqVO request) {
        deptService.updateDept(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('system:dept:delete')")
    @DeleteMapping("/delete")
    public CommonResult<Boolean> deleteDept(@RequestParam @NotNull Long id) {
        deptService.deleteDept(id);
        return CommonResult.success(true);
    }
}
