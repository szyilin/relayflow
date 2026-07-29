package com.relayflow.module.docs.controller.admin.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DocAdminRestoreReqVO {

    @NotEmpty
    private List<Long> objectIds;

    /** null/omit = keep previous owner */
    private Long newOwnerUserId;
}
