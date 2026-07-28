package com.relayflow.module.im.controller.admin.productsettings.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImProductSettingsRespVO {

    private Integer groupMemberLimitDefault;

    private List<GroupSizeUserRuleVO> userRules = new ArrayList<>();

    private List<GroupSizeGroupRuleVO> groupRules = new ArrayList<>();

    private Boolean allowPublicGroup;

    private String deptGroupLeavePolicy;

    private String memberExportPolicy;

    private List<String> memberExportDesignatedUserIds = new ArrayList<>();

    private Boolean recallEnabled;

    private Integer recallWithinHours;

    private Boolean editEnabled;

    private Integer editWithinHours;
}
