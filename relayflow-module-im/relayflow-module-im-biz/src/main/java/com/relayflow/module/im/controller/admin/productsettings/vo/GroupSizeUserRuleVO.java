package com.relayflow.module.im.controller.admin.productsettings.vo;

import lombok.Data;

@Data
public class GroupSizeUserRuleVO {

    private String subjectType;

    private String subjectId;

    private String subjectLabel;

    private Integer memberLimit;
}
