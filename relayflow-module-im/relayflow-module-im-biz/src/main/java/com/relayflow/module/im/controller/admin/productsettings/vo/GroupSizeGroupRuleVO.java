package com.relayflow.module.im.controller.admin.productsettings.vo;

import lombok.Data;

@Data
public class GroupSizeGroupRuleVO {

    private String conversationId;

    private String groupName;

    private Integer memberLimit;
}
