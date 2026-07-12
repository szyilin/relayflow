package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class TenantSelectionDataVO {

    private List<AuthRegisterTenantSummaryVO> tenants;
}
