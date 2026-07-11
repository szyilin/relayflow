package com.relayflow.module.infra.controller.admin.storage.vo;

import lombok.Data;

import java.util.List;

@Data
public class StorageConfigRespVO {

    private List<StorageProviderRespVO> providers;
}
