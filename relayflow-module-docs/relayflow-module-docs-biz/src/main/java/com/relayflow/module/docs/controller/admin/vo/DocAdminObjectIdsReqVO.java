package com.relayflow.module.docs.controller.admin.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DocAdminObjectIdsReqVO {

    @NotEmpty
    private List<Long> objectIds;
}
