package com.relayflow.module.docs.controller.admin.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DocAdminTransferOwnerReqVO {

    @NotEmpty
    private List<Long> objectIds;

    @NotNull
    private Long newOwnerUserId;
}
