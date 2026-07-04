package com.relayflow.module.system.controller.admin.tenant;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.convert.TenantConvert;
import com.relayflow.module.system.controller.admin.tenant.vo.TenantRespVO;
import com.relayflow.module.system.service.tenant.TenantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/system/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/default")
    public CommonResult<TenantRespVO> getDefaultTenant() {
        return CommonResult.success(TenantConvert.toVo(tenantService.getDefaultTenant()));
    }

    @GetMapping("/current")
    public CommonResult<TenantRespVO> getCurrentTenant() {
        Long tenantId = TenantContextHolder.get();
        return CommonResult.success(TenantConvert.toVo(tenantService.getTenant(tenantId)));
    }
}
