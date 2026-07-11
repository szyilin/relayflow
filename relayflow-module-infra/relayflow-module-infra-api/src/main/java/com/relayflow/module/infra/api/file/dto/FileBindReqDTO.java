package com.relayflow.module.infra.api.file.dto;

import lombok.Data;

@Data
public class FileBindReqDTO {

    private Long fileId;
    private String bizType;
    private Long bizId;
}
