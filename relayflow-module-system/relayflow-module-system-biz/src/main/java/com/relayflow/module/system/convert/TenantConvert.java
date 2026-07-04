package com.relayflow.module.system.convert;

import com.relayflow.module.system.api.tenant.dto.TenantRespDTO;
import com.relayflow.module.system.controller.admin.tenant.vo.TenantRespVO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;

public final class TenantConvert {

    private TenantConvert() {
    }

    public static TenantRespDTO toDto(SysTenantDO tenant) {
        if (tenant == null) {
            return null;
        }
        TenantRespDTO dto = new TenantRespDTO();
        dto.setId(tenant.getId());
        dto.setCode(tenant.getCode());
        dto.setName(tenant.getName());
        dto.setStatus(tenant.getStatus());
        dto.setCreateTime(tenant.getCreateTime());
        return dto;
    }

    public static TenantRespVO toVo(SysTenantDO tenant) {
        if (tenant == null) {
            return null;
        }
        TenantRespVO vo = new TenantRespVO();
        vo.setId(tenant.getId());
        vo.setCode(tenant.getCode());
        vo.setName(tenant.getName());
        vo.setStatus(tenant.getStatus());
        vo.setCreateTime(tenant.getCreateTime());
        return vo;
    }
}
