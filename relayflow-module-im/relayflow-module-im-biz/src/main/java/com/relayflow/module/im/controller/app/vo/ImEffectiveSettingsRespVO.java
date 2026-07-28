package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

@Data
public class ImEffectiveSettingsRespVO {

    private Integer groupMemberLimitDefault;

    private Boolean allowPublicGroup;

    private String deptGroupLeavePolicy;

    private String memberExportPolicy;

    private Boolean recallEnabled;

    private Integer recallWithinHours;

    private Boolean editEnabled;

    private Integer editWithinHours;
}
